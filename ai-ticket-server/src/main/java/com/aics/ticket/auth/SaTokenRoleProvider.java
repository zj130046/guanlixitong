package com.aics.ticket.auth;

import cn.dev33.satoken.stp.StpInterface;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 角色来源实现
 * <p>
 * 登录 ID 采用 "类型:ID" 前缀（如 {@code ADMIN:1} / {@code AGENT:2} / {@code USER:3}），
 * 此处解析前缀并映射为 Sa-Token 角色，供 {@code StpUtil.checkRole/checkRoleOr} 使用。
 * 缺失该类时，/admin/** 的 ADMIN 角色校验恒为 false（全部 403）。
 * </p>
 */
@Component
public class SaTokenRoleProvider implements StpInterface {

    private static final Set<String> KNOWN_PREFIXES = Set.of("USER", "AGENT", "ADMIN");

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) return List.of();
        String text = loginId.toString();
        int colon = text.indexOf(':');
        if (colon <= 0) return List.of();
        String prefix = text.substring(0, colon).toUpperCase();
        if (!KNOWN_PREFIXES.contains(prefix)) return List.of();
        return List.of(prefix);
    }
}
