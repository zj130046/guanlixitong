# =============================================================================
# 后端 Spring Boot Dockerfile
# 多阶段构建：Maven 编译 → JRE 运行
# 注意：pom.xml 的 <java.version> 为 17，镜像版本需与之对齐
# =============================================================================

# ── Stage 1: 编译 ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
# 缓存依赖（仅 pom.xml 变化时该层才失效，加速反复构建）
# 必须显式 -Pprod：dev profile 是 activeByDefault，不激活 prod 则 jar 缺 MySQL 驱动
RUN mvn -B -Pprod dependency:go-offline || true
COPY src/ src/
RUN mvn -B -Pprod package -DskipTests

# ── Stage 2: 运行 ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
# 非 root 运行；uploads 目录需可写（compose 挂载 upload_data:/app/uploads）
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R app:app /app
USER app

EXPOSE 8080

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
