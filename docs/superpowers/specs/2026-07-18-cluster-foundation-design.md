# 标准版多节点集群 P0-P4 设计

## 范围

本规格实施集群改造的 P0-P4：集群配置、节点注册、在线状态重构、本地 Channel 收敛和内部接口安全。

本阶段不引入 Kafka、Outbox、消息表或数据库结构变更。消息持久化和可靠投递链路保留到 P5-P8。

## 目标

- 使用稳定的 `nodeId` 路由在线用户，不再保存或解析 `IP:port:connectionId`。
- 节点与用户在线信息均由 Redis TTL 自动过期，节点宕机后不会长期残留。
- 本地 WebSocket Channel、全局在线状态、节点注册和内部 HTTP 调用分别由独立组件负责。
- 内部接口要求来源 IP、节点身份、时间戳、随机数和 HMAC 签名同时有效。
- 未显式配置集群参数时，单节点运行仍可完成登录、心跳、消息推送、登出和互踢。

## 配置

新增 `easychat.cluster` 配置，并允许环境变量覆盖：

```properties
easychat.cluster.node-id=${EASYCHAT_CLUSTER_NODE_ID:}
easychat.cluster.internal-host=${EASYCHAT_CLUSTER_INTERNAL_HOST:127.0.0.1}
easychat.cluster.internal-port=${EASYCHAT_CLUSTER_INTERNAL_PORT:${server.port}}
easychat.cluster.ws-port=${EASYCHAT_CLUSTER_WS_PORT:${ws.port}}
easychat.cluster.node-ttl-seconds=${EASYCHAT_CLUSTER_NODE_TTL_SECONDS:30}
easychat.cluster.node-heartbeat-seconds=${EASYCHAT_CLUSTER_NODE_HEARTBEAT_SECONDS:10}
easychat.cluster.presence-ttl-seconds=${EASYCHAT_CLUSTER_PRESENCE_TTL_SECONDS:90}
easychat.cluster.internal-secret=${EASYCHAT_CLUSTER_INTERNAL_SECRET:}
easychat.cluster.internal-allowed-ips=${EASYCHAT_CLUSTER_INTERNAL_ALLOWED_IPS:127.0.0.1,::1}
easychat.cluster.internal-request-max-age-seconds=${EASYCHAT_CLUSTER_INTERNAL_REQUEST_MAX_AGE_SECONDS:30}
```

生产环境必须提供非空的 `EASYCHAT_CLUSTER_NODE_ID` 和 `EASYCHAT_CLUSTER_INTERNAL_SECRET`。本地开发时，空 `node-id` 使用 `hostname-internalPort` 派生；空密钥必须使内部接口拒绝请求，避免无意中暴露接口。

## 组件与职责

| 组件 | 职责 |
| --- | --- |
| `ClusterNodeProperties` | 校验并提供集群配置及本地节点元数据。 |
| `NodeRegistryService` | 注册、续约、查询和注销节点。 |
| `ContactCacheService` | 管理好友和群组缓存。 |
| `AuthSessionService` | 管理登录 session。 |
| `UserPresenceService` | 管理用户到节点与连接 ID 的在线状态。 |
| `LocalChannelRegistry` | 仅管理本 JVM 的 `userId -> Channel`、本地推送和本地下线。 |
| `InternalNodeClient` | 解析目标节点、签名并调用内部 HTTP 接口。 |
| `InternalAuthInterceptor` | 验证内部 HTTP 请求。 |

删除 `IRedisService` 与 `RedisServiceImpl`。所有调用点按职责迁移，禁止新旧 Redis 聚合接口并存。`ChannelContextUtils` 重命名为 `LocalChannelRegistry`，不再访问 Redis、Mapper 或联系人缓存。

## Redis 模型

```text
cluster:node:{nodeId}
  Hash: nodeId, internalHost, internalPort, wsPort, status, lastHeartbeat
  TTL: node-ttl-seconds

cluster:nodes
  Set: nodeId

ws:user:active:{userId}
  Hash: nodeId, connectionId, loginAt, lastHeartbeat
  TTL: presence-ttl-seconds

ws:connection:{connectionId}
  Hash: userId, nodeId, channelId
  TTL: presence-ttl-seconds

cluster:nonce:{nodeId}:{nonce}
  String: marker
  TTL: internal-request-max-age-seconds
```

`UserPresenceService.disconnect` 仅在活动记录的 `connectionId` 与待清理连接一致时删除 `ws:user:active:{userId}`，避免旧连接断开删除新连接状态。节点查询发现节点 Hash 不存在时，移除 `cluster:nodes` 中的陈旧 nodeId 并视为离线。

## 运行流程

1. 应用启动时 `NodeRegistryService` 注册本节点，并每 10 秒续约 30 秒 TTL；应用关闭时注销。
2. WebSocket 握手认证成功后，`LocalChannelRegistry` 注册本地 Channel，`UserPresenceService` 记录本节点及 connectionId。
3. 每个 WebSocket 入站消息刷新 presence TTL；断开时依连接 ID 条件清理 presence，并移除本地 Channel。
4. 消息推送或强制下线先查询 `UserPresenceService`。目标 `nodeId` 等于本节点时直接调用 `LocalChannelRegistry`；否则由 `InternalNodeClient` 查询节点注册信息并调用目标节点。
5. 目标节点的内部 Controller 只调用 `LocalChannelRegistry`，不重新进行路由。

## 内部 HTTP 鉴权

内部请求使用以下 Header：

```text
X-Cluster-Node-Id
X-Cluster-Timestamp
X-Cluster-Nonce
X-Cluster-Signature
```

签名输入为 `METHOD + "\\n" + PATH + "\\n" + TIMESTAMP + "\\n" + NONCE + "\\n" + SHA256(BODY)`，其中 `"\\n"` 是一个 LF 换行符；使用共享密钥计算 HMAC-SHA256。`InternalNodeClient` 负责生成这些 Header。

`InternalAuthInterceptor` 按以下顺序校验：

1. 请求来源地址匹配 `internal-allowed-ips`。
2. 共享密钥已配置，Header 齐全，且时间戳处于有效期内。
3. `nodeId` 对应的 Redis 节点注册存在并处于健康状态。
4. 基于原始请求体计算的 HMAC 与 Header 使用常量时间比较一致。
5. 使用 `SET NX EX` 写入 nonce 成功；已存在则拒绝为重放请求。

鉴权失败不进入 Controller，缺失或错误凭据返回 401，来源 IP 不允许或请求重放返回 403。`/internal/**` 从普通 JWT 拦截器白名单移除，但普通 JWT 拦截器不拦截该路径，由专用内部拦截器独占处理。

## 接口契约

- `POST /internal/push?userId={userId}`：签名请求体为 `MessageSendDTO`，目标节点仅本地推送。
- `POST /internal/offline?userId={userId}`：签名请求体为下线原因，目标节点仅本地下线。

`MessagePushService` 与 `UserOnlineService` 不再解析 IP、端口或自行构造 HTTP 请求；它们依赖 presence、节点注册和 `InternalNodeClient`。

## 测试与验收

- 节点注册、续约和关闭注销正确更新 Redis 及 TTL。
- presence 建连、心跳、断连和旧连接延迟断开都符合条件清理规则。
- `LocalChannelRegistry` 可对本地在线用户推送和强制下线，对不存在用户安全返回。
- 同一 nodeId 路由走本地分支；不同 nodeId 使用节点注册信息发起远程调用。
- 内部鉴权分别拒绝空凭据、签名篡改、过期时间戳、重复 nonce、未知节点和不允许来源 IP。
- 使用默认本地配置时，既有登录、WebSocket 初始化、消息推送与互踢流程继续可用。
