package com.aics.ticket.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 全局认证拦截器配置
 * <p>
 * 拦截所有 /api/** 请求，自动校验登录态。
 * 公开路径（登录、注册、健康检查等）在 excludePathPatterns 中排除。
 * 管理员专有接口通过路由前缀 /admin/** 校验 ADMIN 角色。
 * </p>
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 放行 CORS 预检请求（OPTIONS 请求不携带 token，由浏览器在 preflight 阶段发起）
                    if ("OPTIONS".equalsIgnoreCase(SaHolder.getRequest().getMethod())) {
                        return;
                    }
                    // 校验当前请求是否已登录（公开路径已排除）
                    StpUtil.checkLogin();
                }))
                .addPathPatterns("/**")
                // ── 公开路径（无需登录） ──
                .excludePathPatterns(
                        "/health",
                        "/user/auth/login",
                        "/user/auth/register",
                        "/user/auth/reset-password",
                        "/agent/auth/login",
                        "/admin/auth/login",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/files/download/**"
                )
                // ── 管理端角色校验 ──
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/auth/login");  // admin 登录本身豁免

        // 注意：admin 接口的角色鉴权在第二层：
        // 所有 /admin/** 请求需满足 StpUtil.checkRole("ADMIN")
        // 但 /agent/admin/** 路径是 admin 操作 agent 的接口，同样需要 ADMIN 角色
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 放行 CORS 预检请求（OPTIONS 请求不携带 token）
                    if ("OPTIONS".equalsIgnoreCase(SaHolder.getRequest().getMethod())) {
                        return;
                    }
                    StpUtil.checkRoleOr("ADMIN");
                }))
                .addPathPatterns("/admin/**", "/agent/admin/**")
                .excludePathPatterns("/admin/auth/login");
    }
}
