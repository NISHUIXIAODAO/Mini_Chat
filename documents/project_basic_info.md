# EasyChat 项目基础信息

## 1. 项目概述

EasyChat 是一个基于 Java 的即时通讯项目，后端采用 Spring Boot 构建，结合 WebSocket/Netty 实现实时消息能力，并集成了 MySQL、Redis、Kafka、JWT、邮件发送等常见中间件。仓库中同时包含一个前端工程，使用 Vite + React + TypeScript 构建。

从当前代码结构看，这个项目的核心目标是支持用户登录注册、好友/联系人管理、群聊、消息收发、历史消息查询等聊天场景。

## 2. 技术栈

### 后端

- Java 8
- Spring Boot 2.7.12
- MyBatis / MyBatis-Plus
- MySQL
- Redis
- Kafka
- Netty
- JWT
- Spring Validation
- Spring Mail
- Knife4j Swagger 文档
- Lombok
- Hutool
- Fastjson
- OkHttp
- Redisson

### 前端

- React 18
- TypeScript
- Vite
- React Router
- Tailwind CSS
- Zustand
- lucide-react

## 3. 项目结构

### 后端主目录

后端源码主要位于 `src/main/java/com/easychat`，按职责拆分为：

- `controller`：HTTP 接口层
- `service` / `service/impl`：业务逻辑层
- `mapper`：数据库访问层
- `entity`：实体、DTO、VO
- `config`：Spring 配置
- `kafka`：Kafka 生产/消费相关代码
- `webSocket`：WebSocket/Netty 消息处理
- `interceptor`：拦截器
- `handler`：全局异常处理
- `utils`：通用工具类
- `enums`：枚举定义

### 主要资源文件

- `src/main/resources/application.properties`
- `src/main/resources/mapper/xml/*`
- `src/main/resources/static/*`

### 前端目录

前端主要位于 `frontend/`，当前结构比较轻量，入口页面在：

- `frontend/src/main.tsx`
- `frontend/src/App.tsx`
- `frontend/src/pages/Home.tsx`

## 4. 启动入口

### 后端启动类

- `src/main/java/com/easychat/EasyChatApplication.java`

该类使用：

- `@SpringBootApplication`
- `@MapperScan("com.easychat.mapper")`
- `@EnableAsync`

说明项目启用了：

- Spring Boot 自动配置
- MyBatis Mapper 扫描
- 异步任务支持

### 前端入口

- `frontend/src/App.tsx`

当前路由很少，只有：

- `/` -> `Home`
- `/other` -> 占位页

## 5. 配置与运行依赖

### 本地基础配置

`application.properties` 中可以看出，后端依赖以下本地服务：

- MySQL：`127.0.0.1:3306/easychat`
- Redis：`127.0.0.1:6379`
- Kafka：`localhost:9092`
- 邮件服务：`smtp.163.com`

同时还配置了：

- HTTP 端口：`5050`
- WebSocket 端口：`5051`

### 其他配置特征

- 使用 Jackson 统一日期格式和时区
- 开启 MyBatis 日志输出
- 配置了 Bloom Filter 相关参数，用于邮箱去重或预校验

## 6. 已识别的核心业务模块

从控制器和实体命名来看，项目已经覆盖这些业务方向：

- 用户信息与登录注册
- 联系人管理
- 好友申请与处理
- 群聊管理
- 会话管理
- 消息发送与历史消息
- WebSocket 在线通信

相关控制器包括：

- `UserInfoController`
- `UserContactController`
- `UserContactApplyController`
- `GroupInfoController`
- `ChatSessionController`
- `ChatSessionUserController`
- `ChatMessageController`
- `ChatController`
- `InternalPushController`

## 7. 数据模型概览

实体类主要包括：

- `UserInfo`
- `UserInfoBeauty`
- `UserContact`
- `UserContactApply`
- `GroupInfo`
- `ChatSession`
- `ChatSessionUser`
- `ChatMessage`

DTO/VO 主要包括：

- `LoginDTO`
- `RegisterDTO`
- `MessageSendDTO`
- `ChatSendMessageDTO`
- `GetMessageHistoryDTO`
- `DisposeApplyDTO`
- `ApplyGroupAddDTO`
- `SetGroupDTO`
- `MessageHistoryResponseDTO`
- `UserApplyListResponseDTO`

## 8. 通信与中间件设计

### WebSocket / Netty

项目中存在：

- `webSocket/MessageHandler`
- `webSocket/netty/NettyWebSocketStarter`
- `webSocket/netty/HandlerWebSocket`
- `webSocket/netty/HandlerHeartBeat`

说明实时消息并不是只靠普通 HTTP 接口，而是有独立的长连接消息链路。

### Kafka

项目包含 Kafka 生产者/消费者及序列化器：

- `KafkaMessageProducer`
- `KafkaMessageConsumer`
- `KafkaProducerConfig`
- `KafkaConsumerConfig`
- `MessageSendDTOSerializer`
- `MessageSendDTODeserializer`

这通常用于削峰、异步投递，或者把消息处理从主请求链路中解耦。

### Redis

项目引入 Redis 和 Redisson，说明缓存、分布式锁、在线状态、临时数据等场景大概率有使用空间。

## 9. 前端现状

前端目前更像一个基础壳：

- 已配置路由
- 已接入 TypeScript、Tailwind、Zustand
- `Home` 页面当前为空白占位

所以面试时可以把重点放在后端架构和实时通信能力上，前端更适合作为调用和展示层来理解。

## 10. 面试时可先记住的几个关键词

- Spring Boot 即时通讯项目
- WebSocket + Netty 长连接
- Kafka 异步消息链路
- Redis 缓存/状态管理
- MySQL 持久化
- JWT 认证
- 联系人、群聊、会话、消息四层业务模型

## 11. 备注

当前仓库里已经包含本地化配置和开发依赖，适合做学习和面试复盘，但其中的数据库、邮箱等敏感信息不建议直接带到生产环境。
