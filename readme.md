# 生产实习课程任务 — AI 智能客服工单处理系统

## 项目简介

AI 智能客服工单处理系统面向线下商户、高校后勤、互联网中小企业，提供 FAQ 知识库 + RAG 智能问答 + 工单全流程流转 + 多角色坐席管理 + 数据统计分析的一体化工单平台。

## 项目结构

```
guanlixitong/
├── ai-ticket-server/          # Spring Boot 3 后端服务
├── ai-ticket-user-web/        # Vue 3 用户端
├── ai-ticket-agent-web/       # Vue 3 客服坐席端
├── ai-ticket-admin-web/       # Vue 3 管理后台
├── docker-compose.yaml        # Docker Compose 一键部署
├── Dockerfile.frontend        # 前端统一构建镜像
├── nginx.conf                 # Nginx 配置
├── ref/                       # 参考文档
│   ├── project_intro.md       # 项目介绍与需求
│   ├── todo_lack.md           # 缺失功能清单
│   ├── todo_fill.md           # 待完善功能清单
│   └── design.md              # 工程设计文档
└── readme.md                  # 本文件
```

## Docker Compose 一键部署

### 前置要求

- Docker Engine 20.10+
- Docker Compose v2
- **DeepSeek 或 DashScope API Key**（可选；分别驱动 AI 对话 / 向量模型。不配置时自动回退 Mock 演示模式，全流程仍可跑通）
- 首次构建需拉取后端构建镜像（约 450MB；也可让 `docker compose build` 自动拉取）：

  ```bash
  docker pull maven:3.9-eclipse-temurin-17
  docker pull eclipse-temurin:17-jre-alpine
  ```

### 快速启动

```bash
# 1. 设置 AI API Key（可选，不设置则用 Mock 演示模式）
export DEEPSEEK_API_KEY=sk-your-key-here
# export DASHSCOPE_API_KEY=sk-xxx        # 若用阿里云百炼（qwen 对话 + text-embedding 向量）

# 2. 构建并启动所有服务
docker compose up -d --build

# 3. 查看服务状态
docker compose ps

# 4. 停止所有服务
docker compose down

# 5. 停止并清除数据（重新开始）
docker compose down -v
```

### 访问地址

| 平台 | 地址 | 预置账号 | 密码 |
|------|------|----------|------|
| 用户端 (user-web) | http://localhost:3003 | user001 | user123 |
| 坐席端 (agent-web) | http://localhost:3002 | agent001 | agent123 |
| 管理后台 (admin-web) | http://localhost:3001 | admin | admin123 |
| 后端 API | http://localhost:8080/api | — | — |
| Swagger 文档 | http://localhost:8080/api/swagger-ui.html | — | — |

### 服务架构

```
┌─────────────────────────────────────────────────────┐
│                    Nginx :3001/:3002/:3003            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│  │admin-web │  │agent-web │  │ user-web │  前端     │
│  │  :3001   │  │  :3002   │  │  :3003   │           │
│  └──────────┘  └──────────┘  └──────────┘           │
│         │             │             │                │
│         └─────────────┼─────────────┘                │
│                       │ /api/*                       │
│                       ▼                              │
│            Spring Boot :8080 (后端)                  │
│                       │                              │
│                       ▼                              │
│              MySQL :3306 (数据库)                    │
└─────────────────────────────────────────────────────┘
```

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | `your-deepseek-api-key` |
| `DEEPSEEK_BASE_URL` | DeepSeek API 地址 | `https://api.deepseek.com` |
| `DASHSCOPE_API_KEY` | 阿里云百炼（DashScope）API 密钥 | 空（回退 Mock） |
| `DASHSCOPE_BASE_URL` | 阿里云百炼 API 地址 | `https://dashscope.aliyuncs.com` |

### 常用命令（run.sh）

仓库根目录的 [run.sh](run.sh) 封装了后端构建/运行、前端构建/运行、Docker 部署与冒烟测试等常用操作（可 `source run.sh` 后直接调用函数，或 `./run.sh <命令>` 调用）。

```bash
# 后端
./run.sh be_compile                 # 编译（dev profile）
./run.sh be_package                 # 打包（prod，与 Docker 镜像产物一致）
./run.sh be_run                     # 运行 dev/H2（默认 :8080），可加端口参数
./run.sh be_stop                    # 停止后端进程（默认 :8080）
./run.sh be_clean                   # mvn clean

# 前端
./run.sh fe_install                 # 三个前端安装依赖
./run.sh fe_dev user                # dev server（user|agent|admin|all，默认 all 后台）
./run.sh fe_build all               # 构建（含 vue-tsc 类型检查）
./run.sh fe_build_fast all          # 构建（跳过 vue-tsc，同 Docker）
./run.sh fe_stop                    # 停止前端 dev server

# Docker
./run.sh dk_images                  # 预拉后端构建镜像
./run.sh dk_up                      # docker compose up -d --build
./run.sh dk_down / dk_down_v        # 停止 / 停止并清数据
./run.sh dk_ps / dk_logs server     # 状态 / 日志

# 冒烟（需后端已启动）
./run.sh smoke_health               # 健康检查
./run.sh smoke_login admin          # 登录并打印 token（user|agent|admin）
./run.sh smoke_chat "你好"          # 用户对话冒烟

./run.sh help                       # 完整命令列表
```

> 说明：`smoke_chat` 使用预置账号 `user001/user123`；`smoke_login` 账号分别为 `user001/user123`、`agent001/agent123`、`admin/admin123`。

### 不使用 Docker 的本地开发

```bash
# 后端（需要 Java 17 + Maven）
cd ai-ticket-server
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 前端（需要 Node.js 20+）
cd ai-ticket-user-web && npm install && npm run dev    # 用户端 :3003
cd ai-ticket-agent-web && npm install && npm run dev   # 坐席端 :3002
cd ai-ticket-admin-web && npm install && npm run dev   # 管理端 :3001
```

## AI 功能配置（后端）

后端 AI 功能基于 **Spring AI 1.0.0**，通过 `application.yml` 的 `app.ai` 配置驱动，支持 **DeepSeek / DashScope / Mock** 三种提供商；对话模型与向量模型可独立指定。

### 配置项总览

| 配置 | 位置 | 说明 | 默认值 |
|------|------|------|--------|
| `app.ai.provider` | application.yml | 对话模型提供商（mock/deepseek/dashscope） | `mock` |
| `app.ai.embedding-provider` | application.yml | 向量模型提供商（mock/dashscope） | `mock` |
| `app.ai.system-prompt` | application.yml | AI 系统提示词 | 智能客服默认话术 |
| `app.ai.stream-timeout-seconds` | application.yml | SSE 流式超时（秒） | `120` |
| `app.ai.deepseek.api-key` | yml / `DEEPSEEK_API_KEY` | DeepSeek API Key | 空 |
| `app.ai.deepseek.base-url` | yml / `DEEPSEEK_BASE_URL` | DeepSeek 地址（**只写 host**） | `https://api.deepseek.com` |
| `app.ai.deepseek.completions-path` | application.yml | 对话接口路径 | `/v1/chat/completions` |
| `app.ai.deepseek.chat-model` | application.yml | DeepSeek 对话模型 | `deepseek-chat` |
| `app.ai.deepseek.temperature` | application.yml | 采样温度 | `0.7` |
| `app.ai.dashscope.api-key` | yml / `DASHSCOPE_API_KEY` | 阿里云百炼 API Key | 空 |
| `app.ai.dashscope.base-url` | yml / `DASHSCOPE_BASE_URL` | 百炼 API 地址（**只写 host，勿带 /api/v1**） | `https://dashscope.aliyuncs.com` |
| `app.ai.dashscope.chat-model` | application.yml | 百炼对话模型 | `qwen-plus` |
| `app.ai.dashscope.embed-model` | application.yml | 百炼向量模型 | `text-embedding-v2` |
| `app.faq.similarity-threshold` | application.yml | FAQ 语义匹配阈值 | `0.72` |
| `app.faq.semantic-search-enabled` | application.yml | 是否启用语义检索 | `true` |

### Provider 选择逻辑

对话模型（`app.ai.provider`）：

| 配置值 | 条件 | 实际模型 | 说明 |
|--------|------|----------|------|
| `deepseek` | Key 非空 | `OpenAiChatModel` | OpenAI 兼容，`base-url`+`completions-path` 拼接请求地址 |
| `dashscope` | Key 非空 | `DashScopeChatModel` | 默认模型 `qwen-plus` |
| `mock` 或缺 Key | — | `MockChatModel` | 模板回复，无 Key 也能跑通全流程 |

向量模型（`app.ai.embedding-provider`）：

| 配置值 | 条件 | 实际模型 | 说明 |
|--------|------|----------|------|
| `dashscope` | Key 非空 | `DashScopeEmbeddingModel` | 默认模型 `text-embedding-v2` |
| 其他 / 缺 Key | — | `MockEmbeddingModel` | 64 维确定性伪向量（基于文本哈希） |

### 推荐组合

> **DeepSeek 不提供 embedding API**，对话用 DeepSeek、向量用 DashScope 是推荐组合：
>
> ```yaml
> app:
>   ai:
>     provider: deepseek
>     embedding-provider: dashscope
> ```
> 需要同时配置 `DEEPSEEK_API_KEY` 与 `DASHSCOPE_API_KEY`；只配一个也能启动（缺的那个自动回退 Mock）。

### 配置方式

**方式一：环境变量（推荐，Docker / 生产）**

```bash
export DEEPSEEK_API_KEY=sk-xxx
export DEEPSEEK_BASE_URL=https://api.deepseek.com      # 只写 host
export DASHSCOPE_API_KEY=sk-xxx
export DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com
docker compose up -d --build
```

**方式二：直接修改 `ai-ticket-server/src/main/resources/application.yml`（本地开发）**

```yaml
app:
  ai:
    provider: dashscope            # mock | deepseek | dashscope
    embedding-provider: dashscope  # mock | dashscope
    deepseek:
      api-key: sk-xxx
      base-url: https://api.deepseek.com   # 只写 host，勿带 /v1（否则会拼成 /v1/v1/...）
      completions-path: /v1/chat/completions
      chat-model: deepseek-chat
    dashscope:
      api-key: sk-xxx
      base-url: https://dashscope.aliyuncs.com   # 只写 host，勿带 /api/v1（库内自动拼接 /api/v1/services/...）
      chat-model: qwen-plus
      embed-model: text-embedding-v2
```

### FAQ 语义检索

- `app.faq.similarity-threshold`：语义匹配分数达到该值才算命中（默认 0.72）。
- `app.faq.semantic-search-enabled: false` 可关闭语义检索，仅保留关键词 / 模糊匹配。
- 匹配链路逐级降级：**语义 → 关键词 → 模糊 → 交由大模型**。

### ⚠️ 切换向量模型后需重建向量

不同向量模型**维度不同**（Mock=64 维，DashScope text-embedding 为千维）。切换 `embedding-provider` 后，旧的 FAQ 向量因维度不匹配会被跳过，需重建：

```bash
# 管理端 → FAQ → 相似度配置 → 重建向量
# 或直接调用接口（需 admin token）：
curl -X POST http://localhost:8080/api/admin/faq/vectors/rebuild \
  -H "Authorization: <admin-token>"
```

`rebuildVectors` 会自动探测当前模型维度，并重建所有**空向量 / 维度不匹配**的条目，无需人工干预。

### 其他说明

- **无 Key 不崩**：任一 provider 缺 Key 都会回退 Mock，对话 / FAQ / 统计全流程仍可运行。
- **扩展新提供商**：加一个 Maven 依赖 + `AiConfig` 一个分支 + 一段配置即可。
- **排查**：`/api/chat/messages` 返回 Mock 模板话术，说明未配置 Key 或 Key 无效，检查对应环境变量；管理端 admin 接口若返回 403，请确认登录的是管理员账号（角色来自登录 ID 前缀）。

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 + TypeScript | ^3.5 / ^5.7 |
| UI 组件库 | Element Plus | ^2.9 |
| 状态管理 | Pinia | ^2.3 |
| 路由 | Vue Router | ^4.5 |
| HTTP 客户端 | Axios | ^1.7 |
| 图表库 | ECharts | ^6.1 |
| 后端框架 | Spring Boot | 3.3.6 |
| ORM | MyBatis-Plus | 3.5.9 |
| 权限框架 | Sa-Token | 1.39.0 |
| API 文档 | SpringDoc OpenAPI | 2.6.0 |
| 数据库 | MySQL | 8.0 |
| AI 服务 | DeepSeek API | deepseek-chat / text-embedding-v2 |
| 部署 | Docker + Nginx | — |

## 如何修改启动配置？

### Docker Compose

修改 `docker-compose.yaml` 中的 `environment` 或 `ports` 配置。

### 前端

前端 Vite 开发服务器配置位于各项目的 `vite.config.ts`，可调整：
- `server.port` — 开发服务器端口
- `server.proxy` — API 代理目标

### 后端

Spring Boot 配置位于 `ai-ticket-server/src/main/resources/application.yml`，可通过环境变量覆盖：
- `SERVER_PORT` — 服务端口（默认 8080）
- `SPRING_DATASOURCE_URL` — 数据库连接
- `APP_AI_API_KEY` — AI API 密钥

## 预置账号

| 平台 | 账号 | 密码 |
|------|------|------|
| user-web | user001 | user123 |
| agent-web | agent001 | agent123 |
| admin-web | admin | admin123 |

## 参考文档

- [项目需求介绍](ref/project_intro.md)
- [缺失功能清单](ref/todo_lack.md)
- [待完善功能清单](ref/todo_fill.md)
- [工程设计文档](ref/design.md)

## 遇到问题？

为什么不先问问 AI 怎么看呢？
