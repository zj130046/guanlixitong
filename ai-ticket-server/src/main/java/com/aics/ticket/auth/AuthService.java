package com.aics.ticket.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.aics.ticket.admin.SysAdmin;
import com.aics.ticket.admin.mapper.SysAdminMapper;
import com.aics.ticket.agent.SysAgent;
import com.aics.ticket.agent.mapper.SysAgentMapper;
import com.aics.ticket.common.BusinessException;
import com.aics.ticket.common.enums.IdentityType;
import com.aics.ticket.user.SysUser;
import com.aics.ticket.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证服务：登录、注册、密码重置、强制下线
 * <p>
 * 密码兼容策略：
 * <ul>
 *   <li>{@code {bcrypt}$2a$...}  — BCrypt 加密（推荐）</li>
 *   <li>{@code {noop}xxx}        — 明文（仅用于旧数据兼容，登录成功后自动升级为 {bcrypt}）</li>
 *   <li>无前缀                    — 视为 {noop}（兼容更早的种子数据）</li>
 * </ul>
 * </p>
 */
@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysAgentMapper agentMapper;
    private final SysAdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;

    private static final String BCRYPT_PREFIX = "{bcrypt}";
    private static final String NOOP_PREFIX = "{noop}";

    public AuthService(SysUserMapper userMapper, SysAgentMapper agentMapper, SysAdminMapper adminMapper,
                       PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.agentMapper = agentMapper;
        this.adminMapper = adminMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // ════════════════════════════════════════════════════════════════════
    // 登录 / 登出
    // ════════════════════════════════════════════════════════════════════

    public LoginResponse login(LoginRequest request, IdentityType identityType) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("请输入账号和密码");
        }
        return switch (identityType) {
            case USER -> loginUser(request);
            case AGENT -> loginAgent(request);
            case ADMIN -> loginAdmin(request);
        };
    }

    public void logout() {
        StpUtil.logout();
    }

    // ════════════════════════════════════════════════════════════════════
    // 注册
    // ════════════════════════════════════════════════════════════════════

    /**
     * 用户自助注册，密码以 {bcrypt} 格式存储
     */
    public LoginResponse register(RegisterRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("账号和密码不能为空");
        }
        // 检查用户名是否已被占用（用户表、坐席表、管理员表）
        if (userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", request.getUsername())) > 0) {
            throw new BusinessException("该账号已被注册");
        }
        if (agentMapper.selectCount(new QueryWrapper<SysAgent>().eq("username", request.getUsername())) > 0) {
            throw new BusinessException("该账号已被注册");
        }
        if (adminMapper.selectCount(new QueryWrapper<SysAdmin>().eq("username", request.getUsername())) > 0) {
            throw new BusinessException("该账号已被注册");
        }
        SysUser user = new SysUser();
        user.username = request.getUsername();
        user.passwordHash = BCRYPT_PREFIX + passwordEncoder.encode(request.getPassword());
        user.phone = request.getPhone();
        user.email = request.getEmail();
        user.status = "ACTIVE";
        userMapper.insert(user);

        // 注册后自动登录
        StpUtil.login("USER:" + user.id);
        return new LoginResponse(StpUtil.getTokenValue(), user.username, IdentityType.USER, user.id, user.username);
    }

    // ════════════════════════════════════════════════════════════════════
    // 密码重置
    // ════════════════════════════════════════════════════════════════════

    /**
     * 用户自助重置密码：验证旧密码后更新为新密码（BCrypt）
     */
    public void resetPassword(ResetPasswordRequest request) {
        if (!StringUtils.hasText(request.getUsername())
                || !StringUtils.hasText(request.getOldPassword())
                || !StringUtils.hasText(request.getNewPassword())) {
            throw new BusinessException("账号、旧密码和新密码均不能为空");
        }
        SysUser user = userMapper.selectOne(
                new QueryWrapper<SysUser>().eq("username", request.getUsername()).last("LIMIT 1"));
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.status)) {
            throw new BusinessException("账号不存在或已被禁用");
        }
        if (!verifyPassword(request.getOldPassword(), user.passwordHash)) {
            throw new BusinessException("旧密码错误");
        }
        user.passwordHash = BCRYPT_PREFIX + passwordEncoder.encode(request.getNewPassword());
        userMapper.updateById(user);
    }

    // ════════════════════════════════════════════════════════════════════
    // 强制下线（管理员操作）
    // ════════════════════════════════════════════════════════════════════

    /**
     * 管理员强制踢出指定身份的会话
     * @param identityType 身份类型
     * @param id           用户/坐席/管理员 ID
     */
    public void forceLogout(IdentityType identityType, Long id) {
        String loginId = identityType.name() + ":" + id;
        // 如果该用户当前在线，则踢下线
        if (StpUtil.isLogin(loginId)) {
            StpUtil.logout(loginId);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 私有方法
    // ════════════════════════════════════════════════════════════════════

    private LoginResponse loginUser(LoginRequest request) {
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", request.getUsername()).last("LIMIT 1"));
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.status)
                || !verifyAndUpgradePassword(request.getPassword(), user)) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        StpUtil.login("USER:" + user.id);
        return new LoginResponse(StpUtil.getTokenValue(), user.username, IdentityType.USER, user.id, user.username);
    }

    private LoginResponse loginAgent(LoginRequest request) {
        SysAgent agent = agentMapper.selectOne(new QueryWrapper<SysAgent>().eq("username", request.getUsername()).last("LIMIT 1"));
        if (agent == null || !"ACTIVE".equalsIgnoreCase(agent.status)
                || !verifyAndUpgradePassword(request.getPassword(), agent)) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        StpUtil.login("AGENT:" + agent.id);
        return new LoginResponse(StpUtil.getTokenValue(), agent.username, IdentityType.AGENT, agent.id,
                StringUtils.hasText(agent.realName) ? agent.realName : agent.username);
    }

    private LoginResponse loginAdmin(LoginRequest request) {
        SysAdmin admin = adminMapper.selectOne(new QueryWrapper<SysAdmin>().eq("username", request.getUsername()).last("LIMIT 1"));
        if (admin == null || !"ACTIVE".equalsIgnoreCase(admin.status)
                || !verifyAndUpgradePassword(request.getPassword(), admin)) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        StpUtil.login("ADMIN:" + admin.id);
        return new LoginResponse(StpUtil.getTokenValue(), admin.username, IdentityType.ADMIN, admin.id,
                StringUtils.hasText(admin.realName) ? admin.realName : admin.username);
    }

    // ════════════════════════════════════════════════════════════════════
    // 密码验证与自动升级
    // ════════════════════════════════════════════════════════════════════

    /**
     * 验证密码并通过后，如果旧密码是非 BCrypt 格式则自动升级
     *
     * @return true 验证通过，false 验证失败
     */
    private boolean verifyAndUpgradePassword(String rawPassword, SysUser user) {
        return verifyAndUpgradePassword(rawPassword, user, () -> userMapper.updateById(user));
    }

    private boolean verifyAndUpgradePassword(String rawPassword, SysAgent agent) {
        return verifyAndUpgradePassword(rawPassword, agent, () -> agentMapper.updateById(agent));
    }

    private boolean verifyAndUpgradePassword(String rawPassword, SysAdmin admin) {
        return verifyAndUpgradePassword(rawPassword, admin, () -> adminMapper.updateById(admin));
    }

    /**
     * 验证密码，若密码为 {noop} 或纯明文格式则自动升级为 {bcrypt}
     */
    private boolean verifyAndUpgradePassword(String rawPassword, Object entity, Runnable updater) {
        try {
            String storedHash = getPasswordHash(entity);
            if (!StringUtils.hasText(storedHash)) return false;

            if (storedHash.startsWith(BCRYPT_PREFIX)) {
                // BCrypt 验证
                String bcryptHash = storedHash.substring(BCRYPT_PREFIX.length());
                return passwordEncoder.matches(rawPassword, bcryptHash);
            }

            // {noop} 或纯明文验证
            String plainPart = storedHash.startsWith(NOOP_PREFIX)
                    ? storedHash.substring(NOOP_PREFIX.length())
                    : storedHash;
            boolean matched = rawPassword.equals(plainPart);

            if (matched) {
                // 自动升级为 BCrypt
                setPasswordHash(entity, BCRYPT_PREFIX + passwordEncoder.encode(rawPassword));
                updater.run();
            }
            return matched;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 仅验证密码（不升级），用于密码重置场景
     */
    private boolean verifyPassword(String rawPassword, String storedHash) {
        if (!StringUtils.hasText(storedHash)) return false;
        if (storedHash.startsWith(BCRYPT_PREFIX)) {
            return passwordEncoder.matches(rawPassword, storedHash.substring(BCRYPT_PREFIX.length()));
        }
        String plainPart = storedHash.startsWith(NOOP_PREFIX)
                ? storedHash.substring(NOOP_PREFIX.length())
                : storedHash;
        return rawPassword.equals(plainPart);
    }

    // ════════════════════════════════════════════════════════════════════
    // 反射辅助（避免为每个实体定义接口）
    // ════════════════════════════════════════════════════════════════════

    private String getPasswordHash(Object entity) {
        if (entity instanceof SysUser u) return u.passwordHash;
        if (entity instanceof SysAgent a) return a.passwordHash;
        if (entity instanceof SysAdmin a) return a.passwordHash;
        return null;
    }

    private void setPasswordHash(Object entity, String hash) {
        if (entity instanceof SysUser u) u.passwordHash = hash;
        else if (entity instanceof SysAgent a) a.passwordHash = hash;
        else if (entity instanceof SysAdmin a) a.passwordHash = hash;
    }
}
