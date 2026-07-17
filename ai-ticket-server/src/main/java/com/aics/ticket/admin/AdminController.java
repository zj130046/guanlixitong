package com.aics.ticket.admin;

import com.aics.ticket.admin.mapper.SysAdminMapper;
import com.aics.ticket.agent.SysAgent;
import com.aics.ticket.agent.mapper.SysAgentMapper;
import com.aics.ticket.common.ApiResponse;
import com.aics.ticket.common.BusinessException;
import com.aics.ticket.user.SysUser;
import com.aics.ticket.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final SysUserMapper userMapper;
    private final SysAgentMapper agentMapper;
    private final SysAdminMapper adminMapper;

    public AdminController(SysUserMapper userMapper, SysAgentMapper agentMapper, SysAdminMapper adminMapper) {
        this.userMapper = userMapper;
        this.agentMapper = agentMapper;
        this.adminMapper = adminMapper;
    }

    @GetMapping("/menus")
    public ApiResponse<List<Map<String, String>>> menus() {
        return ApiResponse.ok(List.of(
                Map.of("name", "首页看板", "path", "/dashboard"),
                Map.of("name", "工单管理", "path", "/tickets"),
                Map.of("name", "FAQ 知识库", "path", "/faq/entries"),
                Map.of("name", "统计报表", "path", "/reports/monthly")
        ));
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> users() {
        return ApiResponse.ok(userMapper.selectList(new QueryWrapper<SysUser>().orderByDesc("created_at"))
                .stream().map(this::userView).toList());
    }

    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(@RequestBody Map<String, Object> request) {
        SysUser user = new SysUser();
        applyUser(user, request, true);
        userMapper.insert(user);
        return ApiResponse.ok(userView(user));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        applyUser(user, request, false);
        userMapper.updateById(user);
        return ApiResponse.ok(userView(userMapper.selectById(id)));
    }

    @PostMapping("/users/{id}/status")
    public ApiResponse<Map<String, Object>> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.status = String.valueOf(request.getOrDefault("status", "ACTIVE"));
        userMapper.updateById(user);
        return ApiResponse.ok(userView(userMapper.selectById(id)));
    }

    @GetMapping("/agents")
    public ApiResponse<List<Map<String, Object>>> agents() {
        return ApiResponse.ok(agentMapper.selectList(new QueryWrapper<SysAgent>().orderByDesc("created_at"))
                .stream().map(this::agentView).toList());
    }

    @PostMapping("/agents")
    public ApiResponse<Map<String, Object>> createAgent(@RequestBody Map<String, Object> request) {
        SysAgent agent = new SysAgent();
        applyAgent(agent, request, true);
        agentMapper.insert(agent);
        return ApiResponse.ok(agentView(agent));
    }

    @PutMapping("/agents/{id}")
    public ApiResponse<Map<String, Object>> updateAgent(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        SysAgent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new BusinessException(404, "客服不存在");
        }
        applyAgent(agent, request, false);
        agentMapper.updateById(agent);
        return ApiResponse.ok(agentView(agentMapper.selectById(id)));
    }

    @PostMapping("/agents/{id}/status")
    public ApiResponse<Map<String, Object>> updateAgentStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        SysAgent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new BusinessException(404, "客服不存在");
        }
        agent.status = String.valueOf(request.getOrDefault("status", "ACTIVE"));
        agentMapper.updateById(agent);
        return ApiResponse.ok(agentView(agentMapper.selectById(id)));
    }

    @GetMapping("/admins")
    public ApiResponse<List<Map<String, Object>>> admins() {
        return ApiResponse.ok(adminMapper.selectList(new QueryWrapper<SysAdmin>().orderByDesc("created_at"))
                .stream().map(admin -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", admin.id);
                    row.put("username", admin.username);
                    row.put("realName", admin.realName);
                    row.put("status", admin.status);
                    row.put("createdAt", admin.createdAt);
                    return row;
                }).toList());
    }

    private void applyUser(SysUser user, Map<String, Object> request, boolean creating) {
        if (creating || request.containsKey("username")) {
            user.username = required(request, "username");
        }
        if (creating || request.containsKey("password")) {
            user.passwordHash = stringOrDefault(request.get("password"), creating ? "demo123456" : user.passwordHash);
        }
        if (request.containsKey("phone")) {
            user.phone = stringValue(request.get("phone"));
        }
        if (request.containsKey("email")) {
            user.email = stringValue(request.get("email"));
        }
        if (request.containsKey("status")) {
            user.status = stringOrDefault(request.get("status"), "ACTIVE");
        } else if (creating) {
            user.status = "ACTIVE";
        }
    }

    private void applyAgent(SysAgent agent, Map<String, Object> request, boolean creating) {
        if (creating || request.containsKey("username")) {
            agent.username = required(request, "username");
        }
        if (creating || request.containsKey("password")) {
            agent.passwordHash = stringOrDefault(request.get("password"), creating ? "agent123456" : agent.passwordHash);
        }
        if (request.containsKey("realName")) {
            agent.realName = stringValue(request.get("realName"));
        }
        if (request.containsKey("groupId") && request.get("groupId") != null) {
            agent.groupId = Long.valueOf(request.get("groupId").toString());
        }
        if (request.containsKey("role")) {
            agent.role = stringOrDefault(request.get("role"), "AGENT");
        } else if (creating) {
            agent.role = "AGENT";
        }
        if (request.containsKey("onlineStatus")) {
            agent.onlineStatus = stringOrDefault(request.get("onlineStatus"), "OFFLINE");
        } else if (creating) {
            agent.onlineStatus = "OFFLINE";
        }
        if (request.containsKey("status")) {
            agent.status = stringOrDefault(request.get("status"), "ACTIVE");
        } else if (creating) {
            agent.status = "ACTIVE";
        }
    }

    private Map<String, Object> userView(SysUser user) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", user.id);
        row.put("username", user.username);
        row.put("phone", user.phone);
        row.put("email", user.email);
        row.put("status", user.status);
        row.put("createdAt", user.createdAt);
        row.put("updatedAt", user.updatedAt);
        return row;
    }

    private Map<String, Object> agentView(SysAgent agent) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", agent.id);
        row.put("username", agent.username);
        row.put("realName", agent.realName);
        row.put("onlineStatus", agent.onlineStatus);
        row.put("groupId", agent.groupId);
        row.put("role", agent.role);
        row.put("status", agent.status);
        row.put("createdAt", agent.createdAt);
        row.put("updatedAt", agent.updatedAt);
        return row;
    }

    private String required(Map<String, Object> request, String key) {
        String value = stringValue(request.get(key));
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("缺少必填字段: " + key);
        }
        return value;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String stringOrDefault(Object value, String fallback) {
        String text = stringValue(value);
        return StringUtils.hasText(text) ? text : fallback;
    }
}
