# AI 智能客服工单处理系统 - 后端服务

`ai-ticket-server` 是 AI 智能客服工单处理系统的统一后端服务仓库，面向用户端、客服坐席端和管理后台提供 API。

## 技术栈

- Java 17
- Spring Boot 3
- Maven
- MyBatis-Plus
- Sa-Token
- Spring AI / Spring AI Alibaba（预留接入）
- MySQL

## 功能模块

- 账号鉴权：用户、客服、管理员三身份登录，预留 Sa-Token 多身份隔离、在线状态、强制下线、密码重置。
- 工单流程引擎：创建、分配、接单、处理、回访、完结、驳回、超时预警、检索、归档导出。
- AI 智能对话：实时咨询、流式回复、转人工、历史会话、违规内容拦截。
- FAQ 知识库：分类、标准问答、关键词标签、导入导出、语义相似度配置。
- 坐席与统计：客服分组、主管督办、接待量、自动回复率、工单完结率、满意度、月报。

## 本地开发

1. 创建数据库：

```sql
CREATE DATABASE ai_ticket DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行初始化脚本：

```text
src/main/resources/sql/schema.sql
src/main/resources/sql/seed.sql
```

3. 配置本地环境变量或复制本地配置：

```bash
export DB_USERNAME=root
export DB_PASSWORD=your-password
```

4. 启动：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

默认接口前缀：`http://localhost:8080/api`

## AI 接入说明

默认使用 `MockAiChatClient`，不需要真实大模型 API Key 即可跑通流式响应接口。后续接入 Spring AI Alibaba 时，可启用 `ai-alibaba` Maven profile，并补充 DashScope/API Key 配置。

## API 草稿

见：`docs/api/openapi.yaml`
