package com.aics.ticket.auth;

import com.aics.ticket.common.ApiResponse;
import com.aics.ticket.common.enums.IdentityType;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

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

    @PostMapping("/auth/logout")
    public ApiResponse<Map<String, Boolean>> logout() {
        authService.logout();
        return ApiResponse.ok(Map.of("success", true));
    }
}
