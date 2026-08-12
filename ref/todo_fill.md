# 待完善功能清单 (todo_fill)

> 本文档记录功能**已有骨架或部分实现**，但不完整或存在明显缺陷，需要补充完善的内容。

---

## 1. 账号鉴权模块

### 1.1 密码存储安全性
- **需求**: 用户/坐席/管理员密码安全存储
- **现状**: ✅ **已完成**: `PasswordConfig` 使用 `BCryptPasswordEncoder(10)`，`{bcrypt}` 格式存储。`{noop}` 明文密码登录时自动升级。
- **待完善**: 种子数据密码保留 `{noop}` 以兼容首次启动，生产环境应预生成 BCrypt 哈希。

### 1.2 登录验证码
- **需求**: 防暴力破解
- **现状**: 登录接口无验证码机制，无登录失败次数限制。
- **待完善**: 添加图形验证码或滑动验证，限制登录失败重试次数。

### 1.3 Token 刷新机制
- **需求**: 长期会话管理
- **现状**: Token 有效期 30 天但无刷新机制。前端仅将 Token 存储在 localStorage。
- **待完善**: 添加 Token 刷新端点，前端实现 Token 过期自动刷新。

---

## 2. 工单流程引擎模块

### 2.1 工单回访 (`FOLLOWING`) 状态 ✅
- **需求**: "接单、处理、回访、完结、驳回"
- **现状**: ✅ **已完成**: Agent 端 `TicketDetailView.vue` 已包含"标记跟进"按钮 + `follow()` API 对接。

### 2.2 工单超时自动提醒 ✅
- **需求**: "工单超时自动提醒"
- **现状**: ✅ **已完成**: `TicketTimeoutScheduler` 每 30 分钟执行 `@Scheduled` 定时任务，扫描超时/即将超时工单并打印日志告警。Agent 前端 `TimeoutWarningView.vue` 可通过 API 手动刷新。

### 2.3 工单优先级调整 ✅
- **需求**: "优先级调整"
- **现状**: ✅ **已完成**: Agent 端 `TicketDetailView.vue` 新增"调整优先级"按钮 + 弹窗 + `adjustPriority()` API。管理端 `TicketManageView.vue` 原有优先级编辑功能继续可用。

### 2.4 工单自动分配算法
- **需求**: "按分类/部门自动分配"
- **现状**: `TicketService.autoAssign()` 通过 `scene`/`name` 字段 LIKE 模糊匹配分类/部门到坐席组。匹配策略基于字符串相似度，可进一步优化。
- **待完善**: 建议使用精确映射表（工单分类 → 坐席组），而非 LIKE 模糊匹配。

---

## 3. AI 智能对话模块

### 3.1 快速问题功能 (q 参数)
- **需求**: 用户端 Dashboard 点击 FAQ 热门问题直接发起 AI 对话
- **现状**: DashboardView.vue 的 `goChatWithQuestion()` 跳转到 `/chat?q=问题文本`，但 AiChatView.vue 的 `onMounted` 未读取 `route.query.q`，导致快速问题预填充不生效。
- **待完善**: AiChatView.vue 的 `onMounted` 中需要读取 `q` 参数并在输入框中预填充，或直接调用 `handleSend()`。

### 3.2 人工坐席接管对话
- **需求**: 用户转人工后，坐席通过人工会话界面接管
- **现状**: Agent 端 `HumanChatView.vue` 路由为 `/chat/:conversationId`，但需要验证后端是否有坐席回复用户的功能（`ChatSenderType.AGENT` 已定义，需确认 ChatController 是否有坐席发送消息的接口）。
- **待完善**: 确保坐席可以在 HumanChatView 中发送消息，消息通过 SSE/WebSocket 实时推送给用户。

### 3.3 违规内容拦截增强
- **需求**: "违规内容拦截"
- **现状**: `ViolationGuardService` 采用硬编码关键词黑名单（约 20 个中文敏感词）+ 2000 字长度限制。
- **待完善**: 敏感词列表应外部化（配置文件或数据库），支持正则匹配，可考虑接入第三方内容安全 API。

---

## 4. 坐席与数据统计模块

### 4.1 用户端 Dashboard 统计 ✅
- **需求**: 用户首页展示个人相关统计数据
- **现状**: ✅ **已完成**: `DashboardView.vue` 接入 `getStatisticsOverview()` 获取真实 AI 回复率等数据。

### 4.2 统计 API 使用 ✅
- **需求**: 全平台统计看板
- **现状**: ✅ **已完成**: Admin 端 Dashboard 5 个 ECharts 图表全部接入真实 API。Agent 端 AgentStatsView 趋势+分类图表接入真实 API + 满意度评分。用户端 Dashboard 接入真实统计。

### 4.3 坐席满意度统计 ✅
- **需求**: "满意度统计"
- **现状**: ✅ **已完成**: AgentStatsView 的满意度卡片从 `getSatisfactionStats()` 获取真实均分。Admin 端 Dashboard/MonthlyReport 均接入满意度分布图表。

---

## 5. FAQ 知识库管理模块

### 5.1 语义匹配配置界面 ✅
- **需求**: "大模型语义相似度匹配配置"
- **现状**: ✅ **已完成**: `GET /admin/faq/config` API + SimilarityConfigView 在 `onMounted` 中加载真实配置。

### 5.2 FAQ 反馈统计 ✅
- **需求**: 用户对 FAQ 答案的反馈
- **现状**: ✅ **已完成**: `POST /faq/{entryId}/feedback` API + `FaqFeedback` Entity + 用户端 FaqView 调用 `submitFaqFeedback()`。

### 5.3 语义向量重建
- **需求**: 批量重建 FAQ 向量
- **现状**: `POST /admin/faq/vectors/rebuild` 已实现。创建/更新单条 FAQ 时同步调用 `generateVector()`。
- **待完善**: 向量生成可考虑异步执行以避免 API 超时。

---

## 6. 前端代码质量

### 6.1 组件复用
- **需求**: DRY 原则
- **现状**: 三个前端项目的 `src/components/` 目录均仅有 README.md 占位，无任何可复用 Vue 组件。`formatTime()`、`statusLabel()` 等工具函数在多个视图中重复实现。
- **待完善**: 提取通用组件（状态标签、时间线、文件上传、满意度评分）和工具函数（日期格式化、状态映射）。

### 6.2 前端相似代码
- **需求**: 三个前端项目代码高度相似
- **现状**: `api/client.ts`、`stores/auth.ts`、`layouts/BasicLayout.vue`、`assets/styles.css` 在三个项目中高度重复。仅 `LoginView.vue` 通过标题和凭证区分。
- **待完善**: 抽取公共包 `@ai-ticket/shared-ui` 或使用 monorepo 共享代码。

### 6.3 TypeScript 路径别名
- **需求**: 代码可维护性
- **现状**: 所有导入使用相对路径 (`../../api/...`、`../types/...`)，无 TypeScript path aliases 配置。
- **待完善**: 配置 `@/` 别名简化导入路径。

---

## 统计

| 模块 | 待完善功能数 |
|------|------------|
| 账号鉴权模块 | 2 |
| 工单流程引擎模块 | 1 |
| AI 智能对话模块 | 3 |
| 坐席与数据统计模块 | 0 |
| FAQ 知识库管理模块 | 1 |
| 前端代码质量 | 3 |
| **总计** | **7** |
