package com.aics.ticket.agent;

import com.aics.ticket.agent.mapper.AgentGroupMapper;
import com.aics.ticket.agent.mapper.SysAgentMapper;
import com.aics.ticket.auth.CurrentIdentity;
import com.aics.ticket.common.ApiResponse;
import com.aics.ticket.common.PageResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
public class AgentController {

    private final AgentGroupMapper groupMapper;
    private final SysAgentMapper agentMapper;

    public AgentController(AgentGroupMapper groupMapper, SysAgentMapper agentMapper) {
        this.groupMapper = groupMapper;
        this.agentMapper = agentMapper;
    }

    // ========== 坐席组 ==========

    @GetMapping("/groups")
    public ApiResponse<List<AgentGroup>> groups() {
        return ApiResponse.ok(groupMapper.selectList(new QueryWrapper<AgentGroup>().orderByAsc("id")));
    }

    // ========== 坐席上下线 ==========

    @PostMapping("/online")
    public ApiResponse<Map<String, Object>> goOnline() {
        Long agentId = CurrentIdentity.currentIdOrDefault("AGENT", 1L);
        SysAgent agent = agentMapper.selectById(agentId);
        if (agent != null) {
            agent.onlineStatus = "ONLINE";
            agentMapper.updateById(agent);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("online", true);
        data.put("onlineStatus", "ONLINE");
        return ApiResponse.ok(data);
    }

    @PostMapping("/offline")
    public ApiResponse<Map<String, Object>> goOffline() {
        Long agentId = CurrentIdentity.currentIdOrDefault("AGENT", 1L);
        SysAgent agent = agentMapper.selectById(agentId);
        if (agent != null) {
            agent.onlineStatus = "OFFLINE";
            agentMapper.updateById(agent);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("online", false);
        data.put("onlineStatus", "OFFLINE");
        return ApiResponse.ok(data);
    }

    @GetMapping("/me")
    public ApiResponse<SysAgent> me() {
        Long agentId = CurrentIdentity.currentIdOrDefault("AGENT", 1L);
        return ApiResponse.ok(agentMapper.selectById(agentId));
    }

    // ========== 管理端：坐席管理 ==========

    @GetMapping("/admin/agents")
    public ApiResponse<PageResponse<SysAgent>> listAgents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String status) {
        QueryWrapper<SysAgent> wrapper = new QueryWrapper<SysAgent>().orderByDesc("created_at");
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("username", keyword).or().like("real_name", keyword));
        }
        if (groupId != null) wrapper.eq("group_id", groupId);
        if (status != null && !status.isEmpty()) wrapper.eq("status", status);
        Page<SysAgent> p = agentMapper.selectPage(new Page<>(page, size), wrapper);
        return ApiResponse.ok(new PageResponse<>(p.getRecords(), p.getTotal(), page, size));
    }

    @PostMapping("/admin/agents")
    public ApiResponse<SysAgent> createAgent(@RequestBody Map<String, Object> request) {
        SysAgent agent = new SysAgent();
        agent.username = (String) request.get("username");
        agent.passwordHash = (String) request.getOrDefault("passwordHash", "123456");
        agent.realName = (String) request.get("realName");
        agent.groupId = request.get("groupId") != null ? Long.valueOf(request.get("groupId").toString()) : null;
        agent.role = (String) request.getOrDefault("role", "AGENT");
        agent.status = (String) request.getOrDefault("status", "ACTIVE");
        agent.onlineStatus = "OFFLINE";
        agentMapper.insert(agent);
        return ApiResponse.ok(agent);
    }

    @PutMapping("/admin/agents/{id}")
    public ApiResponse<SysAgent> updateAgent(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        SysAgent agent = agentMapper.selectById(id);
        if (agent == null) return ApiResponse.fail(404, "坐席不存在");
        if (request.containsKey("realName")) agent.realName = (String) request.get("realName");
        if (request.containsKey("groupId")) agent.groupId = request.get("groupId") != null ? Long.valueOf(request.get("groupId").toString()) : null;
        if (request.containsKey("role")) agent.role = (String) request.get("role");
        if (request.containsKey("status")) agent.status = (String) request.get("status");
        if (request.containsKey("passwordHash") && request.get("passwordHash") != null
                && !((String) request.get("passwordHash")).isEmpty()) {
            agent.passwordHash = (String) request.get("passwordHash");
        }
        agentMapper.updateById(agent);
        return ApiResponse.ok(agentMapper.selectById(id));
    }

    // ========== 管理端：坐席组管理 ==========

    @GetMapping("/admin/groups")
    public ApiResponse<List<AgentGroup>> listGroups() {
        return ApiResponse.ok(groupMapper.selectList(new QueryWrapper<AgentGroup>().orderByAsc("id")));
    }

    @PostMapping("/admin/groups")
    public ApiResponse<AgentGroup> createGroup(@RequestBody Map<String, Object> request) {
        AgentGroup group = new AgentGroup();
        group.name = (String) request.get("name");
        group.scene = (String) request.get("scene");
        group.leaderAgentId = request.get("leaderAgentId") != null ? Long.valueOf(request.get("leaderAgentId").toString()) : null;
        groupMapper.insert(group);
        return ApiResponse.ok(group);
    }

    @PutMapping("/admin/groups/{id}")
    public ApiResponse<AgentGroup> updateGroup(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        AgentGroup group = groupMapper.selectById(id);
        if (group == null) return ApiResponse.fail(404, "分组不存在");
        if (request.containsKey("name")) group.name = (String) request.get("name");
        if (request.containsKey("scene")) group.scene = (String) request.get("scene");
        if (request.containsKey("leaderAgentId")) {
            group.leaderAgentId = request.get("leaderAgentId") != null ? Long.valueOf(request.get("leaderAgentId").toString()) : null;
        }
        groupMapper.updateById(group);
        return ApiResponse.ok(groupMapper.selectById(id));
    }
}
