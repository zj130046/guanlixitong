# 前后端联调漏洞清单 (todo_opt)

> 本文档聚焦核心复杂处理链路中的**前后端联调漏洞**，采用 6 种技法交叉发掘。
> 版本: 2026-07-30 | 分支: feat-xwj | 3 个 Auditor Agent 并行审计 + 人工复核

---

## 发掘方法论

| 技法 | 说明 |
|------|------|
| **契约对比** | 逐一对比前端 API 调用签名 vs 后端 Controller 入参/返回值 |
| **状态机推演** | 穷举工单/会话/FAQ 所有状态转移路径，寻找非法跳转 |
| **数据流追踪** | 追踪一个字段从前端表单→API→Service→DB→响应→前端渲染的完整路径 |
| **异常路径注入** | 在每一步假设"如果这里抛异常/返回null/超时会怎样" |
| **并发窗口** | 寻找缺少 @Transactional / 乐观锁 / 幂等检查的多步写操作 |
| **权限边界** | 以不同角色身份遍历 API，检查是否存在越权路径 |

---

## 一、🔴 CRITICAL（共 12 项）

### 1.1 Schema 缺失 `ticket.attachment_urls` 列 → 运行时 SQL 错误

- **技法**: 契约对比 / 数据流追踪
- **文件**:
  - [TicketEntity.java:23](ai-ticket-server/src/main/java/com/aics/ticket/ticket/TicketEntity.java#L23) — 声明 `public String attachmentUrls`
  - [schema-h2.sql:46-62](ai-ticket-server/src/main/resources/sql/schema-h2.sql#L46-L62) — ticket 表无此列
  - [schema.sql](ai-ticket-server/src/main/resources/sql/schema.sql) — MySQL 同样缺失
- **描述**: MyBatis-Plus 自动映射 `attachmentUrls` → `attachment_urls`，但两个 DDL 都未定义该列。任何涉及附件的工单操作都会抛出 `BadSqlGrammarException`。
- **修复**: 在两个 schema 的 `ticket` 表中添加 `attachment_urls TEXT`。

### 1.2 管理端导出使用了错误的 Token Key

- **技法**: 数据流追踪
- **文件**:
  - [admin-web/api/ticket.ts:73](ai-ticket-admin-web/src/api/ticket.ts#L73) — `localStorage.getItem('AI_TICKET_USER_TOKEN')`
  - [admin-web/FaqEntryView.vue:309](ai-ticket-admin-web/src/views/FaqEntryView.vue#L309) — 同上
- **描述**: admin-web 的 `TOKEN_KEY = 'AI_TICKET_ADMIN_TOKEN'`，但两处 raw fetch 硬编码了用户端的 key。管理员导出 CSV 时因 token 为空而 401。
- **修复**: 改为 `AI_TICKET_ADMIN_TOKEN` 或从 client.ts 导入 `TOKEN_KEY`。

### 1.3 Docker Compose 环境变量与 application.yml 不匹配 → AI 永远用 Mock

- **技法**: 数据流追踪
- **文件**:
  - [docker-compose.yaml:78-79](docker-compose.yaml#L78-L79) — `APP_AI_API_KEY`, `APP_AI_BASE_URL`
  - [application.yml:38-39](ai-ticket-server/src/main/resources/application.yml#L38-L39) — 读取 `${DEEPSEEK_API_KEY:...}`, `${DEEPSEEK_BASE_URL:...}`
- **描述**: `@Value` 占位符名称（`DEEPSEEK_API_KEY`）与 Docker 环境变量名（`APP_AI_API_KEY`）不同。Spring 的 `${}` 不做 relaxed binding → 永远解析为默认值 → `AiConfig` 回退到 `MockAiChatClient`。
- **修复**: Docker Compose 中改为 `DEEPSEEK_API_KEY` 和 `DEEPSEEK_BASE_URL`。

### 1.4 工单操作无状态机校验（除 accept/archive 外所有操作）

- **技法**: 状态机推演
- **文件**: [TicketService.java:494-503](ai-ticket-server/src/main/java/com/aics/ticket/ticket/TicketService.java#L494-L503)
- **描述**: `updateStatus()` 直接覆盖 `ticket.status`，不校验当前状态。可绕过整条流程：CREATED→COMPLETED、ARCHIVED→PROCESSING 等非法跳转均被允许。
- **修复**: 加入状态转移白名单校验。

### 1.5 工单详情/操作无归属校验 → 越权查看与操作

- **技法**: 权限边界
- **文件**:
  - [TicketService.java:199](ai-ticket-server/src/main/java/com/aics/ticket/ticket/TicketService.java#L199) — `get(id)` 不校验 userId
  - [TicketService.java:203-216](ai-ticket-server/src/main/java/com/aics/ticket/ticket/TicketService.java#L203-L216) — `accept()` 不检查 assignee
- **描述**: 用户 A 可通过修改 URL ID 查看用户 B 的工单。坐席 B 可接单/完结坐席 A 已分配的工单。
- **修复**: `get()` 中根据角色校验归属；`accept()` 中检查 `assigneeAgentId` 是否为 null 或当前坐席。

### 1.6 `CurrentIdentity.currentIdOrDefault` 回退到 ID=1 → 静默身份错误

- **技法**: 异常路径注入 / 权限边界
- **文件**: [TicketService.java:52-54](ai-ticket-server/src/main/java/com/aics/ticket/ticket/TicketService.java#L52-L54) 等 7 处
- **描述**: 当 Session 中 loginId 的前缀不匹配时（如 USER token 调 AGENT 接口），回退到 `1L`，所有操作以 admin/agent #1 身份执行，审计日志被污染。
- **修复**: 回退时应抛 `BusinessException(403, "无权访问")` 而非静默使用默认 ID。

### 1.7 统计接口无角色保护 → 任何登录用户可查看全局数据

- **技法**: 权限边界
- **文件**:
  - [SaTokenConfig.java:48-52](ai-ticket-server/src/main/java/com/aics/ticket/config/SaTokenConfig.java#L48-L52) — ADMIN 拦截器仅覆盖 `/admin/**`
  - [StatisticsController.java](ai-ticket-server/src/main/java/com/aics/ticket/statistics/StatisticsController.java) — 路径为 `/statistics/**`
- **描述**: `/statistics/**` 不在 ADMIN 角色拦截器中，普通用户登录后可查看全平台敏感统计数据。
- **修复**: ADMIN 拦截器增加 `/statistics/**`。

### 1.8 满意度提交接受裸 Entity → ID/时间戳注入

- **技法**: 契约对比 / 权限边界
- **文件**: [StatisticsController.java:276-279](ai-ticket-server/src/main/java/com/aics/ticket/statistics/StatisticsController.java#L276-L279)
- **描述**: `submitSatisfaction(@RequestBody SatisfactionEntity request)` 直接接受完整 entity，攻击者可注入 `id` 覆盖已有评价、伪造 `createdAt`、伪装 `userId`。
- **修复**: 使用专用 DTO（仅 `ticketId` + `score`），从 Session 获取 userId，服务端设置 id=null 和 createdAt。

### 1.9 统计方法全表加载 → OOM 风险

- **技法**: 异常路径注入
- **文件**:
  - [TicketService.java:458,476](ai-ticket-server/src/main/java/com/aics/ticket/ticket/TicketService.java#L458) — `statusDistribution()` 全表加载
  - [StatisticsController.java:181,201,248](ai-ticket-server/src/main/java/com/aics/ticket/statistics/StatisticsController.java#L181) — 同上
- **描述**: 多个统计方法使用 `selectList(new QueryWrapper<>())` 将全表加载到内存再聚合。百万级数据直接 OOM。
- **修复**: 使用 SQL `GROUP BY` 聚合查询替代内存聚合。

### 1.10 坐席排行 N+1 查询 → 100 坐席 = 201 次 DB 查询

- **技法**: 数据流追踪
- **文件**: [StatisticsController.java:223-242](ai-ticket-server/src/main/java/com/aics/ticket/statistics/StatisticsController.java#L223-L242)
- **描述**: 对每个坐席单独执行 2 次 COUNT，50 个坐席 = 101 次查询。
- **修复**: 使用 SQL `GROUP BY assignee_agent_id` 一次查询。

### 1.11 CSV 导出 N+1 查询 → 10000 工单 = 20001 次 DB 查询

- **技法**: 数据流追踪
- **文件**: [TicketService.java:298-316](ai-ticket-server/src/main/java/com/aics/ticket/ticket/TicketService.java#L298-L316)
- **描述**: `exportCsv()` 对每个工单调用 `getRequesterName()` + `getAgentName()`，各执行一次 `selectById`。
- **修复**: 批量收集所有 userId/agentId，一次 `IN` 查询后构建 HashMap 查找。

### 1.12 `application.yml` 默认 datasource 为 MySQL，但 dev profile 仅含 H2 驱动 → 启动失败

- **技法**: 契约对比
- **文件**:
  - [application.yml:11-14](ai-ticket-server/src/main/resources/application.yml#L11-L14) — MySQL driver/URL
  - [pom.xml:79-92](ai-ticket-server/pom.xml#L79-L92) — dev profile 仅加 H2
- **描述**: `mvn spring-boot:run`（默认 dev profile）时，Spring 尝试加载 `com.mysql.cj.jdbc.Driver` 但 classpath 只有 H2 → `ClassNotFoundException`。
- **修复**: 创建 `application-dev.yml` 覆盖 datasource 为 H2 配置。

---

## 二、🟡 HIGH（共 14 项）

### 2.1 SaTokenConfig 公开路径列表与设计文档不一致

- **技法**: 契约对比
- **文件**: [SaTokenConfig.java:28-40](ai-ticket-server/src/main/java/com/aics/ticket/config/SaTokenConfig.java#L28-L40) vs [design.md:472-490](ref/design.md#L472-L490)
- **描述**: design.md 将 `/faq/categories`、`/faq/entries`、`/statistics/overview` 等标为"公共接口"，但 SaTokenConfig 未排除这些路径。
- **修复**: 二选一：补全排除列表或更新 design.md。

### 2.2 FAQ 导入 CSV 逗号分割不处理引号转义

- **技法**: 异常路径注入
- **文件**: [FaqController.java:170](ai-ticket-server/src/main/java/com/aics/ticket/kb/FaqController.java#L170)
- **描述**: `line.split(",", -1)` 会将引号内的逗号误拆为列分隔符。
- **修复**: 使用状态机解析或引入 `commons-csv`。

### 2.3 FAQ 反馈 `helpful` 布尔解析 Bug → "true" 字符串被判为 false

- **技法**: 契约对比
- **文件**: [FaqController.java:61](ai-ticket-server/src/main/java/com/aics/ticket/kb/FaqController.java#L61)
- **描述**: `Boolean.TRUE.equals(request.get("helpful"))` 对字符串 `"true"` 返回 false（类型不匹配），`"1".equals("true")` 也返回 false。前端的 `true` 被记录为"无帮助"。
- **修复**: 使用 `"true".equalsIgnoreCase(String.valueOf(...))`。

### 2.4 `FaqFeedback.userId` 从未被填充 → 所有反馈匿名

- **技法**: 数据流追踪
- **文件**: [FaqController.java:56-63](ai-ticket-server/src/main/java/com/aics/ticket/kb/FaqController.java#L56-L63)
- **描述**: Entity 声明了 `userId` 字段，`submitFeedback()` 未赋值，永远为 null。
- **修复**: `feedback.userId = CurrentIdentity.currentIdOrDefault("USER", null)`。

### 2.5 SQL 种子数据 docker vs H2 不一致 → 文档密码无法登录

- **技法**: 契约对比
- **文件**:
  - [docker-compose.yaml:27-29](docker-compose.yaml#L27-L29) — 文档写 `user001/user123`
  - [seed.sql](ai-ticket-server/src/main/resources/sql/seed.sql) — 实际为 `demo/demo123456`
- **描述**: Docker 部署使用 `seed.sql`，账户与文档/H2 完全不同。
- **修复**: 统一两份种子数据。

### 2.6 无外键约束 → 删记录产生孤儿数据

- **技法**: 状态机推演
- **文件**: [schema.sql](ai-ticket-server/src/main/resources/sql/schema.sql) / [schema-h2.sql](ai-ticket-server/src/main/resources/sql/schema-h2.sql)
- **描述**: 10+ 逻辑外键关系无任何 `FOREIGN KEY` 约束。删除 ticket → `ticket_event`/`satisfaction` 孤儿；删除 faq_entry → `faq_entry_tag`/`faq_feedback` 孤儿。
- **修复**: 添加 FK 约束并决定 `ON DELETE CASCADE` vs `SET NULL`。

### 2.7 FAQ 标签/分类删除无级联清理

- **技法**: 状态机推演
- **文件**:
  - [FaqController.java:119](ai-ticket-server/src/main/java/com/aics/ticket/kb/FaqController.java#L119) — `deleteTag` 不清理 join 表
  - [FaqService.java:63](ai-ticket-server/src/main/java/com/aics/ticket/kb/FaqService.java#L63) — `deleteCategory` 不处理关联 entry
- **修复**: 删除前清理关联表或禁止删除有关联数据的记录。

### 2.8 `register()` 无 `@Transactional` → 重名并发

- **技法**: 并发窗口
- **文件**: [AuthService.java:73-98](ai-ticket-server/src/main/java/com/aics/ticket/auth/AuthService.java#L73-L98)
- **描述**: 三表查重 → 插入之间无事务保护，并发同用户名注册可双双通过检查。
- **修复**: 加 `@Transactional` + 依赖 DB unique 约束兜底。

### 2.9 月度报表生成非幂等 → 并发生成重复

- **技法**: 并发窗口
- **文件**: [StatisticsController.java:297-348](ai-ticket-server/src/main/java/com/aics/ticket/statistics/StatisticsController.java#L297-L348)
- **描述**: SELECT-then-INSERT 无事务保护，两并发请求可能插入两条同月报表。
- **修复**: `INSERT ... ON DUPLICATE KEY UPDATE` + `@Transactional`。

### 2.10 `adjustPriority` 不校验枚举值

- **技法**: 异常路径注入
- **文件**: [TicketService.java:265-276](ai-ticket-server/src/main/java/com/aics/ticket/ticket/TicketService.java#L265-L276)
- **描述**: 传入 `"OVERDRIVE"` 直接入库，后续 `TicketPriority.valueOf()` 会抛异常。
- **修复**: 事前 `TicketPriority.valueOf(newPriority)` 校验。

### 2.11 密码自动升级失败返回"密码错误"

- **技法**: 异常路径注入
- **文件**: [AuthService.java:225-227](ai-ticket-server/src/main/java/com/aics/ticket/auth/AuthService.java#L225-L227)
- **描述**: 密码验证正确但 BCrypt 升级写入失败时 → 整个方法返回 false → 用户看到"用户名或密码错误"。
- **修复**: 升级失败应单独 try-catch，不影响登录结果。

### 2.12 `autoAssign` WHERE 条件 OR 语义混乱

- **技法**: 契约对比
- **文件**: [TicketService.java:92-99](ai-ticket-server/src/main/java/com/aics/ticket/ticket/TicketService.java#L92-L99)
- **描述**: `groupWrapper.and(...).or().like("name", department)` 生成 `(scene LIKE ? OR name LIKE ?) OR name LIKE ?`，department 条件绕过 category 过滤。
- **修复**: 明确 AND/OR 优先级，使用嵌套 wrapper。

### 2.13 前端 axios `baseURL` 默认直连后端 → Docker 中绕过 Nginx

- **技法**: 数据流追踪
- **文件**: 三个前端 `client.ts:7`
- **描述**: `VITE_API_BASE_URL` 未设置时默认 `http://localhost:8080/api`，Docker 部署下绕过 Nginx 代理，SSE buffering 失效。
- **修复**: Dockerfile.frontend 构建时设置 `VITE_API_BASE_URL=/api`。

### 2.14 Sa-Token 无 Redis/JWT 配置 → 多实例/重启即登出

- **技法**: 并发窗口 / 异常路径注入
- **文件**: [application.yml:24-31](ai-ticket-server/src/main/resources/application.yml#L24-L31)
- **描述**: `token-style: uuid` + 内存存储 → 服务重启所有用户登出；多实例负载均衡时 session 不共享。
- **修复**: 生产环境配置 Redis 或 JWT 模式。

---

## 三、🟡 MEDIUM（共 14 项）

### 3.1 前端 401/403 清除 token 但不跳转登录页

- **技法**: 异常路径注入
- **文件**: 三个前端 `client.ts:32-35`
- **描述**: 拦截器 `localStorage.removeItem(TOKEN_KEY)` 但 Pinia store 仍为已登录状态，页面不跳转，用户困在当前页面反复 401。
- **修复**: 触发 `window.location.href = '/login'` 或调用 router.push。

### 3.2 GET 请求错误被静默吞掉（约 15 处）

- **技法**: 异常路径注入
- **文件**: 三个前端 `client.ts:24-29` — `if (method !== 'GET')` 跳过错误提示
- **描述**: GET API 返回 500/业务错误时，拦截器只 reject 不弹 toast。Dashboard/FaqView 等页面 catch 块为空 → 数据显示 0/空但用户不知情。
- **修复**: GET 错误也应 toast 或页面级展示 error state + 重试按钮。

### 3.3 `SimilarityConfigView` 的"保存配置"是空操作

- **技法**: 契约对比
- **文件**: [SimilarityConfigView.vue:117-120](ai-ticket-admin-web/src/views/SimilarityConfigView.vue#L117-L120)
- **描述**: `handleSave()` 只弹 toast，不调任何 API。刷新页面配置丢失。
- **修复**: 添加 `PUT /admin/faq/config` 后端接口并前端对接。

### 3.4 `SimilarityConfigView` 匹配测试失败时返回硬编码 Mock 数据

- **技法**: 异常路径注入
- **文件**: [SimilarityConfigView.vue:139-148](ai-ticket-admin-web/src/views/SimilarityConfigView.vue#L139-L148)
- **描述**: catch 块返回 mock 假数据 → 管理员永远看到"匹配成功 - 宿舍水管漏水怎么办？"。
- **修复**: catch 中设置 `testResult = null` + `ElMessage.error`。

### 3.5 `RegisterView` 绕过 Pinia Store 的 login action

- **技法**: 数据流追踪
- **文件**: [RegisterView.vue:86-93](ai-ticket-user-web/src/views/RegisterView.vue#L86-L93)
- **描述**: 注册成功后手动设置 `auth.token/username/identityType` + 直接写 localStorage，跳过 store.login()。若 login() 未来增加副作用（如上线通知），注册用户将缺失。
- **修复**: 改为调用 `auth.login()`。

### 3.6 DashboardView 读取 `totalTickets` 但 API 返回 `todayConsultations`

- **技法**: 契约对比
- **文件**: [DashboardView.vue:166](ai-ticket-user-web/src/views/DashboardView.vue#L166) vs [StatisticsController.java:92](ai-ticket-server/src/main/java/com/aics/ticket/statistics/StatisticsController.java#L92)
- **描述**: 前端读 `data.totalTickets`，后端返回的 key 是 `todayConsultations` → 前端取到 undefined → 永远显示 0。
- **修复**: 统一字段名。

### 3.7 `GlobalExceptionHandler` 将 BusinessException(401/404) 统一返回 HTTP 400

- **技法**: 契约对比
- **文件**: [GlobalExceptionHandler.java:15-19](ai-ticket-server/src/main/java/com/aics/ticket/common/GlobalExceptionHandler.java#L15-L19)
- **描述**: `@ResponseStatus(HttpStatus.BAD_REQUEST)` 硬编码为 400，忽略 `BusinessException.code`（401/404）。
- **修复**: 移除静态注解，动态设置 `response.setStatus(exception.getCode())`。

### 3.8 缺失关键异常处理器 → 500 代替 400/405/415

- **技法**: 异常路径注入
- **文件**: [GlobalExceptionHandler.java](ai-ticket-server/src/main/java/com/aics/ticket/common/GlobalExceptionHandler.java)
- **描述**: 无 `MethodArgumentTypeMismatchException`（传 abc 给 Long → 应 400）、`HttpRequestMethodNotSupportedException`（应 405）、`HttpMessageNotReadableException`（应 400）的专用处理器。
- **修复**: 添加对应 `@ExceptionHandler`。

### 3.9 Docker Compose 无文件上传 volume → 重启丢附件

- **技法**: 数据流追踪
- **文件**: [docker-compose.yaml:64-89](docker-compose.yaml#L64-L89)
- **描述**: 容器内 `/app/uploads` 无 volume 挂载，重启后所有附件丢失但 DB 中的 URL 仍存在。
- **修复**: 添加 `- upload_data:/app/uploads`。

### 3.10 `Dockerfile.frontend` npm/pnpm 混用

- **技法**: 契约对比
- **文件**: [Dockerfile.frontend](Dockerfile.frontend#L9-L10)
- **描述**: 复制 `pnpm-lock.yaml` 但执行 `npm install`，锁文件不生效 → 依赖版本不可复现。
- **修复**: 统一使用 pnpm 或移除 pnpm 文件。

### 3.11 `TicketService.updateStatus` 隐式自动分配坐席

- **技法**: 状态机推演
- **文件**: [TicketService.java:497](ai-ticket-server/src/main/java/com/aics/ticket/ticket/TicketService.java#L497)
- **描述**: `if (ticket.assigneeAgentId == null) ticket.assigneeAgentId = agentId` → 任何人都可以通过调 process/complete 隐式认领未分配工单。
- **修复**: 移除隐式分配，要求显式 accept。

### 3.12 CSV 导出 filename 编码不完整

- **技法**: 契约对比
- **文件**: [TicketController.java:177-178](ai-ticket-server/src/main/java/com/aics/ticket/ticket/TicketController.java#L177-L178)
- **描述**: `Content-Disposition` filename 未 URL 编码，含中文时浏览器可能乱码。
- **修复**: `URLEncoder.encode(filename, StandardCharsets.UTF_8)`。

### 3.13 上传/导出使用 raw fetch 绕过 axios 拦截器

- **技法**: 数据流追踪
- **文件**:
  - [user-web/api/ticket.ts:60-73](ai-ticket-user-web/src/api/ticket.ts#L60-L73) — `uploadFile`
  - [admin-web/views/FaqEntryView.vue:309](ai-ticket-admin-web/src/views/FaqEntryView.vue#L309) — `handleExportCsv`
- **描述**: 手动 fetch + 手动读 token → 不经过 axios 拦截器的统一错误处理/响应解包。
- **修复**: 使用 axios instance + `responseType: 'blob'` 或封装统一 fetch wrapper。

### 3.14 `FaqController.createTag` "null" 字符串注入

- **技法**: 异常路径注入
- **文件**: [FaqController.java:114](ai-ticket-server/src/main/java/com/aics/ticket/kb/FaqController.java#L114)
- **描述**: `String.valueOf(request.get("name"))` 在 name 为 null 时返回字符串 `"null"` → 创建名为 "null" 的标签。
- **修复**: 显式 null 检查 + 返回 400。

---

## 四、🟢 LOW（共 8 项）

### 4.1 `formatTime()` 在 5+ 个组件中重复定义，格式不统一

- 提取为 `utils/format.ts` 共享。

### 4.2 `faq_entry_tag` 关联表已建但从未使用 → 死表

- 要么实现标签关联功能，要么删除该表。

### 4.3 用户端 Dashboard 统计数据是全局而非个人数据

- `totalTickets` 显示全平台工单数 → 用户误以为是自己名下的。

### 4.4 LoginView 预填默认凭证 → 生产安全隐患

- `username: 'user001', password: 'user123'` 应改为空字符串。

### 4.5 `createForUser` 无论 autoAssign 成功与否都设 status=ASSIGNED

- `CREATED` 枚举值成为死代码 → 应区分"已分配"与"进池"。

### 4.6 `timeoutWarnings` 未排除 `ARCHIVED` 工单

- 已归档的超时工单仍出现在预警列表中。

### 4.7 前端登录/注册表单缺少客户端校验规则

- 空用户名/密码可直接提交。

### 4.8 `TicketCreateView` `handleReset` 不重置 department（无 prop）

- 加显式 `form.department = ''`。

---

## 统计

### 按严重程度

| 严重度 | 数量 | 关键主题 |
|--------|------|----------|
| 🔴 CRITICAL | 12 | Schema缺失、Docker env不匹配、状态机无校验、越权、OOM、启动失败 |
| 🟡 HIGH | 14 | 公开路径不一致、CSV解析Bug、布尔解析Bug、无FK约束、无事务保护、Nginx绕过 |
| 🟡 MEDIUM | 14 | 401不跳转、GET错误吞掉、配置空操作、Mock假数据、注册绕过Store、字段名不匹配 |
| 🟢 LOW | 8 | 重复代码、死表、默认凭证、死状态码 |
| **总计** | **48** | |

### 按发掘技法

| 技法 | 数量 |
|------|------|
| 契约对比 | 15 |
| 数据流追踪 | 10 |
| 异常路径注入 | 9 |
| 状态机推演 | 6 |
| 权限边界 | 5 |
| 并发窗口 | 3 |
