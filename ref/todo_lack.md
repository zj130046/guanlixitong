# 缺失功能清单 (todo_lack)

> 本文档记录项目介绍 (`ref/project_intro.md`) 中明确提及但当前代码**完全未实现**的功能需求。

---

## 1. 账号鉴权模块

> ✅ **已完成**: 用户注册（`POST /user/auth/register`）、密码重置（`POST /user/auth/reset-password`）、强制下线（`POST /admin/auth/force-logout/{type}/{id}`）、BCrypt 密码哈希 + 自动升级、全局 SaInterceptor 认证拦截 + ADMIN 角色控制。仅剩登录失败限制和 Token 刷新机制待后续完善。

---

---

## 2. 工单流程引擎模块

> ✅ **已完成**: 归档 API（`POST /agent/tickets/{id}/archive`）、CSV 导出（`GET /admin/tickets/export`）、优先级调整（`POST /agent/tickets/{id}/adjust-priority`）、超时自动调度（`@Scheduled` 每30分钟）、工单附件上传（`POST /files/upload`）。

---

## 3. AI 智能对话模块

（当前实现基本覆盖所有需求，无明显缺失）

---

## 4. 坐席与数据统计模块

> ✅ **已完成**: 
> - **ECharts 可视化**: 管理端 Dashboard 5 个图表（趋势/饼图/柱状图/排行/满意度）+ 月度报表 4 个图表
> - **坐席端统计看板**: AgentStatsView 已有趋势+分类图表 + 满意度接入真实 API
> - **月度报表**: `MonthlyReportEntity/Mapper` + `POST /statistics/monthly-reports/generate` + 前端生成/导出/历史列表
> - **用户端 Dashboard**: 接入 `getStatisticsOverview()` 替换硬编码 82%

---

## 5. FAQ 知识库管理模块

> ✅ **已完成**:
> - **批量导入导出**: `POST /admin/faq/entries/import` (CSV 导入) + `GET /admin/faq/entries/export` (CSV 导出)
> - **标签系统**: `FaqTag` Entity/Mapper + `GET/POST/DELETE /admin/faq/tags` CRUD
> - **FAQ 反馈**: `POST /faq/{entryId}/feedback` + 用户端 FaqView 按钮已对接
> - **语义匹配配置**: `GET /admin/faq/config` + SimilarityConfigView 已接入真实 API

---

## 6. 通用功能

### 6.1 文件上传

> ✅ **已完成**: `POST /files/upload` + 前端集成 + `TicketEntity.attachmentUrls` 字段。

---

## 统计

| 模块 | 缺失功能数 |
|------|-----------|
| 账号鉴权模块 | 0 |
| 工单流程引擎模块 | 0 |
| AI 智能对话模块 | 0 |
| 坐席与数据统计模块 | 0 |
| FAQ 知识库管理模块 | 0 |
| 通用功能 | 0 |
| **总计** | **0** |
