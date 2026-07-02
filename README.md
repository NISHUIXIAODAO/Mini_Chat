# EasyChat

EasyChat 是一个基于 Spring Boot + React 的即时通讯项目，支持用户登录注册、联系人管理、好友申请、单聊消息、历史消息查询和 WebSocket 实时推送。

项目当前已做前后端分离：

- 后端：Spring Boot，提供 HTTP API、JWT 鉴权、MySQL 持久化、Redis 在线状态/缓存、Kafka 消息链路、Netty WebSocket。
- 前端：React + TypeScript + Vite，开发端口固定为 `3000`，通过 Vite 代理访问后端 API。

## 技术栈

后端：

- Java 8 语法级别
- Spring Boot
- MyBatis / MyBatis-Plus
- MySQL
- Redis / Redisson
- Kafka
- Netty WebSocket
- JWT
- Lombok
- Fastjson / Jackson

前端：

- React 18
- TypeScript
- Vite
- React Router
- Tailwind CSS
- lucide-react

## 目录结构

```text
easychat_java
├── src/main/java/com/easychat      # 后端源码
├── src/main/resources              # 后端配置、Mapper XML、旧静态资源
├── frontend                        # React 前端项目
├── pom.xml                         # 后端 Maven 配置
└── README.md
```

主要后端目录：

- `controller`：HTTP 接口
- `service` / `service/impl`：业务逻辑
- `mapper`：MyBatis Mapper
- `entity`：实体、DTO、响应对象
- `config`：拦截器、Redis、Kafka 等配置
- `interceptor`：JWT 请求拦截
- `webSocket`：Netty WebSocket 和通道管理
- `kafka`：Kafka 生产者、消费者和序列化器

主要前端目录：

- `frontend/src/pages/AuthPage.tsx`：登录/注册页
- `frontend/src/pages/ChatPage.tsx`：聊天页
- `frontend/src/lib/api.ts`：HTTP 请求封装
- `frontend/src/lib/chatSocket.ts`：WebSocket 客户端封装

## 本地环境要求

需要先准备以下服务：

- JDK：推荐 JDK 8 或 JDK 17
- Maven
- Node.js
- npm 或 pnpm
- MySQL：`127.0.0.1:3306`
- Redis：`127.0.0.1:6379`
- Kafka：`localhost:9092`

后端默认数据库配置位于 `src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/easychat?...
spring.datasource.username=root
spring.datasource.password=123123
```

本地需要存在名为 `easychat` 的数据库，并准备好对应表结构。当前仓库未提供一键初始化 SQL 时，需要根据实体和 Mapper 自行准备数据库表。

## 端口说明

| 服务 | 地址 |
| --- | --- |
| 前端 Vite | `http://127.0.0.1:3000` |
| 后端 HTTP API | `http://localhost:5050` |
| WebSocket | `ws://localhost:5051/ws` |
| MySQL | `127.0.0.1:3306/easychat` |
| Redis | `127.0.0.1:6379` |
| Kafka | `localhost:9092` |

前端通过 Vite 代理访问后端：

```text
/api/* -> http://localhost:5050/*
```

## 启动后端

在项目根目录执行：

```powershell
mvn spring-boot:run
```

如果当前终端没有配置 `JAVA_HOME`，可以临时指定：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17.0.18'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
```

后端启动后会同时启动：

- Spring Boot HTTP 服务：`5050`
- Netty WebSocket 服务：`5051`

## 启动前端

进入前端目录：

```powershell
cd frontend
npm install
npm run dev
```

前端固定运行在：

```text
http://127.0.0.1:3000
```

如果使用 pnpm：

```powershell
cd frontend
pnpm install
pnpm dev
```

## 常用脚本

前端：

```powershell
cd frontend
npm run dev      # 启动开发服务，固定 3000 端口
npm run check    # TypeScript 检查
npm run lint     # ESLint 检查
npm run build    # 生产构建
```

后端：

```powershell
mvn -q -DskipTests compile
mvn test
mvn spring-boot:run
```

## 登录鉴权

项目使用 JWT 做接口鉴权。

前端请求头统一使用：

```http
Authorization: Bearer <token>
```

JWT 配置：

```properties
jwt.secret=${JWT_SECRET:easychat-default-jwt-secret-change-me-please-32bytes}
jwt.expiration-millis=${JWT_EXPIRATION_MILLIS:86400000}
```

注意：`jwt.secret` 用于 HS256 签名，长度需要至少 32 字节。生产环境应通过环境变量 `JWT_SECRET` 覆盖默认值。

## WebSocket

WebSocket 地址：

```text
ws://localhost:5051/ws?token=<jwt>
```

前端登录后会自动建立 WebSocket 连接，用于实时接收聊天消息、系统通知和好友申请通知。

## 注意事项

- 后端启动依赖 MySQL、Redis、Kafka，请先确认这些服务可用。
- `documents/`、`frontend/dist/`、`frontend/node_modules/`、`target/` 等本地文档或构建产物已加入 `.gitignore`。
- `.gitignore` 不会自动取消已被 Git 跟踪的文件。如果某些文件已经进入版本库，需要使用 `git rm --cached` 取消跟踪。
- 邮件验证码使用 `spring.mail.*` 配置，实际运行注册验证码功能前需要确认邮箱授权码有效。
- 旧版静态页面仍保留在 `src/main/resources/static`，当前推荐使用 `frontend/` 下的 React 前端。

## 基本使用流程

1. 启动 MySQL、Redis、Kafka。
2. 启动后端：`mvn spring-boot:run`。
3. 启动前端：`cd frontend && npm run dev`。
4. 打开 `http://127.0.0.1:3000`。
5. 注册账号或登录。
6. 添加好友后进入聊天页面发送消息。
