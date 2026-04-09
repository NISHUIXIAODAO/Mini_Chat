# 改动汇总（本机双实例 5050/6060 + Redis 精准路由）

本文件用于汇总近期在本项目中完成的关键改动点（后端消息链路、集群/双实例路由、前端展示与联调修复、AI 接入等）。

---

## 1. 单聊从 Kafka 广播 → Redis 精准路由（定向投递）

### 1.1 Redis 在线位置注册/读取

- 新增/启用用户在线位置的存取能力（Key：`user:location:{userId}`，Value：`ip:port`）
  - [RedisServiceImpl.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/service/impl/RedisServiceImpl.java#L108-L121)
- 接口抽象（便于后续替换实现/统一调用）
  - [IRedisService.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/service/IRedisService.java)

### 1.2 WebSocket 握手成功时写入 Redis（支持 5050/6060 双实例）

- 握手成功后：
  - 绑定 `userId -> channel`（本机内存映射）
  - 计算当前实例地址 `serverIp:serverPort`
  - 写入 Redis：`saveUserLocation(userId, address)`
  - [HandlerWebSocket.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/webSocket/netty/HandlerWebSocket.java#L105-L131)
- 连接断开时清理 Redis 位置信息：`removeUserLocation(userId)`
  - [HandlerWebSocket.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/webSocket/netty/HandlerWebSocket.java#L71-L85)

说明：
- 当前端口读取使用 `Environment.getProperty("server.port")`；当你在 IDEA 的 VM options 里使用 `-Dserver.port=6060` 启动时，Spring Environment 会感知该值并返回正确端口。

### 1.3 后端推送链路改造（本机直推 / 远程 HTTP 转发）

- **发送消息**：保存 DB 后不再广播 Kafka（保留注释作为切换点），改为调用 `pushMessageToUser`
  - [ChatMessageServiceImpl.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/service/impl/ChatMessageServiceImpl.java#L159-L170)
- **机器人回复**：同样走 `pushMessageToUser`
  - [ChatMessageServiceImpl.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/service/impl/ChatMessageServiceImpl.java#L240-L267)
- **pushMessageToUser 逻辑**：
  - 从 Redis 获取目标用户 `ip:port`
  - 若 `ip:port` 与本机一致：调用 `ChannelContextUtils.sendMessage` 本机 WebSocket 推送
  - 否则：HTTP `POST http://{ip}:{port}/internal/push` 转发到目标节点
  - [ChatMessageServiceImpl.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/service/impl/ChatMessageServiceImpl.java#L274-L323)

---

## 2. 内部转发接口（跨节点定向投递）

- 新增内部接口：`POST /internal/push`，仅用于集群内部转发到目标节点后，再由该节点执行本机 WebSocket 推送
  - [InternalPushController.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/controller/InternalPushController.java#L12-L28)
- 拦截器白名单放行 `/internal/**`，避免内部转发被鉴权拦截导致 403
  - [InterceptorConfig.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/config/InterceptorConfig.java#L20-L39)

---

## 3. 历史聊天记录接口修复（按 sessionId 查询 + 顺序处理）

- 获取历史记录时：
  - 单聊：通过 `SessionIdUtils.generateSessionId(userId, contactId)` 生成 sessionId 后按 sessionId 查询
  - 群聊：按 contactId 查询
  - [ChatMessageServiceImpl.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/service/impl/ChatMessageServiceImpl.java#L176-L234)
- 返回前对结果按 `sendTime` 做升序排序，保证前端渲染顺序稳定（旧消息在前，新消息在后）
  - [ChatMessageServiceImpl.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/service/impl/ChatMessageServiceImpl.java#L225-L233)

相关 Mapper 补齐/调整：
- 新增历史查询方法（按 sessionId / 按 contactId），并使用 `@ResultMap("BaseResultMap")` 复用 XML 映射
  - [ChatMessageMapper.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/mapper/ChatMessageMapper.java#L40-L66)
  - 映射定义： [ChatMessageMapper.xml](file:///e:/Download_E/Java/easychat_java/src/main/resources/mapper/xml/ChatMessageMapper.xml#L5-L20)

---

## 4. 前端聊天页面：左右气泡、刷新后历史渲染、联系人预览更新

### 4.1 聊天气泡左右展示（按 sendUserId 判断）

- 核心判断：`sendUserId == 当前用户 userId` → 右侧（sent），否则左侧（received）
  - [chat.js](file:///e:/Download_E/Java/easychat_java/src/main/resources/static/js/chat.js#L146-L186)
- 兼容字段：历史记录/后端 DTO 使用 `sendUserId`；前端临时对象曾使用 `senderId`，因此做了兼容处理与字符串化比较，避免类型不一致导致误判
  - [chat.js](file:///e:/Download_E/Java/easychat_java/src/main/resources/static/js/chat.js#L154-L169)

### 4.2 发送链路（WebSocket + HTTP 兜底）

- WebSocket 发送 + HTTP `/chat/sendMessage` 兜底，避免单通道不稳定时发送失败
  - [chat.js](file:///e:/Download_E/Java/easychat_java/src/main/resources/static/js/chat.js#L188-L238)

### 4.3 联系人列表最新消息预览（实时置顶）

- 收到 WebSocket 消息后：
  - 若属于当前会话：渲染到聊天区（含去重）
  - 同时更新左侧联系人“最后消息预览”并置顶 + 再调用 `loadContacts()` 同步后端数据
  - [chat.js](file:///e:/Download_E/Java/easychat_java/src/main/resources/static/js/chat.js#L305-L394)

### 4.4 WebSocket 前端管理

- WebSocket 管理：重连、心跳、按 messageType 分发回调（chat/system/friendRequest）
  - [websocket.js](file:///e:/Download_E/Java/easychat_java/src/main/resources/static/js/websocket.js)
- 聊天页额外的 `setupChatHandlers`（用于在 chat.html 中直接追加消息）
  - [websocket.js](file:///e:/Download_E/Java/easychat_java/src/main/resources/static/js/websocket.js#L335-L383)

### 4.5 样式优化

- 增强消息气泡样式（换行、圆角差异等）
  - [style.css](file:///e:/Download_E/Java/easychat_java/src/main/resources/static/css/style.css)

---

## 5. AI 机器人接入（AIService + Hutool）

- AIService 抽象与实现：使用 Hutool `HttpRequest` 调用 OpenAI 兼容的 Chat Completions 接口（DeepSeek 默认）
  - [AIService.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/service/AIService.java)
  - [AIServiceImpl.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/service/impl/AIServiceImpl.java#L20-L80)
- 机器人自动回复：在发送目标为机器人时触发异步回复并推送给用户
  - [ChatMessageServiceImpl.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/service/impl/ChatMessageServiceImpl.java#L236-L272)

---

## 6. 其他修复/调整

- `JsonUtils` 缺失/编译问题修复：使用标准 `org.slf4j` Logger
  - [JsonUtils.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/utils/JsonUtils.java)
- `UserContactMapper#getContactTypeByContactId` 参数调整：补齐 `userId + contactId` 维度，避免单参导致单聊/群聊判断不准
  - [UserContactMapper.java](file:///e:/Download_E/Java/easychat_java/src/main/java/com/easychat/mapper/UserContactMapper.java#L40-L44)
- chat.html 引入脚本顺序（确保 websocket/chat 逻辑加载）
  - [chat.html](file:///e:/Download_E/Java/easychat_java/src/main/resources/static/chat.html#L60-L62)

---

## 7. 已知注意点（便于后续迭代）

- 当前 `websocket.js` 与 `chat.js` 都会在 chat.html 场景下注册 `onChatMessage` 处理逻辑；若出现重复追加/重复渲染，需要统一由一个文件负责 UI 渲染，另一个仅负责连接管理。
- `AIServiceImpl` 需要通过配置注入 API Key（不要将真实 Key 写入仓库）。

