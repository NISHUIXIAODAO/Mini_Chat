# 标准版多节点集群 P5-P10 设计

## 范围

本规格实施 P5-P10：消息 Outbox、Kafka 投递、节点本地推送分发、群聊收件人展开、故障处理和双节点验证。

依赖已完成的 P0-P4：节点注册、`nodeId` presence、本地 Channel 注册表以及内部接口签名鉴权。当前开发 Kafka 为本机 Docker 的 `127.0.0.1:9092`，单 broker 副本数为 1。

## 架构

```text
发送消息事务
  -> chat_message / 未读数 / 接收状态
  -> message_outbox(PENDING)
  -> OutboxPublisher
  -> Kafka: easychat.push.event
  -> 每个节点独立 consumer group
  -> PushDispatcher 仅推送本节点 presence 用户
  -> LocalChannelRegistry
```

每个节点使用 `easychat-push-${nodeId}` 消费者组。因此所有节点都会消费同一事件，但只对 presence 显示为本节点的收件人写 WebSocket。消费者不发起跨节点 HTTP 调用。

## 数据模型

新增 `message_outbox`：

| 字段 | 说明 |
| --- | --- |
| `id` | 自增主键。 |
| `event_id` | UUID，全局唯一索引。 |
| `aggregate_id` | 对应 `chat_message.message_id`。 |
| `event_type` | 固定为 `CHAT_MESSAGE`。 |
| `payload` | Kafka 事件 JSON。 |
| `status` | `PENDING`、`PROCESSING`、`PUBLISHED`、`FAILED`。 |
| `retry_count` | 已失败次数。 |
| `next_retry_at` | 下次允许投递的毫秒时间戳。 |
| `lease_until` | 发布器租约截止时间。 |
| `published_at` | Kafka 确认时间。 |
| `last_error` | 最近失败摘要。 |
| `trace_id` | 端到端追踪 ID。 |
| `create_time` / `update_time` | 记录时间。 |

唯一约束包含 `event_id`，扫描索引覆盖 `status`、`next_retry_at` 与 `lease_until`。本阶段提供 SQL 迁移脚本，仍由现有项目的手工迁移流程执行。

## 事件契约

Topic：`easychat.push.event`，key 使用 `messageId`。事件为：

```json
{
  "eventId": "uuid",
  "traceId": "uuid",
  "eventType": "CHAT_MESSAGE",
  "messageId": 123,
  "sessionId": "P_1_2",
  "targetUserIds": [1002],
  "messagePayload": {},
  "createdAt": 0
}
```

私聊 `targetUserIds` 为对方用户。群聊在消息事务中查询 `chat_session_user`，排除发送用户后写入完整在线候选人集合。离线用户也保留在事件中，消费者会跳过，重连后从历史消息补齐。

## Outbox 状态机

1. 消息业务事务写入完成后，同一事务写入 `PENDING` Outbox。
2. `OutboxPublisher` 用短租约将可投递记录置为 `PROCESSING`，防止多节点并发扫描重复抢占。
3. Kafka producer 成功确认后，记录转为 `PUBLISHED`。
4. 失败时递增 `retry_count`：未达到阈值则回到 `PENDING` 并指数退避；达到阈值则为 `FAILED`。
5. `FAILED` 保留原始 payload，可通过服务方法或 SQL 重置为 `PENDING` 进行人工重放。

默认扫描间隔 1 秒、批量 100、租约 30 秒、最大重试 10 次、退避从 1 秒起并封顶 5 分钟。发布失败不会回滚已提交的聊天消息。

## Kafka 与消费幂等

新增 `spring-kafka`，生产者等待 broker 确认。消费者使用手动确认并在成功处理后提交 offset；处理异常抛出，让 Spring Kafka 按固定间隔重试。默认不配置 dead-letter topic，避免在单 broker 开发环境引入不可见的丢弃路径；超过重试上限将记录含 `eventId` 与 `traceId` 的错误日志并保留 offset 以便人工恢复。

消费者以 Redis `SET NX EX` 保存 `push:consumed:{nodeId}:{eventId}`，TTL 24 小时。仅在事件在本节点至少有一个目标用户推送成功或确认本节点没有匹配的 presence 后登记完成；重复 Kafka 事件不会重复推送。

`PushDispatcher` 遍历目标用户：只有 `UserPresenceService` 返回的 `nodeId` 等于当前节点时，调用 `LocalChannelRegistry.send`。若 presence 已指向本节点但 Channel 不存在，记录诊断日志，不跨节点重试。

## 可观测性与故障处理

- 消息创建、Outbox 创建、发布尝试、Kafka 确认、消费、跳过和本地推送均输出 `traceId`、`eventId`、`messageId`、`nodeId`。
- 节点宕机由 P0-P4 Redis TTL 清理；其他节点消费事件后不会为该节点用户推送。
- Kafka 暂不可用时，Outbox 按退避重试；应用重启后继续扫描未完成记录。
- Outbox 发布器租约超时可被其他节点重新抢占，保证节点进程中断后事件仍可恢复。

## 配置与双节点运行

新增 `easychat.outbox`、`easychat.kafka` 配置和两个 profile：`node-a`、`node-b`。二者共享 MySQL、Redis 与 Kafka，分别使用不同 server/WS 端口、稳定 `nodeId` 和同一个内部集群密钥。

开发默认：

```properties
spring.kafka.bootstrap-servers=127.0.0.1:9092
easychat.kafka.push-topic=easychat.push.event
easychat.kafka.consumer-retry-attempts=3
```

生产环境使用多个 Kafka broker 后才可提高 topic 副本数与 `min.insync.replicas`；当前单 broker 只支持副本数 1。

## 测试与验收

- 消息事务同时持久化消息及 Outbox；事务失败时两者都不留下记录。
- 私聊和群聊的目标用户集合正确，群聊排除发送者。
- Outbox 租约、成功发布、失败退避和人工重放状态机正确。
- Kafka 同一事件被不同 `nodeId` consumer group 接收；每节点只推送本节点用户。
- 重复事件不会导致同一节点重复 WebSocket 推送。
- 双节点分别登录用户后，私聊和群聊实时到达；关闭节点后 TTL 到期，不再出现错误节点推送。
- 暂停 Kafka 后消息仍入库且 Outbox 保持待投递；恢复后成功发布。

