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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysAgentMapper agentMapper;
    private final SysAdminMapper adminMapper;

    public AuthService(SysUserMapper userMapper, SysAgentMapper agentMapper, SysAdminMapper adminMapper) {
        this.userMapper = userMapper;
        this.agentMapper = agentMapper;
        this.adminMapper = adminMapper;
    }

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

    private LoginResponse loginUser(LoginRequest request) {
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", request.getUsername()).last("LIMIT 1"));
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.status) || !matchesPassword(request.getPassword(), user.passwordHash)) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        StpUtil.login("USER:" + user.id);
        return new LoginResponse(StpUtil.getTokenValue(), user.username, IdentityType.USER, user.id, user.username);
    }

    private LoginResponse loginAgent(LoginRequest request) {
        SysAgent agent = agentMapper.selectOne(new QueryWrapper<SysAgent>().eq("username", request.getUsername()).last("LIMIT 1"));
        if (agent == null || !"ACTIVE".equalsIgnoreCase(agent.status) || !matchesPassword(request.getPassword(), agent.passwordHash)) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        StpUtil.login("AGENT:" + agent.id);
        return new LoginResponse(StpUtil.getTokenValue(), agent.username, IdentityType.AGENT, agent.id,
                StringUtils.hasText(agent.realName) ? agent.realName : agent.username);
    }

    private LoginResponse loginAdmin(LoginRequest request) {
        SysAdmin admin = adminMapper.selectOne(new QueryWrapper<SysAdmin>().eq("username", request.getUsername()).last("LIMIT 1"));
        if (admin == null || !"ACTIVE".equalsIgnoreCase(admin.status) || !matchesPassword(request.getPassword(), admin.passwordHash)) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        StpUtil.login("ADMIN:" + admin.id);
        return new LoginResponse(StpUtil.getTokenValue(), admin.username, IdentityType.ADMIN, admin.id,
                StringUtils.hasText(admin.realName) ? admin.realName : admin.username);
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        return storedPassword != null && (storedPassword.equals(rawPassword) || storedPassword.equals("{noop}" + rawPassword));
    }
}
