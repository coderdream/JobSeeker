# Get Jobs 求职聚合平台

Get Jobs 是一个面向招聘平台自动化的全栈应用。本仓库包含 Java 21 Spring Boot 后端，以及 `front/` 目录下的 Next.js 前端。系统提供带 JWT 认证的 REST API，管理各招聘平台配置、Cookie、任务状态、岗位数据和进度流，并通过 Playwright worker 执行平台登录、岗位采集和投递自动化。

当前支持的平台：

- Boss 直聘
- 51job
- 猎聘
- 智联招聘
- 鱼泡直聘页面与接口入口

## 功能概览

- 用户注册、登录和 JWT 鉴权
- 按用户隔离的平台配置、Cookie、AI 设置和任务状态
- 平台登录状态检查、Cookie 保存、任务启动与停止
- Boss 和 51job 任务进度 SSE 推送
- 猎聘和智联任务状态轮询
- 岗位数据列表、统计分析和刷新
- 基础参考数据管理
- 前端页面覆盖登录、平台配置、平台执行、数据分析、基础数据、环境配置和 AI 配置

## 技术栈

后端：

- Java 21
- Spring Boot 3.5.x、Spring MVC、Spring Security、Bean Validation
- MyBatis-Plus
- PostgreSQL 开发环境数据源
- Flyway 数据库迁移
- JWT 认证，基于 `jjwt`
- Playwright Java 浏览器自动化
- JUnit 5、Mockito、H2 测试

前端：

- Next.js 16、React 19、TypeScript
- Tailwind CSS
- Framer Motion
- Chart.js
- lucide-react

## 目录结构

- `src/main/java/com/wh/jobsbackend`：后端应用源码
- `src/main/resources/db/migration`：Flyway 数据库迁移脚本
- `src/test/java/com/wh/jobsbackend`：后端测试
- `front/app`：Next.js App Router 页面
- `front/components`：前端通用组件
- `front/lib`：前端 API、认证、SSE 和平台请求封装
- `docs/frontend-backend-integration.md`：前后端联调说明

## 运行配置

默认 Spring profile 为 `dev`。

重要环境变量：

- `DB_URL`，默认 `jdbc:postgresql://127.0.0.1:5432/jobs_backend`
- `DB_USERNAME`，默认 `jobs_backend`
- `DB_PASSWORD`，默认 `jobs_backend`
- `JWT_SECRET`，生产环境必须替换为安全密钥

默认端口：

- 后端：`8889`
- 前端开发服务：`6866`
- 前端 API 基地址：`http://localhost:8889`

## 本地开发

安装后端依赖并运行测试：

```powershell
mvn "-Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository" test
```

运行指定后端测试：

```powershell
mvn "-Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository" "-Dtest=UserAutomationRegistryTest,ProgressStreamServiceTest" test
```

启动后端：

```powershell
$env:JAVA_HOME='E:\jdk21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn --% -Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository -DskipTests spring-boot:run
```

启动前端：

```powershell
cd front
npm install
npm run dev
```

构建前端：

```powershell
cd front
npm run build
```

构建前端并复制静态产物到后端资源目录：

```powershell
cd front
npm run build:prod
```

## 测试与回归

常用后端测试：

```powershell
mvn "-Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository" test
```

常用前端检查：

```powershell
cd front
npm run lint
npm run test:auth-registration
npm run test:auth-provider-session
npm run test:authed-request-token
npm run test:api-base-url
npm run test:api-client-auth
npm run test:sse-auth
npm run test:platform-contract
```

## 开发注意事项

- 私有数据必须通过当前用户隔离，避免跨用户读取配置、Cookie、岗位数据和任务状态。
- 修改数据库结构时新增 Flyway 版本化迁移，不修改已应用迁移。
- 修改平台能力时同步检查后端 Controller、Service、worker、前端页面和 `front/lib` 请求封装。
- 普通单元测试不要触发真实可见浏览器自动化。
- `target/`、`front/.next`、`front/out`、`front/node_modules`、`src/main/resources/dist` 和本地数据库文件属于运行或构建产物，通常不应提交。

## 英文文档

English documentation is available in [README.en.md](README.en.md).
