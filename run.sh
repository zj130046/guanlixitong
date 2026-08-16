#!/usr/bin/env bash
# =============================================================================
# AI 智能客服工单处理系统 — 常用构建/运行/部署命令（可复用函数）
#
# 用法:
#   ./run.sh <命令> [参数...]        # 直接调用
#   source run.sh                    # 或 source 后直接调用内部函数
#
# 依赖命令: mvn(Java 17) / npm(node) / docker / curl
#
# 常用:
#   ./run.sh be_run                  # 后端 dev/H2 :8080
#   ./run.sh be_run 8090             # 后端指定端口
#   ./run.sh fe_dev user             # 前端 dev（user|agent|admin|all）
#   ./run.sh dk_up                   # docker compose up -d --build
#   ./run.sh smoke_health            # 健康检查
#   ./run.sh help                    # 完整命令列表
# =============================================================================
set -euo pipefail

# ── 路径 ────────────────────────────────────────────────────────────────────
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="${ROOT}/ai-ticket-server"

# ── 颜色输出 ────────────────────────────────────────────────────────────────
C_RED=$'\033[31m'; C_GREEN=$'\033[32m'; C_YELLOW=$'\033[33m'; C_CYAN=$'\033[36m'; C_OFF=$'\033[0m'
info() { printf "${C_CYAN}[run.sh]${C_OFF} %s\n" "$*"; }
ok()   { printf "${C_GREEN}[run.sh]${C_OFF} %s\n" "$*"; }
warn() { printf "${C_YELLOW}[run.sh]${C_OFF} %s\n" "$*"; }
err()  { printf "${C_RED}[run.sh]${C_OFF} %s\n" "$*" >&2; }

# 校验依赖命令存在
require() {
  for cmd in "$@"; do
    command -v "$cmd" >/dev/null 2>&1 || { err "缺少命令: $cmd（请先安装）"; return 1; }
  done
}

# 前端项目名 → 目录
web_dir() {
  case "$1" in
    user)  echo ai-ticket-user-web ;;
    agent) echo ai-ticket-agent-web ;;
    admin) echo ai-ticket-admin-web ;;
    *)     return 1 ;;
  esac
}

# =============================================================================
# 后端（Spring Boot 3.4 / Java 17，profile: dev=H2, prod=MySQL）
# =============================================================================

# 编译（dev profile）
be_compile() { require mvn && (cd "$SERVER_DIR" && mvn -q -Pdev compile) && ok "编译完成"; }

# 打包（-Pprod 带上 MySQL 驱动，与 Docker 镜像产物一致；跳过测试）
be_package() {
  require mvn && info "打包后端 (prod)..."
  (cd "$SERVER_DIR" && mvn -q -Pprod package -DskipTests)
  ok "产物: $SERVER_DIR/target/*.jar"
}

# 运行（dev/H2）；可选端口参数，默认 8080
be_run() {
  require mvn
  local port="${1:-8080}"
  info "启动后端 (dev/H2, port=${port})... Ctrl+C 停止"
  (cd "$SERVER_DIR" && mvn -Pdev spring-boot:run -Dspring-boot.run.arguments="--server.port=${port}")
}

# 停止占用指定端口的后端进程（默认 8080）
be_stop() {
  local port="${1:-8080}"
  if command -v fuser >/dev/null 2>&1; then
    fuser -k "${port}/tcp" 2>/dev/null || true
  elif command -v lsof >/dev/null 2>&1; then
    lsof -ti":${port}" | xargs -r kill 2>/dev/null || true
  fi
  pkill -f "spring-boot:run" 2>/dev/null || true
  ok "已清理 port=${port} 上的后端进程"
}

be_clean() { require mvn && (cd "$SERVER_DIR" && mvn -q clean) && ok "已 clean"; }

# =============================================================================
# 前端（Vue3 + Vite，三端共用）
# =============================================================================

# 三个前端统一安装依赖
fe_install() {
  require npm
  for d in ai-ticket-user-web ai-ticket-agent-web ai-ticket-admin-web; do
    info "安装依赖: ${d}"; (cd "${ROOT}/${d}" && npm install)
  done
  ok "依赖安装完成"
}

# 前端 dev server；参数 user|agent|admin|all（默认 all，后台启动）
fe_dev() {
  require npm
  local target="${1:-all}"
  if [ "$target" = "all" ]; then
    for d in ai-ticket-user-web ai-ticket-agent-web ai-ticket-admin-web; do
      info "后台启动 ${d} dev server"
      (cd "${ROOT}/${d}" && nohup npm run dev >/dev/null 2>&1 &)
    done
    ok "三个前端 dev server 已后台启动（admin:3001 / agent:3002 / user:3003）"
    return 0
  fi
  local dir; dir="$(web_dir "$target")" || { err "未知前端: $target（user|agent|admin|all）"; return 1; }
  info "启动 ${dir} dev server... Ctrl+C 停止"
  (cd "${ROOT}/${dir}" && npm run dev)
}

# 前端构建（含 vue-tsc 类型检查）；参数 user|agent|admin|all
fe_build() {
  require npm
  local target="${1:-all}"
  local build_one
  build_one() {
    local dir="$1"; info "构建 ${dir}..."
    (cd "${ROOT}/${dir}" && npm run build)
  }
  if [ "$target" = "all" ]; then
    for d in ai-ticket-user-web ai-ticket-agent-web ai-ticket-admin-web; do build_one "$d"; done
  else
    local dir; dir="$(web_dir "$target")" || { err "未知前端: $target"; return 1; }
    build_one "$dir"
  fi
  ok "前端构建完成"
}

# 前端构建（跳过 vue-tsc，与 Docker 内 npx vite build 一致，更快）
fe_build_fast() {
  require npx
  local target="${1:-all}"
  if [ "$target" = "all" ]; then
    for d in ai-ticket-user-web ai-ticket-agent-web ai-ticket-admin-web; do
      info "快速构建 ${d}"; (cd "${ROOT}/${d}" && npx vite build)
    done
  else
    local dir; dir="$(web_dir "$target")" || { err "未知前端: $target"; return 1; }
    (cd "${ROOT}/${dir}" && npx vite build)
  fi
  ok "前端快速构建完成"
}

# 停止前端 dev server
fe_stop() { pkill -f "vite" 2>/dev/null || true; ok "已清理前端 dev server"; }

# =============================================================================
# Docker Compose（mysql + server + nginx）
# =============================================================================

# 预拉后端构建/运行镜像（首次构建前可先拉取加速）
dk_images() {
  require docker
  docker pull maven:3.9-eclipse-temurin-17
  docker pull eclipse-temurin:17-jre-alpine
  ok "后端镜像已就绪"
}

dk_build()        { require docker && docker compose build; }
dk_build_server() { require docker && docker compose build server; }
dk_build_web()    { require docker && docker compose build nginx; }
dk_up()           { require docker && docker compose up -d --build; }
dk_up_only()      { require docker && docker compose up -d; }
dk_down()         { require docker && docker compose down; }
dk_down_v()       { require docker && docker compose down -v; }
dk_ps()           { require docker && docker compose ps; }
dk_logs()         { require docker && docker compose logs -f --tail=100 "${1:-server}"; }

# =============================================================================
# 冒烟测试（需后端已启动）
# =============================================================================

smoke_health() { curl -s "http://localhost:${1:-8080}/api/health"; echo; }

# 登录并打印 token；参数 user|agent|admin（默认 user），可选端口
smoke_login() {
  local type="${1:-user}" port="${2:-8080}" ep u p
  case "$type" in
    user)  ep=user;  u=user001; p=user123 ;;
    agent) ep=agent; u=agent001; p=agent123 ;;
    admin) ep=admin; u=admin;    p=admin123 ;;
    *) err "未知身份: $type（user|agent|admin）"; return 1 ;;
  esac
  curl -s -X POST "http://localhost:${port}/api/${ep}/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${u}\",\"password\":\"${p}\"}"
  echo
}

# 用户对话冒烟（登录 → /chat/messages）；参数 [端口] [消息]
smoke_chat() {
  local port="${1:-8080}" msg="${2:-你好}"
  local token
  token="$(curl -s -X POST "http://localhost:${port}/api/user/auth/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"user001","password":"user123"}' \
    | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
  [ -n "$token" ] || { err "登录失败，拿不到 token"; return 1; }
  curl -s -X POST "http://localhost:${port}/api/chat/messages" \
    -H "Authorization: ${token}" -H 'Content-Type: application/json' \
    -d "{\"message\":\"${msg}\"}"
  echo
}

# =============================================================================
# 文档（需求说明书）
# =============================================================================

# 需求说明书 md → docx（pandoc --toc 生成 Word 多级目录）；参数 [md] [out]，默认 ref/require_doc.md
ref_docx() {
  require pandoc || return 1
  local md="${1:-${ROOT}/ref/require_doc.md}"
  local out="${2:-${md%.md}.docx}"
  info "转换 ${md} → ${out}"
  pandoc "$md" -o "$out" --toc --toc-depth=3
  ok "生成: ${out}"
}

# =============================================================================
# 工具
# =============================================================================

# 查看端口占用
port_check() { ss -tlnp 2>/dev/null | grep -E ":$1\b" || echo "port $1 空闲"; }

# 帮助
help_me() {
  cat <<'EOF'
用法: ./run.sh <命令> [参数...]

后端:
  be_compile               编译 (dev profile)
  be_package               打包 (prod, 与 Docker 一致)
  be_run [端口]            运行 (dev/H2, 默认 8080)
  be_stop [端口]           停止后端进程
  be_clean                 mvn clean

前端:
  fe_install               三个前端安装依赖
  fe_dev [user|agent|admin|all]   dev server (默认 all, 后台)
  fe_build [user|agent|admin|all] 构建 (含 vue-tsc 类型检查)
  fe_build_fast [user|agent|admin|all] 构建 (跳过 vue-tsc, 同 Docker)
  fe_stop                  停止前端 dev server

Docker:
  dk_images                预拉后端镜像
  dk_build / dk_build_server / dk_build_web
  dk_up / dk_up_only / dk_down / dk_down_v
  dk_ps / dk_logs [服务]

冒烟 (需后端已启动):
  smoke_health [端口]
  smoke_login [user|agent|admin] [端口]
  smoke_chat [端口] [消息]

文档:
  ref_docx [md] [out]      需求说明书 md → docx (含多级目录, 需 pandoc)

工具:
  port_check <端口>        查看端口占用
  help                     显示本帮助
EOF
}

# ── 分发：仅直接执行时生效；source 本文件时不触发 ──────────────────────────
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
  case "${1:-}" in
    ""|-h|--help|help) help_me ;;
    *)
      if declare -F "$1" >/dev/null 2>&1; then
        "$@"
      else
        err "未知命令: $1（运行 ./run.sh 查看帮助）"; exit 1
      fi
      ;;
  esac
fi
