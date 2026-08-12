package com.aics.ticket.auth;

import com.aics.ticket.common.ApiResponse;
import com.aics.ticket.common.enums.IdentityType;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── 登录 ────────────────────────────────────────────────────────────

    @PostMapping("/user/auth/login")
    public ApiResponse<LoginResponse> userLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request, IdentityType.USER));
    }

    @PostMapping("/agent/auth/login")
    public ApiResponse<LoginResponse> agentLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request, IdentityType.AGENT));
    }

    @PostMapping("/admin/auth/login")
    public ApiResponse<LoginResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request, IdentityType.ADMIN));
    }

    // ── 登出 ────────────────────────────────────────────────────────────

    @PostMapping("/auth/logout")
    public ApiResponse<Map<String, Boolean>> logout() {
        authService.logout();
        return ApiResponse.ok(Map.of("success", true));
    }

    // ── 注册 ────────────────────────────────────────────────────────────

    /**
     * 用户自助注册（公开接口）
     */
    @PostMapping("/user/auth/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    // ── 密码重置 ────────────────────────────────────────────────────────

    /**
     * 用户自助重置密码（公开接口，需提供旧密码验证）
     */
    @PostMapping("/user/auth/reset-password")
    public ApiResponse<Map<String, Boolean>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok(Map.of("success", true));
    }

    // ── 强制下线（管理员操作） ───────────────────────────────────────────

    /**
     * 管理员强制指定会话下线
     * @param identityType USER / AGENT / ADMIN
     * @param id          对应表的主键 ID
     */
    @PostMapping("/admin/auth/force-logout/{identityType}/{id}")
    public ApiResponse<Map<String, Boolean>> forceLogout(
            @PathVariable String identityType,
            @PathVariable Long id) {
        IdentityType type;
        try {
            type = IdentityType.valueOf(identityType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(400, "无效的身份类型: " + identityType);
        }
        authService.forceLogout(type, id);
        return ApiResponse.ok(Map.of("success", true));
    }
}
