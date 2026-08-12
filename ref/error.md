jakarta.servlet.ServletException: Request processing failed: cn.dev33.satoken.exception.NotLoginException: 未能读取到有效 token
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1022) ~[spring-webmvc-6.1.15.jar:6.1.15]
	at org.springframework.web.servlet.FrameworkServlet.doOptions(FrameworkServlet.java:950) ~[spring-webmvc-6.1.15.jar:6.1.15]
	at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:599) ~[tomcat-embed-core-10.1.33.jar:6.0]
	at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:885) ~[spring-webmvc-6.1.15.jar:6.1.15]
	at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:658) ~[tomcat-embed-core-10.1.33.jar:6.0]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:195) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-6.1.15.jar:6.1.15]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.1.15.jar:6.1.15]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:101) ~[spring-web-6.1.15.jar:6.1.15]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:101) ~[spring-web-6.1.15.jar:6.1.15]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.core.ApplicationDispatcher.invoke(ApplicationDispatcher.java:632) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.core.ApplicationDispatcher.processRequest(ApplicationDispatcher.java:410) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.core.ApplicationDispatcher.doForward(ApplicationDispatcher.java:330) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.core.ApplicationDispatcher.forward(ApplicationDispatcher.java:267) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.core.StandardHostValve.custom(StandardHostValve.java:362) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.core.StandardHostValve.status(StandardHostValve.java:222) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.core.StandardHostValve.throwable(StandardHostValve.java:308) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:149) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:93) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:344) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:397) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:905) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1741) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:52) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1190) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:63) ~[tomcat-embed-core-10.1.33.jar:10.1.33]
	at java.base/java.lang.Thread.run(Thread.java:840) ~[na:na]
Caused by: cn.dev33.satoken.exception.NotLoginException: 未能读取到有效 token
	at cn.dev33.satoken.exception.NotLoginException.newInstance(NotLoginException.java:134) ~[sa-token-core-1.39.0.jar:na]
	at cn.dev33.satoken.stp.StpLogic.getLoginId(StpLogic.java:955) ~[sa-token-core-1.39.0.jar:na]
	at cn.dev33.satoken.stp.StpLogic.checkLogin(StpLogic.java:937) ~[sa-token-core-1.39.0.jar:na]
	at cn.dev33.satoken.stp.StpUtil.checkLogin(StpUtil.java:341) ~[sa-token-core-1.39.0.jar:na]
	at com.aics.ticket.config.SaTokenConfig.lambda$0(SaTokenConfig.java:24) ~[classes/:na]
	at cn.dev33.satoken.interceptor.SaInterceptor.preHandle(SaInterceptor.java:102) ~[sa-token-spring-boot3-starter-1.39.0.jar:na]
	at org.springframework.web.servlet.HandlerExecutionChain.applyPreHandle(HandlerExecutionChain.java:146) ~[spring-webmvc-6.1.15.jar:6.1.15]
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1084) ~[spring-webmvc-6.1.15.jar:6.1.15]
	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:979) ~[spring-webmvc-6.1.15.jar:6.1.15]
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1014) ~[spring-webmvc-6.1.15.jar:6.1.15]
	... 36 common frames omitted

---

## 处理记录

- **日期**: 2026-08-10
- **异常**: `NotLoginException: 未能读取到有效 token`（`cn.dev33.satoken.exception`）
- **触发位置**: `SaTokenConfig.lambda$0(SaTokenConfig.java:24)`，经 `FrameworkServlet.doOptions` 进入
- **根因**: 该请求是 **HTTP OPTIONS（CORS 预检请求）**。浏览器跨域访问后端时先发不带 Authorization header 的 OPTIONS preflight，被 Sa-Token 登录拦截器 `StpUtil.checkLogin()` 拦截。
  - `CorsConfig` 用 `addCorsMappings` 只注册了 CORS 元数据，**不会跳过拦截器**；`HandlerExecutionChain` 中 `PreFlightHandler` 仍会经过 `SaInterceptor`。
  - 拦截器抛出的异常无法被 `@RestControllerAdvice` 捕获，冒泡到 Tomcat 错误页。
- **修复**: 在 `SaTokenConfig` 的两个 `SaInterceptor` lambda 开头放行 OPTIONS 请求（`SaHolder.getRequest().getMethod()` 判断），登录校验和 ADMIN 角色校验均跳过预检。
- **状态**: ✅ 已修复并实测验证。
- **实测记录**（`mvn spring-boot:run -Dspring-boot.run.profiles=dev`，端口 8090）:
  | 场景 | 结果 |
  |------|------|
  | `OPTIONS /api/user/tickets`（带 Origin + Access-Control-Request-* 头） | ✅ `200`，返回完整 `Access-Control-Allow-Origin/Methods/Headers/Credentials` 头 |
  | `GET /api/user/tickets` 无 token | ✅ `401` + `{"code":401,"message":"请先登录","data":null}`（统一 JSON） |
  | `POST /api/user/auth/login` 正确账号 | ✅ 返回 token，随后带 token 访问工单列表成功 |

- **关于 401 统一 JSON**: 经反编译 `DispatcherServlet.doDispatch` 字节码确认，`applyPreHandle`（拦截器）抛出的 `NotLoginException` 会进入 `processDispatchResult` → `processHandlerException` → 被 `GlobalExceptionHandler.handleNotLoginException` 捕获并返回统一 JSON。**无需额外拦截器异常处理**——此前的 500 HTML 仅为 OPTIONS 预检被拦截的副产物，放行 OPTIONS 后即消失。
- **注意**: 若本地 8080 仍运行修复前的旧实例（OPTIONS 仍 500），需重启该实例加载新代码。
