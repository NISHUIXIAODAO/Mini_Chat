# EasyChat 项目问题分析与改进建议

## 1. 总体判断

当前 EasyChat 已经具备即时通讯项目的基本骨架：用户、联系人、群聊、会话、消息、WebSocket、Kafka、Redis、JWT 等模块都已经出现，适合作为面试项目继续打磨。

但从代码质量和工程完整度看，目前更像一个学习阶段项目，距离“面试可讲、可演示、可追问”的状态还有明显差距。建议优先把它改造成一个重点突出的后端项目：突出实时通信链路、消息可靠性、认证鉴权、缓存设计和工程化能力。

## 2. 高优先级问题

### 2.1 配置文件存在明文敏感信息

**现象**

- `application.properties` 中直接写了 MySQL 用户名和密码。
- 邮箱账号和授权码直接写在配置文件中。
- `AIServiceImpl` 中存在默认 API Key。
- `JWTServiceImpl` 中硬编码了 JWT 密钥。

**风险**

- 一旦代码上传到 GitHub 或交给面试官查看，会暴露账号、密钥和授权码。
- 面试中容易被追问安全意识。
- 不利于多环境部署。

**建议**

- 将敏感配置全部改为环境变量或本地私有配置文件。
- 新增 `application-example.properties`，只保留示例配置。
- 使用 `.gitignore` 排除真实配置文件。
- JWT 密钥、邮件授权码、AI Key 使用 `${ENV_NAME}` 注入。

**面试表达**

可以说：“我把项目中的硬编码密钥迁移到了环境变量，并提供 example 配置，避免敏感信息进入仓库。”

### 2.2 JWT 校验逻辑存在严重缺陷

**现象**

- `JWTServiceImpl.checkToken` 目前直接 `return true`，没有真正解析和验证 token。
- JWT 密钥硬编码。
- token 只做了 24 小时过期，没有 refresh token、黑名单、主动失效机制。
- `Interceptor` 拦截失败时直接 `return false`，没有统一返回 JSON 错误响应。

**风险**

- 伪造、过期、非法 token 可能被放行。
- 前端拿不到规范错误响应。
- 登录态设计不完整。

**建议**

- `checkToken` 中真正调用 `parseJWT`，捕获过期、签名错误等异常。
- JWT 密钥改为配置项。
- 拦截器中对未登录、token 无效统一写出 JSON 响应。
- 统一请求头名称，建议使用 `Authorization: Bearer <token>`。
- 可增加 Redis token 黑名单或登录态版本号，用于退出登录和踢下线。

**面试表达**

可以说：“我补齐了 JWT 的真实验签、过期校验和统一认证失败响应，并统一了 Authorization 请求头规范。”

### 2.3 全局异常处理存在二次异常风险

**现象**

- `GlobalExceptionHandler.BusinessException(String message)` 构造方法允许 `errorCode` 为 `null`。
- `handleBusinessException` 中直接调用 `ex.getErrorCode().getCode()`。
- 如果抛出只带 message 的业务异常，会触发空指针，业务异常会变成系统异常。

**风险**

- 原本可控的业务错误被错误地包装成 500。
- 排查问题困难。
- 面试中容易被认为异常体系不稳。

**建议**

- 给纯 message 的 `BusinessException` 默认错误码，例如 `BAD_REQUEST`。
- 或者禁止无错误码构造方式。
- Controller 不要到处 try-catch 后返回字符串，让异常交给全局异常处理器。
- `ResultVo` 和 `ErrorResponse` 需要统一响应格式，避免项目里出现两套返回结构。

**面试表达**

可以说：“我把异常从 Controller 层收敛到全局异常处理器，避免重复 try-catch，也修复了业务异常二次 NPE 的问题。”

### 2.4 核心业务缺少事务边界

**现象**

- `UserContactServiceImpl` 中添加好友、同意申请、创建会话、插入会话成员、插入消息、更新 Redis、发送 Kafka 消息混在一个方法里。
- 这些流程涉及多张表，但没有明显的 `@Transactional`。
- `ChatMessageServiceImpl.saveMessage` 中同时更新会话、插入消息、推送消息，也没有清晰的事务边界。

**风险**

- 数据库部分成功、部分失败时会产生脏数据。
- 消息入库成功但推送失败，或推送成功但事务回滚，会出现一致性问题。
- 面试官很容易追问：“发送消息失败怎么办？会不会重复？会不会丢？”

**建议**

- 对好友申请通过、建会话、发消息等核心写流程增加事务。
- 把“数据库写入”和“消息推送”拆开：先保证消息落库，再异步推送。
- 可以引入本地消息表或 Outbox 模式，提升消息可靠性。
- 对幂等写入增加唯一索引或业务幂等判断，例如好友关系、会话成员不能重复插入。

**面试表达**

可以说：“我把核心链路拆成事务内的数据落库和事务后的异步投递，避免数据库状态和 WebSocket 推送状态互相污染。”

## 3. 架构与代码结构问题

### 3.1 Service 类职责过重

**现象**

- `UserContactServiceImpl` 文件较大，承担了好友申请、群申请、联系人查询、会话创建、消息插入、Redis 更新、Kafka 推送等职责。
- `ChatMessageServiceImpl` 同时处理消息校验、消息保存、会话更新、AI 机器人、跨节点推送。
- 部分 Controller 只是简单转发，但仍然包含 try-catch 和返回拼接。

**风险**

- 代码难维护、难测试。
- 任意小改动都可能影响多个业务流程。
- 面试时不容易讲清楚边界。

**建议**

- 拆出独立组件：
  - `ContactApplicationService`：处理好友/群申请流程。
  - `SessionDomainService`：负责会话创建、会话成员维护。
  - `MessageApplicationService`：负责消息发送主流程。
  - `MessagePushService`：负责 WebSocket/Kafka/跨节点推送。
  - `RobotChatService`：负责机器人回复。
- Controller 只做参数接收和调用应用服务。
- Mapper 只负责数据库访问，不承载业务判断。

**面试表达**

可以说：“我按即时通讯领域对象重新拆分了联系人、会话、消息、推送四个边界，让核心链路更清晰。”

### 3.2 依赖注入方式不统一

**现象**

- 项目中同时存在字段注入、构造器注入、`@Autowired`、`@Resource`。
- 一些类使用接口注入，一些类直接注入实现类，例如 `Interceptor` 注入 `JWTServiceImpl`。

**风险**

- 单元测试不方便。
- 依赖关系不清晰。
- 不利于替换实现。

**建议**

- 统一使用构造器注入。
- 尽量依赖接口，例如注入 `IJWTService` 而不是 `JWTServiceImpl`。
- 删除无用依赖和重复 import。

### 3.3 命名和包结构不规范

**现象**

- `hander` 应为 `handler`。
- `test` 包放在 `src/main/java` 下。
- `email.java`、`map.java` 类名小写，不符合 Java 命名规范。
- 部分 DTO、VO、DO 混用，返回结构不统一。

**建议**

- 将测试/实验代码移动到 `src/test/java` 或删除。
- 修正包名和类名。
- 统一实体分层：
  - `entity/domain` 或 `entity/po`：数据库实体。
  - `dto/request`：请求参数。
  - `dto/response`：响应对象。
  - `vo`：前端展示对象。

## 4. 业务正确性问题

### 4.1 好友和群聊流程存在重复插入风险

**现象**

- 添加好友、同意好友申请时多处直接 insert 双向联系人关系。
- 会话和会话成员在存在时 update，但后面仍可能继续 insert。
- 缺少明确的唯一约束说明。

**风险**

- 重复好友关系。
- 重复会话成员。
- 重复申请或重复处理。

**建议**

- 给联系人表增加唯一索引：`user_id + contact_id + contact_type`。
- 给会话成员表增加唯一索引：`user_id + contact_id` 或 `user_id + session_id`。
- 同意申请接口增加幂等判断：已同意直接返回成功。
- 使用 upsert 或先查后更新，并确保并发下仍安全。

### 4.2 消息发送链路可靠性不足

**现象**

- 发送消息时先更新会话再插入消息，再直接推送。
- 当前代码中有从 Kafka 广播改成精准推送的痕迹，但链路没有完全统一。
- 用户不在线时只是记录日志，没有明确未读数、离线消息、重试机制。

**风险**

- 消息可能丢推送。
- 多节点时用户位置不准确会影响投递。
- 无法回答“消息可靠性如何保证”。

**建议**

- 明确消息状态流转：`SENDING -> SENT -> DELIVERED -> READ`。
- 消息必须先落库，客户端根据消息历史补偿。
- 推送失败不代表发送失败，客户端重连后通过拉历史消息补齐。
- 增加未读数表或 Redis 未读计数。
- 增加消息 ACK 机制：客户端收到后回执，服务端更新状态。

### 4.3 群聊 sessionId 设计需要重新确认

**现象**

- 部分代码使用 `generateSessionId(userId, groupId)` 生成群聊会话。
- 群聊通常应该所有成员共享一个稳定 sessionId，例如 `G_<groupId>`。

**风险**

- 同一个群在不同用户视角可能生成不同 sessionId。
- 历史消息查询和会话聚合会变复杂。

**建议**

- 单聊 sessionId：根据两个 userId 排序后生成。
- 群聊 sessionId：固定使用 groupId 生成，例如 `group_${groupId}`。
- 在 `SessionIdUtils` 中封装单聊和群聊两个方法，避免业务层自行拼装。

### 4.4 登录在线状态设计偏粗糙

**现象**

- 登录时通过 Redis 心跳判断“用户已在别处登录”。
- WebSocket 建连后保存用户位置。
- 心跳、登录态、WebSocket 在线状态混在一起。

**风险**

- 用户异常断线时可能长时间无法登录。
- 多端登录、踢下线、重连恢复不好扩展。

**建议**

- 区分认证 token、在线连接、用户位置三个概念。
- 在线状态用 Redis key + TTL 管理。
- WebSocket 心跳续期。
- 支持单端登录时，应在新登录后通知旧连接下线，而不是简单拒绝新登录。

## 5. WebSocket 与分布式通信问题

### 5.1 本地 Channel Map 与 Redis 用户位置混用

**现象**

- `ChannelContextUtils` 使用本地静态 `ConcurrentHashMap` 保存用户连接。
- 同时 `HandlerWebSocket` 又把用户位置保存到 Redis。
- `ChatMessageServiceImpl` 判断用户所在机器后选择本地推送或 HTTP 转发。

**风险**

- 单机和多机模型混在一起，边界不清。
- 节点 IP、端口识别容易出错。
- HTTP 内部推送接口如果暴露，会有安全风险。

**建议**

- 明确当前阶段目标：
  - 如果只做单机：移除跨节点 HTTP 转发，专注本地 Channel 管理。
  - 如果做多机：用户位置、内部通信鉴权、节点注册都要完整设计。
- `/internal/push` 不能放在登录白名单裸奔，应增加内部 token、IP 白名单或网关隔离。
- 群聊推送建议通过 Redis Pub/Sub、Kafka topic 或统一 MessagePushService 分发。

### 5.2 WebSocket token 通过 URL 查询参数传递

**现象**

- `HandlerWebSocket` 从 `requestUri` 的 `?token=` 中解析 token。

**风险**

- URL 可能被浏览器历史、代理日志、服务端日志记录。
- 安全性弱于 Header 或 WebSocket 子协议认证。

**建议**

- 如果前端限制只能 query 传参，至少避免打印完整 token。
- 生产设计中建议使用短期一次性连接票据。
- WebSocket 握手成功后立即绑定用户，连接期间只靠 channel 上下文识别身份。

## 6. 接口与参数校验问题

### 6.1 DTO 缺少校验注解

**现象**

- `LoginDTO`、`RegisterDTO` 等请求对象没有 `@NotBlank`、`@Email`、`@Size` 等校验。
- Controller 方法没有使用 `@Valid`。

**风险**

- 空邮箱、空密码、异常长度参数会进入业务层。
- 参数校验散落在 Service 中。

**建议**

- 给 DTO 增加 Bean Validation 注解。
- Controller 参数使用 `@Valid @RequestBody`。
- 参数错误统一由 `GlobalExceptionHandler` 返回。

### 6.2 HTTP 方法使用不规范

**现象**

- 多个接口使用 `@RequestMapping`，没有明确 GET/POST。
- 查询历史消息使用普通参数，不支持分页对象完整传入。

**建议**

- 写操作使用 `@PostMapping`。
- 查询使用 `@GetMapping` 或 `@PostMapping` 搭配查询 DTO。
- 对聊天历史增加分页参数：`lastTimestamp`、`pageSize`。
- API 命名统一，例如 `/api/messages/send`、`/api/messages/history`。

## 7. 数据库与缓存问题

### 7.1 缺少数据库初始化和表结构说明

**现象**

- 仓库里没有明显的建表 SQL 或迁移工具配置。
- 面试演示时很难复现环境。

**建议**

- 新增 `documents/database_schema.md` 或 `sql/init.sql`。
- 补充核心表说明：用户表、联系人表、申请表、会话表、会话成员表、消息表。
- 最好引入 Flyway 或 Liquibase 管理版本化 SQL。

### 7.2 Redis key 规则不统一

**现象**

- 有些地方通过 `redisService.generateRedisKey` 生成 key。
- 有些地方直接拼接 `"user:" + userId + ":friends"`。

**风险**

- key 名不一致导致缓存读写不命中。
- 后期维护困难。

**建议**

- 所有 Redis key 统一放在一个类中管理。
- 给 key 加业务前缀，例如 `easychat:user:{id}:friends`。
- 文档化 Redis key、数据结构、TTL。

### 7.3 缓存一致性策略不清晰

**现象**

- 登录时把好友和群列表写入 Redis。
- 添加好友/群时也更新 Redis。
- 但缺少缓存失效、重建、异常补偿策略。

**建议**

- 明确 Redis 是缓存还是在线状态源。
- 联系人列表可采用 cache aside：查缓存，没有则查库并回填。
- 更新数据库成功后删除或更新缓存。
- 对关键缓存增加过期时间和重建逻辑。

## 8. 安全问题

### 8.1 密码加密方式较弱

**现象**

- 登录注册使用 `md5` 处理密码。

**风险**

- MD5 不适合存储密码。
- 容易被彩虹表破解。

**建议**

- 改为 BCrypt。
- 注册时存储 BCrypt hash。
- 登录时用 `passwordEncoder.matches(raw, encoded)` 验证。

### 8.2 日志可能泄露敏感信息

**现象**

- 拦截器打印 token。
- AI 服务打印请求和响应。
- 登录失败返回中拼接了 MD5 后的密码。

**建议**

- 禁止日志打印 token、密码、验证码、API Key。
- 登录错误只返回“账号或密码错误”，避免枚举用户。
- AI 请求日志只记录 requestId、耗时、状态码。

### 8.3 CORS 配置过窄且不支持配置化

**现象**

- CORS 只允许 `http://localhost:3000`。
- 前端 Vite 默认端口通常是 `5173`。

**建议**

- 将允许源放入配置项。
- 开发环境允许本地端口，生产环境只允许正式域名。

## 9. 工程化问题

### 9.1 当前本机无法完成 Maven 编译

**现象**

- 执行 `mvn -q -DskipTests compile` 失败。
- 错误信息：`JAVA_HOME environment variable is not defined correctly`。

**建议**

- 配置本机 JDK 8 或兼容版本。
- 在 README 中写明 JDK、Maven、MySQL、Redis、Kafka 的版本和启动方式。
- 增加 Docker Compose，一键启动 MySQL、Redis、Kafka。

### 9.2 Maven 依赖存在重复和版本不统一

**现象**

- `spring-kafka-test` 出现重复依赖。
- `mybatis-plus-generator` 出现重复依赖。
- Spring Boot parent 是 `2.7.12`，但部分依赖手动指定 `${springboot.version}` 为 `2.6.1`。
- Spring Boot Maven Plugin 使用 `2.2.6.RELEASE`，和 parent 版本不一致。

**建议**

- 清理重复依赖。
- 依赖版本尽量交给 Spring Boot dependency management。
- 插件版本与 Spring Boot parent 保持一致。
- 把生成器、测试依赖限定在合适 scope。

### 9.3 缺少 README 和启动说明

**现象**

- 根目录没有面向项目的 README。
- 前端 README 还是 Vite 默认模板。

**建议**

- 编写项目 README：
  - 项目简介
  - 技术栈
  - 架构图
  - 本地启动步骤
  - 核心接口
  - WebSocket 使用方式
  - 演示账号
  - 常见问题

### 9.4 仓库结构需要清理

**现象**

- 根目录存在 `target`、`backend/target`、`node_modules` 等构建产物或依赖目录。
- `frontend/node_modules` 体积很大，不应进入版本管理。
- `src/main/java/com/easychat/test` 放了实验代码。

**建议**

- 更新 `.gitignore`。
- 删除构建产物和依赖目录的版本跟踪。
- 测试代码放入 `src/test/java`。

## 10. 测试问题

### 10.1 缺少有效测试

**现象**

- 没有看到标准 `src/test/java` 测试目录。
- 部分测试类在 `src/main/java/com/easychat/test` 下。
- `pom.xml` 中设置了 `<skipTests>true</skipTests>`。

**风险**

- 改造时容易引入回归。
- 面试中被问到测试保障时不好回答。

**建议**

- 删除默认跳过测试配置。
- 增加单元测试：
  - `JWTServiceImplTest`
  - `SessionIdUtilsTest`
  - `UserInfoServiceTest`
  - `ChatMessageServiceTest`
- 增加集成测试：
  - 用户注册登录流程
  - 好友申请同意流程
  - 消息发送与历史查询流程
- 对 Redis/Kafka 可使用 Testcontainers 或 mock。

## 11. 前端问题

### 11.1 前端基本未完成

**现象**

- `Home.tsx` 当前为空。
- 前端 README 是默认模板。
- 没有看到登录、聊天、联系人、群聊等页面实现。

**建议**

- 如果面试重点是后端，可以把前端定位成“演示客户端”，做最小可用版本：
  - 登录/注册页
  - 联系人列表
  - 聊天窗口
  - WebSocket 连接状态
  - 消息历史加载
- 如果时间有限，也可以保留静态 HTML 演示，但 README 中要讲清楚。

## 12. 建议改造路线

### 第一阶段：先让项目安全、可运行、可展示

1. 清理敏感配置，改为环境变量。
2. 修复 JWT 校验。
3. 修复全局异常处理空指针问题。
4. 配好 `JAVA_HOME`，确保后端能编译。
5. 补充 README、数据库初始化 SQL、启动说明。
6. 清理 `.gitignore`，排除 `target`、`node_modules`、真实配置文件。

### 第二阶段：提升核心业务质量

1. 给好友申请、同意申请、消息发送增加事务。
2. 梳理单聊和群聊 sessionId 规则。
3. 增加唯一索引和幂等处理。
4. 统一 Redis key 规则。
5. 统一 Controller 返回和异常处理。
6. 增加 DTO 参数校验。

### 第三阶段：打磨即时通讯亮点

1. 明确消息状态机。
2. 增加离线消息补偿和未读数。
3. 增加客户端 ACK。
4. 抽象 `MessagePushService`。
5. 整理 WebSocket 心跳、重连、用户在线状态。
6. 如需讲高阶能力，再完善多节点推送设计。

### 第四阶段：面试材料完善

1. 画一张架构图。
2. 写一份核心链路文档：
   - 登录注册流程
   - 好友申请流程
   - 单聊消息发送流程
   - 群聊消息发送流程
   - WebSocket 建连流程
3. 准备项目亮点问答：
   - 为什么用 WebSocket/Netty？
   - Kafka 在项目中解决什么问题？
   - Redis 用在哪些地方？
   - 消息可靠性怎么保证？
   - JWT 认证怎么做？
   - 如果部署多台服务怎么推送消息？

## 13. 推荐优先修复清单

| 优先级 | 问题 | 建议动作 |
| --- | --- | --- |
| P0 | 敏感信息硬编码 | 改环境变量，新增示例配置 |
| P0 | JWT 校验无效 | 实现真实验签和过期校验 |
| P0 | 全局异常二次 NPE | 修复 `BusinessException` 默认错误码 |
| P0 | 本机无法编译 | 修复 JDK/JAVA_HOME，保证可运行 |
| P1 | 核心写流程无事务 | 给好友、会话、消息流程加事务 |
| P1 | 消息可靠性不足 | 明确落库、推送、补偿、ACK |
| P1 | Service 职责过重 | 拆分联系人、会话、消息、推送服务 |
| P1 | Redis key 不统一 | 统一 key 生成和缓存策略 |
| P2 | DTO 缺少校验 | 增加 Bean Validation |
| P2 | Maven 依赖混乱 | 清理重复依赖和版本 |
| P2 | 测试缺失 | 补充核心单测和集成测试 |
| P3 | 前端未完成 | 做最小聊天演示客户端 |

## 14. 面试改造目标

最终建议把项目包装成：

> EasyChat 是一个基于 Spring Boot + Netty WebSocket 的即时通讯系统，支持用户登录注册、好友关系、群聊、会话管理、消息持久化、离线消息补偿和实时推送。项目通过 Redis 管理在线状态与缓存，通过 Kafka/异步推送解耦消息投递，并使用 JWT 完成认证鉴权。

这个表达比“我做了一个聊天项目”更有技术密度，也更容易引出你准备好的亮点。
