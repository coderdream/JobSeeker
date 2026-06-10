# AGENTS.md

## 项目概览

本仓库是 Get Jobs 应用的 Java 21 Spring Boot 后端，并包含 `front/` 下的 Next.js 前端页面代码。后端提供带认证的 REST API，并通过 Playwright worker 执行招聘平台自动化任务；前端负责登录、平台配置、任务执行、统计分析、基础数据、环境配置和 AI 配置页面。

核心技术栈：

- Spring Boot 3.5.x、Spring MVC、Spring Security、Bean Validation
- Java 21、Maven Wrapper、Maven
- MyBatis-Plus 持久化
- `dev` 配置下使用 PostgreSQL
- Flyway 数据库迁移，路径为 `src/main/resources/db/migration`
- 基于 `jjwt` 的 JWT 认证
- Playwright Java 浏览器自动化
- JUnit 5、Mockito、H2 测试
- `front/` 使用 Next.js 16、React 19、TypeScript、Tailwind CSS、Framer Motion、Chart.js、lucide-react

应用根包名为 `com.wh.jobsbackend`。

## 文档查询规则

当用户询问库、框架、SDK、API、CLI 工具或云服务相关问题时，必须使用 Context7 MCP 获取当前文档。适用范围包括 API 语法、配置、版本迁移、库相关调试、安装步骤和 CLI 用法。

库文档优先使用 Context7，而不是普通网页搜索。

以下情况不使用 Context7：重构、从零编写脚本、业务逻辑调试、代码审查、通用编程概念。

Context7 使用流程：

1. 使用官方库名和用户完整问题解析 library ID。
2. 选择最合适的官方或高信誉匹配项，优先考虑精确名称、相关描述、代码片段数量和评分。
3. 使用选中的 library ID 和用户完整问题查询文档。
4. 基于查询到的文档回答。

## 仓库结构

- `pom.xml` 定义 Java 版本、Spring Boot parent、后端依赖和测试插件配置。
- `.mvn/`、`mvnw`、`mvnw.cmd` 提供 Maven Wrapper。
- `src/main/java/com/wh/jobsbackend/JobsBackendApplication.java` 是 Spring Boot 启动入口。
- `src/main/java/com/wh/jobsbackend/application/config` 放置应用配置类和 MyBatis/数据初始化相关配置。
- `src/main/java/com/wh/jobsbackend/application/controller` 放置认证、平台任务、配置、Cookie、AI、参考数据、健康检查和全局异常处理 REST 控制器。
- `src/main/java/com/wh/jobsbackend/application/dto` 放置认证请求和响应 DTO。
- `src/main/java/com/wh/jobsbackend/application/entity` 放置用户、平台配置、职位数据、参考数据、任务状态和 Cookie 等 MyBatis-Plus 实体。
- `src/main/java/com/wh/jobsbackend/application/init` 放置启动期初始化逻辑。
- `src/main/java/com/wh/jobsbackend/application/mapper` 放置 Mapper 接口。
- `src/main/java/com/wh/jobsbackend/application/security` 放置无状态 JWT 安全配置、当前用户获取和用户详情加载。
- `src/main/java/com/wh/jobsbackend/application/service` 放置认证、配置、Cookie、AI 配置、平台流程、参考数据和用户任务服务。
- `src/main/java/com/wh/jobsbackend/application/stream` 放置任务进度/SSE 流式推送服务。
- `src/main/java/com/wh/jobsbackend/worker` 放置 Playwright 自动化、平台采集/投递逻辑和 worker 工具。
- `src/main/java/com/wh/jobsbackend/worker/boss`、`job51`、`liepin`、`zhilian` 放置各招聘平台的页面模型、平台配置和定位器。
- `src/main/java/com/wh/jobsbackend/worker/manager` 放置 Playwright 管理器和自动化协调器，`worker/manager/platform` 放置平台专用 Playwright handler。
- `src/main/java/com/wh/jobsbackend/worker/service` 放置平台投递/采集服务模板和各平台实现。
- `src/main/java/com/wh/jobsbackend/worker/session` 放置用户级自动化会话和运行时注册表。
- `src/main/java/com/wh/jobsbackend/worker/dto` 放置 worker 进度消息 DTO。
- `src/main/java/com/wh/jobsbackend/worker/utils` 放置平台枚举、任务模型和 Playwright 工具。
- `src/main/resources/application.yaml` 放置通用应用配置。
- `src/main/resources/application-dev.yaml` 放置 PostgreSQL 开发环境数据源配置。
- `src/main/resources/application.properties` 放置额外的 Spring Boot 属性。
- `src/main/resources/db/migration` 放置 Flyway 版本化迁移脚本，包含用户认证、用户隔离、运行中任务保护、运行时表和参考数据表迁移。
- `src/test/java/com/wh/jobsbackend` 放置后端 JUnit 测试，覆盖应用上下文、认证、安全配置、用户隔离、SSE、参考数据、迁移和 worker 页面模型/并发行为。
- `front/app` 放置 Next.js App Router 页面，包含首页、登录、Boss、51job、猎聘、智联、平台分析、基础数据、环境配置和 AI 配置页面。
- `front/app/components` 放置前端页面级共享组件，如侧边栏、页头和内容区域。
- `front/components` 放置前端通用组件和认证组件，`front/components/ui` 放置基础 UI 组件。
- `front/lib` 放置前端 API 客户端、认证存储、SSE、薪资、工具函数和平台请求工具。
- `front/scripts` 放置前端启动、配置加载、静态构建复制、数据迁移和回归脚本。
- `front/docs/superpowers` 放置前端相关 agentic workflow 规格和实施计划。
- `front/server.config.js` 放置前端端口、API 地址和应用信息配置。
- `docs/frontend-backend-integration.md` 放置前后端联调接口说明。
- `docs/superpowers/plans` 放置后端相关 agentic workflow 实施计划。
- `db/getjobs.db` 是本地 SQLite 数据库文件，属于运行时数据，不应作为源码变更提交。
- `target/`、`front/.next`、`front/out`、`front/node_modules` 和 `src/main/resources/dist` 属于构建或依赖输出，除非用户明确要求，不应作为源码变更提交。

## 平台说明

本项目当前支持 Boss 直聘、51job、猎聘和智联招聘四个平台。每个平台都有独立的配置实体、Mapper、Service、worker 页面模型和前端页面；修改平台逻辑时应先确认对应平台的路由、配置字段、数据表、登录状态、Cookie 保存和任务进度机制。

- Boss 直聘：后端接口主要在 `BossController`、`BossConfigController` 和 `BossAnalyticsController`，路由前缀为 `/api/boss` 与 `/api/boss/config`；worker 代码位于 `worker/boss`、`worker/manager/platform/BossPlaywrightHandler` 和平台服务实现附近。Boss 支持 `/api/boss/login`、`/api/boss/start`、`/api/boss/stop`、`/api/boss/status` 和 `/api/boss/stream`，任务进度通过 SSE 推送。配置、黑名单、岗位数据和 Cookie 都是用户相关数据，修改时必须保留用户隔离。
- 51job：后端接口集中在 `JobController` 中，保留 `/api/51job/...` 路由；worker 代码位于 `worker/job51` 和平台服务实现附近。51job 支持配置读写、扫码登录触发、登录状态、Cookie 读取/保存、启动/停止、健康检查、统计、列表、reload 和 `/api/51job/stream` 进度 SSE；全局登录状态流为 `/api/jobs/login-status/stream`。平台标识在通用 Cookie 与进度消息中使用 `51job`，Java 包和类名使用 `job51`/`Job51`。
- 猎聘：后端接口在 `LiepinController`，路由前缀为 `/api/liepin`；worker 代码位于 `worker/liepin` 和平台服务实现附近。猎聘支持配置、登录状态、登录触发、Cookie 读取/保存、启动/停止、健康检查、统计和列表接口；当前任务进度主要通过 `/api/liepin/status` 轮询，不要默认假设已有平台专属 SSE。修改猎聘字段映射时注意职位 ID、岗位链接和投递状态字段与其他平台不完全一致。
- 智联招聘：后端接口在 `ZhilianController`，路由前缀为 `/api/zhilian`；worker 代码位于 `worker/zhilian` 和平台服务实现附近。智联支持配置、城市/薪资选项、登录触发、登录状态、Cookie 读取/保存、启动/停止、投递按钮检查、健康检查、统计和列表接口；当前任务进度主要通过 `/api/zhilian/status` 轮询，不要默认假设已有平台专属 SSE。智联配置与岗位数据同样需要按当前用户隔离。

新增平台或调整任一平台能力时，需要同步更新本节、`docs/frontend-backend-integration.md`、平台枚举/常量、前端平台页面与 `front/lib` 中的平台请求封装，并补充或更新对应的平台页面模型、服务和用户隔离测试。

## 平台调用链路

平台功能通常按“前端页面 -> Controller -> 应用 Service -> worker service/handler -> 数据表或进度流 -> 前端刷新”的方向排查。修改任一环节时，要同时检查认证 token 注入、当前用户获取、运行中任务保护、Cookie 归属和任务进度回传。

通用链路：

- 配置读取链路：`front/app/<platform>` 页面调用平台 `GET /config` 或选项接口，进入对应 Controller，再到 `BossService`、`Job51Service`、`LiepinService` 或 `ZhilianService` 读取当前用户配置、平台选项和参考数据。worker 启动前统一通过 `ConfigService` 组装 `BossConfig`、`Job51Config`、`LiepinConfig` 或 `ZhilianConfig`。
- 配置保存链路：前端提交平台 `PUT /config`，Controller 做字段归一化后交给平台 Service 保存。多选、城市、薪资等字段要保持前端展示值、平台代码、参考数据和 worker 配置之间的映射一致。
- 登录链路：前端调用平台 `login` 或状态接口，Controller 使用当前用户的自动化会话打开或复用 Playwright 页面，平台 `*PlaywrightHandler` 负责进入扫码/登录状态检查。登录完成后通过平台 `save-cookie` 或通用 `/api/cookie/save?platform=...` 写入 `CookieService`。
- 启动任务链路：前端调用平台 `start`，Controller 先做登录状态和运行中任务保护，再调用对应 `*JobService`。平台 job service 读取 `ConfigService` 配置，驱动对应 `*PlaywrightHandler` 或页面模型执行搜索、采集、投递，并把结果写入平台岗位数据表。
- 进度回传链路：支持 SSE 的平台通过 `ProgressStreamService` 向当前用户 topic 发布 `connected`、`progress`、`ping` 等事件；不支持平台专属 SSE 的平台通过 `status` 查询当前任务状态。前端 SSE 必须使用支持自定义 `Authorization` header 的封装。
- 数据分析链路：worker 写入平台岗位数据表后，前端通过平台 `stats`、`list`、`reload` 等接口读取分析数据。查询必须按当前用户过滤，不能复用其他用户的岗位快照、配置或 Cookie。
- 停止/登出链路：前端调用平台 `stop` 或 `logout`，Controller 清理当前用户对应的平台任务、Playwright 运行时状态或 Cookie 状态。不要影响其他用户或其他平台正在运行的任务。

平台差异链路：

- Boss 直聘：前端目录为 `front/app/boss`；配置链路走 `BossConfigController` 和 `BossService`，黑名单也在 Boss 配置边界内；任务链路走 `BossController` -> `BossJobService` -> `BossPlaywrightHandler`/`worker/boss`；进度走 `/api/boss/stream` SSE；分析走 `BossAnalyticsController` 的 `stats`、`list` 和 `reload`。
- 51job：前端目录为 `front/app/51job`；接口集中在 `JobController`，但 worker 包和类名使用 `job51`/`Job51`；配置链路走 `Job51Service`，任务链路走 `JobController` -> `Job51JobService` -> `Job51PlaywrightHandler`/`worker/job51`；进度走 `/api/51job/stream` SSE，全局登录状态走 `/api/jobs/login-status/stream`；平台参数字符串使用 `51job`。
- 猎聘：前端目录为 `front/app/liepin`；接口走 `LiepinController`，配置与数据服务走 `LiepinService`；任务链路走 `LiepinController` -> `LiepinJobService` -> `LiepinPlaywrightHandler`/`worker/liepin`；当前进度主要通过 `/api/liepin/status` 轮询；字段映射要特别检查职位 ID、岗位链接、薪资、投递状态和参考数据选项。
- 智联招聘：前端目录为 `front/app/zhilian`；接口走 `ZhilianController`，配置与数据服务走 `ZhilianService`；任务链路走 `ZhilianController` -> `ZhilianJobService` -> `ZhilianPlaywrightHandler`/`worker/zhilian`；当前进度主要通过 `/api/zhilian/status` 轮询；智联额外有 `/api/zhilian/apply-button-check`，修改投递逻辑时要同步检查按钮识别和登录态判断。

## 本地命令

本工作区使用 Windows PowerShell。

优先使用 Maven Wrapper：

```powershell
.\mvnw "-Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository" test
```

如果本机 Maven 可用，也可以直接使用：

```powershell
mvn "-Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository" test
```

运行指定测试：

```powershell
mvn "-Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository" "-Dtest=ClassNameTest,OtherTest" test
```

跳过测试构建：

```powershell
mvn "-Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository" -DskipTests package
```

本地启动应用：

```powershell
$env:JAVA_HOME='E:\jdk21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn --% -Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository -DskipTests spring-boot:run
```

默认服务端口为 `8889`。

前端开发服务：

```powershell
cd front
npm run dev
```

前端默认绑定 `127.0.0.1:6866`，API 基地址来自 `front/server.config.js`，当前为 `http://localhost:8889`。

前端静态构建：

```powershell
cd front
npm run build
```

构建并复制静态产物到后端资源目录：

```powershell
cd front
npm run build:prod
```

`build:prod` 会执行 `next build` 并把 `front/out` 复制到 `src/main/resources/dist`。

前端校验和回归脚本：

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

## 运行配置

默认激活的 Spring profile 是 `dev`。

重要环境变量：

- `DB_URL`，默认 `jdbc:postgresql://127.0.0.1:5432/jobs_backend`
- `DB_USERNAME`，默认 `jobs_backend`
- `DB_PASSWORD`，默认 `jobs_backend`
- `JWT_SECRET`，当前存在本地开发默认值，生产环境不得使用默认值

应用日志写入 `target/logs/get-jobs.log`。

前端运行配置在 `front/server.config.js`：

- `port`：默认 `6866`
- `development.hostname`：默认 `127.0.0.1`
- `production.hostname`：默认 `0.0.0.0`
- `api.baseUrl`：默认 `http://localhost:8889`

`front/next.config.ts` 使用静态导出 `output: 'export'`，并把 `server.config.js` 中的 API 地址注入到 `NEXT_PUBLIC_API_BASE_URL`。

## 开发约定

- 修改范围应严格贴合用户请求。
- 遵循现有 controller、service、entity、mapper、stream、worker 包边界。
- 优先使用当前类或附近代码已有的 Spring 写法和注入风格。
- 保持 Java 21 兼容。
- 使用 MyBatis-Plus 时，保持与附近代码一致的 query wrapper 风格。
- 修改行为时添加或更新聚焦测试。
- 不提交运行时数据、本地数据库、日志或 `target` 输出。
- 前端修改遵循现有 Next.js App Router、Tailwind 和组件拆分方式，优先复用 `front/lib/api-client.ts`、`front/components/auth` 和 `front/components/ui`。
- 前端页面默认通过 JWT Bearer Token 调用后端接口，私有接口必须经过登录态保护和 token 注入。
- SSE 接口需要支持自定义 `Authorization` header，避免直接使用无法设置 header 的原生 `EventSource`。
- 参考数据、平台配置、职位数据、Cookie 和任务状态均属于用户相关或业务关键数据，变更时检查后端权限和前端缓存/认证语义。
- 前端静态产物 `front/.next`、`front/out`、`front/node_modules` 和后端 `src/main/resources/dist` 属于构建输出，除非用户明确要求，不应作为源码变更提交。
- 不改写无关文件，不做无关格式化。

## 安全与用户隔离

应用使用无状态 JWT 认证。

公开接口目前由 `SecurityConfig` 控制；除非有明确产品理由，其余接口都应要求认证。

私有数据必须按用户隔离。处理私有表或服务时：

- 通过 `CurrentUserService` 获取当前用户。
- 插入和更新时写入 `user_id`。
- 查询时按当前 `user_id` 过滤。
- 避免对私有数据做全局的“第一条”“最新一条”或未限定用户的查询。
- 修改 Cookie、配置、AI 设置、平台配置、职位数据、参考数据或任务状态时，保留或补充用户隔离测试。

前端涉及 Cookie、配置、AI 设置、平台配置、职位数据、参考数据或任务状态的页面和请求层也必须保持用户隔离语义：切换用户或退出登录时，应清理本地认证状态，并避免复用前一用户的私有数据。

## 数据库与迁移

Flyway 已启用，迁移路径为 `classpath:db/migration`。

修改数据库结构时：

- 在 `src/main/resources/db/migration` 下新增版本化 SQL 迁移脚本。
- 不修改已经应用过的迁移，除非用户明确要求且数据库状态确定可丢弃。
- 迁移脚本保持 PostgreSQL 兼容。
- 测试数据库行为时，优先使用 PostgreSQL mode 的 H2；只有变更明确依赖 PostgreSQL 特性时才使用真实 PostgreSQL。
- 涉及用户隔离、运行中任务保护、参考数据或表名规范化时，同步更新对应迁移测试。

## Playwright Worker 注意事项

`PlaywrightManager` 会启动可见 Chromium，并使用固定 CDP 端口。它属于有明显运行时副作用的代码。

修改 worker 代码时：

- 小心共享 browser、context、page 字段的并发访问。
- 普通单元测试不要触发可见浏览器自动化。
- 应用上下文 smoke test 中尽量 mock worker 依赖。
- 平台常量和平台专用 locator 尽量保留在对应平台包附近。
- 平台流程优先放在 `worker/<platform>`、`worker/service` 或 `worker/manager/platform` 的既有边界内。
- 修改平台逻辑时检查 Cookie 处理、登录状态、用户自动化会话和任务进度推送行为。

## 测试要求

窄范围修改先运行定向测试；影响共享应用行为时，再运行完整测试套件。

相关现有测试：

- `ApplicationStackSmokeTest`：应用上下文启动
- `DataMapperConfigTest`：数据映射配置
- `AuthControllerTest` 和 `AuthServiceRegisterTest`：认证流程
- `JwtTokenServiceTest` 和 `SecurityConfigTest`：Token 与安全配置行为
- `UserScopedServiceTest`：用户隔离持久化行为
- `UserTaskServiceTest`：用户任务状态行为
- `SseControllerTest`、`UserScopedSseControllerTest` 和 `ProgressStreamServiceTest`：SSE 和进度流行为
- `UserIsolationMigrationTest` 和 `ReferenceDataMigrationTest`：迁移覆盖
- `ReferenceDataControllerTest` 和 `ReferenceDataServiceTest`：参考数据接口和服务行为
- `ChineseEncodingRegressionTest`：中文编码回归
- `BossPageModelTest`、`Job51PageModelTest`、`LiepinPageModelTest`、`ZhilianPageModelTest`：平台页面模型行为
- `PlaywrightManagerConcurrencyTest`、`PlatformJobServiceTemplateTest` 和 `UserAutomationRegistryTest`：worker 并发、平台服务模板和用户自动化会话行为
- 前端认证和接口封装相关回归脚本位于 `front/scripts`，修改登录、token、SSE 或 API 基地址逻辑时优先运行对应 `npm run test:*` 脚本。
- 修改前端页面样式或交互时至少运行 `npm run lint`；涉及构建或静态部署时运行 `npm run build` 或 `npm run build:prod`。

如果因为缺少 Java、Maven、PostgreSQL、Playwright 浏览器二进制或网络访问而无法运行测试，必须明确说明。

## Agent Skills

当前项目已经可以使用 Git。需要查看变更、分支或提交历史时，优先使用 `git status`、`git diff`、`git log` 等命令确认当前状态。当前没有发现仓库内 issue tracker 配置；如果后续需要记录 issue，除非用户指定外部系统，否则使用 `.scratch/<feature>/` 下的本地 Markdown 文件。

需要 issue triage 时，默认使用以下标签：

- `needs-triage`
- `needs-info`
- `ready-for-agent`
- `ready-for-human`
- `wontfix`

当前项目按 single-context 处理。使用本 `AGENTS.md`、源码树和 `docs/superpowers/plans` 作为领域上下文。如果后续新增根目录 `CONTEXT.md` 或 `CONTEXT-MAP.md`，优先使用它们理解领域语言和架构意图。
