package com.aics.ticket.ticket;

import com.aics.ticket.agent.AgentGroup;
import com.aics.ticket.agent.SysAgent;
import com.aics.ticket.agent.mapper.AgentGroupMapper;
import com.aics.ticket.agent.mapper.SysAgentMapper;
import com.aics.ticket.auth.CurrentIdentity;
import com.aics.ticket.common.BusinessException;
import com.aics.ticket.common.PageResponse;
import com.aics.ticket.common.enums.TicketPriority;
import com.aics.ticket.common.enums.TicketSource;
import com.aics.ticket.common.enums.TicketStatus;
import com.aics.ticket.ticket.mapper.TicketEventMapper;
import com.aics.ticket.ticket.mapper.TicketMapper;
import com.aics.ticket.user.SysUser;
import com.aics.ticket.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TicketService {

    private final TicketMapper ticketMapper;
    private final TicketEventMapper ticketEventMapper;
    private final SysUserMapper userMapper;
    private final SysAgentMapper agentMapper;
    private final AgentGroupMapper groupMapper;

    public TicketService(TicketMapper ticketMapper, TicketEventMapper ticketEventMapper,
                         SysUserMapper userMapper, SysAgentMapper agentMapper,
                         AgentGroupMapper groupMapper) {
        this.ticketMapper = ticketMapper;
        this.ticketEventMapper = ticketEventMapper;
        this.userMapper = userMapper;
        this.agentMapper = agentMapper;
        this.groupMapper = groupMapper;
    }

    // ========== 工单创建（带自动分配） ==========

    @Transactional
    public TicketRecord create(TicketCreateRequest request, TicketSource source) {
        return createForUser(request, source, CurrentIdentity.currentIdOrDefault("USER", 1L), null);
    }

    @Transactional
    public TicketRecord createForUser(TicketCreateRequest request, TicketSource source,
                                       Long userId, Long conversationId) {
        TicketEntity ticket = new TicketEntity();
        ticket.title = request.getTitle();
        ticket.description = request.getDescription();
        ticket.category = request.getCategory();
        ticket.department = request.getDepartment();
        ticket.priority = request.getPriority() == null
            ? TicketPriority.NORMAL.name() : request.getPriority().name();
        ticket.source = source.name();
        ticket.userId = userId;
        ticket.conversationId = conversationId;
        ticket.attachmentUrls = request.getAttachmentUrls();
        ticket.timeoutAt = LocalDateTime.now().plusHours(priorityHours(ticket.priority));

        // 自动分配
        Long assignedAgentId = autoAssign(ticket.category, ticket.department);
        if (assignedAgentId != null) {
            ticket.assigneeAgentId = assignedAgentId;
            ticket.status = TicketStatus.ASSIGNED.name();
        } else {
            ticket.status = TicketStatus.CREATED.name(); // 进入公共待接单池
        }

        ticketMapper.insert(ticket);
        addEvent(ticket.id, "CREATE", null, ticket.status, "USER", userId,
            "用户提交工单" + (assignedAgentId != null ? "，已自动分配" : "，进入待接单池"));
        return toRecord(ticket, true);
    }

    /**
     * 自动分配：根据分类/部门匹配坐席组，再分配给组内在线且工单最少的坐席
     */
    private Long autoAssign(String category, String department) {
        try {
            // 1. 根据分类找坐席组
            QueryWrapper<AgentGroup> groupWrapper = new QueryWrapper<>();
            if (StringUtils.hasText(category)) {
                groupWrapper.and(w -> w.like("scene", category).or().like("name", category));
            }
            if (StringUtils.hasText(department)) {
                groupWrapper.or().like("name", department);
            }
            List<AgentGroup> groups = groupMapper.selectList(groupWrapper);
            if (groups.isEmpty()) return null;

            // 2. 找所有相关组的在线坐席
            List<Long> groupIds = groups.stream().map(g -> g.id).toList();
            List<SysAgent> agents = agentMapper.selectList(
                new QueryWrapper<SysAgent>()
                    .in("group_id", groupIds)
                    .eq("online_status", "ONLINE")
                    .eq("status", "ACTIVE"));
            if (agents.isEmpty()) return null;

            // 3. 找当前工单最少的坐席
            Long leastAgentId = null;
            int leastCount = Integer.MAX_VALUE;
            for (SysAgent agent : agents) {
                int count = Math.toIntExact(ticketMapper.selectCount(
                    new QueryWrapper<TicketEntity>()
                        .eq("assignee_agent_id", agent.id)
                        .ne("status", TicketStatus.COMPLETED.name())
                        .ne("status", TicketStatus.REJECTED.name())));
                if (count < leastCount) {
                    leastCount = count;
                    leastAgentId = agent.id;
                }
            }
            return leastAgentId;
        } catch (Exception e) {
            return null; // 分配失败进公共池
        }
    }

    // ========== 用户端工单 ==========

    public PageResponse<TicketRecord> listUserTickets(int page, int size, String status, String keyword) {
        Long userId = CurrentIdentity.currentIdOrDefault("USER", 1L);
        QueryWrapper<TicketEntity> wrapper = new QueryWrapper<TicketEntity>()
            .eq("user_id", userId)
            .orderByDesc("created_at");
        if (StringUtils.hasText(status)) wrapper.eq("status", status);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("title", keyword).or().like("description", keyword));
        }
        Page<TicketEntity> p = ticketMapper.selectPage(new Page<>(page, size), wrapper);
        List<TicketRecord> records = p.getRecords().stream()
            .map(t -> toRecord(t, false)).toList();
        return new PageResponse<>(records, p.getTotal(), page, size);
    }

    // ========== 工单池 & 坐席工单 ==========

    public PageResponse<TicketRecord> pool(int page, int size, String category, String priority) {
        QueryWrapper<TicketEntity> wrapper = new QueryWrapper<TicketEntity>()
            .eq("status", TicketStatus.CREATED.name())
            .isNull("assignee_agent_id")
            .orderByDesc("created_at");
        if (StringUtils.hasText(category)) wrapper.eq("category", category);
        if (StringUtils.hasText(priority)) wrapper.eq("priority", priority);
        Page<TicketEntity> p = ticketMapper.selectPage(new Page<>(page, size), wrapper);
        List<TicketRecord> records = p.getRecords().stream()
            .map(t -> toRecord(t, false)).toList();
        return new PageResponse<>(records, p.getTotal(), page, size);
    }

    public PageResponse<TicketRecord> mine(int page, int size, String status, String keyword) {
        Long agentId = CurrentIdentity.currentIdOrDefault("AGENT", 1L);
        QueryWrapper<TicketEntity> wrapper = new QueryWrapper<TicketEntity>()
            .eq("assignee_agent_id", agentId)
            .orderByDesc("updated_at");
        if (StringUtils.hasText(status)) wrapper.eq("status", status);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("title", keyword).or().like("description", keyword));
        }
        Page<TicketEntity> p = ticketMapper.selectPage(new Page<>(page, size), wrapper);
        List<TicketRecord> records = p.getRecords().stream()
            .map(t -> toRecord(t, false)).toList();
        return new PageResponse<>(records, p.getTotal(), page, size);
    }

    // ========== 管理员工单 ==========

    public PageResponse<TicketRecord> adminPage(int page, int size, String status, String category,
                                                 String priority, String keyword, Long agentId) {
        QueryWrapper<TicketEntity> wrapper = new QueryWrapper<TicketEntity>().orderByDesc("created_at");
        if (StringUtils.hasText(status)) wrapper.eq("status", status);
        if (StringUtils.hasText(category)) wrapper.eq("category", category);
        if (StringUtils.hasText(priority)) wrapper.eq("priority", priority);
        if (agentId != null) wrapper.eq("assignee_agent_id", agentId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("title", keyword).or().like("description", keyword));
        }
        Page<TicketEntity> p = ticketMapper.selectPage(new Page<>(page, size), wrapper);
        List<TicketRecord> records = p.getRecords().stream()
            .map(t -> toRecord(t, false)).toList();
        return new PageResponse<>(records, p.getTotal(), page, size);
    }

    // ========== 详情 & 操作 ==========

    public TicketRecord get(Long id) {
        return toRecord(getEntity(id), true);
    }

    @Transactional
    public TicketRecord accept(Long id, String operator) {
        TicketEntity ticket = getEntity(id);
        if (!TicketStatus.ASSIGNED.name().equals(ticket.status)
                && !TicketStatus.CREATED.name().equals(ticket.status)) {
            throw new BusinessException("只有待接单工单可以接单");
        }
        Long agentId = CurrentIdentity.currentIdOrDefault("AGENT", 1L);
        String from = ticket.status;
        ticket.status = TicketStatus.ACCEPTED.name();
        ticket.assigneeAgentId = agentId;
        ticketMapper.updateById(ticket);
        addEvent(ticket.id, "ACCEPT", from, ticket.status, "AGENT", agentId,
            StringUtils.hasText(operator) ? operator + " 接单" : "客服接单");
        return toRecord(getEntity(id), true);
    }

    @Transactional
    public TicketRecord process(Long id, String remark) {
        return updateStatus(id, TicketStatus.PROCESSING, "PROCESS",
            StringUtils.hasText(remark) ? remark : "工单处理中");
    }

    @Transactional
    public TicketRecord follow(Long id, String remark) {
        return updateStatus(id, TicketStatus.FOLLOWING, "FOLLOW",
            StringUtils.hasText(remark) ? remark : "需要继续跟进");
    }

    @Transactional
    public TicketRecord complete(Long id, String remark) {
        return updateStatus(id, TicketStatus.COMPLETED, "COMPLETE",
            StringUtils.hasText(remark) ? remark : "工单已完结");
    }

    @Transactional
    public TicketRecord reject(Long id, String remark) {
        return updateStatus(id, TicketStatus.REJECTED, "REJECT",
            StringUtils.hasText(remark) ? remark : "工单已驳回");
    }

    /**
     * 坐席完结后，管理员或坐席可将已完成工单归档
     */
    @Transactional
    public TicketRecord archive(Long id, String remark) {
        TicketEntity ticket = getEntity(id);
        if (!TicketStatus.COMPLETED.name().equals(ticket.status)) {
            throw new BusinessException("只有已完结的工单才能归档");
        }
        Long agentId = CurrentIdentity.currentIdOrDefault("AGENT", 1L);
        String from = ticket.status;
        ticket.status = TicketStatus.ARCHIVED.name();
        ticketMapper.updateById(ticket);
        addEvent(ticket.id, "ARCHIVE", from, ticket.status, "AGENT", agentId,
            StringUtils.hasText(remark) ? remark : "工单已归档");
        return toRecord(getEntity(id), true);
    }

    /**
     * 坐席申请调整工单优先级（记录事件，实际修改由管理员审核）
     */
    @Transactional
    public TicketRecord adjustPriority(Long id, String newPriority, String remark) {
        TicketEntity ticket = getEntity(id);
        // 校验优先级合法性
        try {
            TicketPriority.valueOf(newPriority);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的优先级: " + newPriority);
        }
        String oldPriority = ticket.priority;
        ticket.priority = newPriority;
        // 同步更新超时时间
        ticket.timeoutAt = LocalDateTime.now().plusHours(priorityHours(newPriority));
        ticketMapper.updateById(ticket);
        Long agentId = CurrentIdentity.currentIdOrDefault("AGENT", 1L);
        addEvent(ticket.id, "ADMIN_UPDATE", oldPriority, newPriority, "AGENT", agentId,
            StringUtils.hasText(remark) ? remark : "优先级从 " + oldPriority + " 调整为 " + newPriority);
        return toRecord(getEntity(id), true);
    }

    /**
     * 工单导出 CSV（管理端）
     *
     * @return CSV 文本内容
     */
    public String exportCsv(String status, String category, String priority,
                            String keyword, Long agentId) {
        QueryWrapper<TicketEntity> wrapper = new QueryWrapper<TicketEntity>().orderByDesc("created_at");
        if (StringUtils.hasText(status)) wrapper.eq("status", status);
        if (StringUtils.hasText(category)) wrapper.eq("category", category);
        if (StringUtils.hasText(priority)) wrapper.eq("priority", priority);
        if (agentId != null) wrapper.eq("assignee_agent_id", agentId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("title", keyword).or().like("description", keyword));
        }
        List<TicketEntity> tickets = ticketMapper.selectList(wrapper);

        // 批量预加载用户名和坐席名，避免 N+1 查询
        Map<Long, String> userNames = batchRequesterNames(tickets);
        Map<Long, String> agentNames = batchAgentNames(tickets);

        // CSV BOM for Excel UTF-8 compatibility
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("工单编号,标题,分类,部门,优先级,状态,来源,提交人,处理人,超时时间,完结时间,创建时间,更新时间\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (TicketEntity t : tickets) {
            StringJoiner row = new StringJoiner(",");
            row.add(String.valueOf(t.id));
            row.add(csvEscape(t.title));
            row.add(csvEscape(t.category));
            row.add(csvEscape(t.department));
            row.add(csvEscape(t.priority));
            row.add(csvEscape(statusLabel(t.status)));
            row.add(csvEscape(t.source));
            row.add(csvEscape(t.userId != null ? userNames.getOrDefault(t.userId, "用户" + t.userId) : ""));
            row.add(csvEscape(t.assigneeAgentId != null ? agentNames.getOrDefault(t.assigneeAgentId, "客服" + t.assigneeAgentId) : "待接单"));
            row.add(t.timeoutAt != null ? fmt.format(t.timeoutAt) : "");
            row.add(t.completedAt != null ? fmt.format(t.completedAt) : "");
            row.add(t.createdAt != null ? fmt.format(t.createdAt) : "");
            row.add(t.updatedAt != null ? fmt.format(t.updatedAt) : "");
            sb.append(row).append("\n");
        }
        return sb.toString();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** 批量加载用户名映射，避免 N+1 */
    private Map<Long, String> batchRequesterNames(List<TicketEntity> tickets) {
        List<Long> ids = tickets.stream().map(t -> t.userId).filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(u -> u.id, u -> u.username, (a, b) -> a));
    }

    /** 批量加载坐席名映射，避免 N+1 */
    private Map<Long, String> batchAgentNames(List<TicketEntity> tickets) {
        List<Long> ids = tickets.stream().map(t -> t.assigneeAgentId).filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return agentMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(
                        a -> a.id, a -> StringUtils.hasText(a.realName) ? a.realName : a.username, (x, y) -> x));
    }

    @Transactional
    public TicketRecord adminUpdate(Long id, Map<String, Object> request) {
        TicketEntity ticket = getEntity(id);
        String from = ticket.status;
        if (request.containsKey("status") && request.get("status") != null) {
            ticket.status = request.get("status").toString();
            if (TicketStatus.COMPLETED.name().equals(ticket.status)) {
                ticket.completedAt = LocalDateTime.now();
            }
        }
        if (request.containsKey("priority") && request.get("priority") != null) {
            ticket.priority = request.get("priority").toString();
        }
        if (request.containsKey("category") && request.get("category") != null) {
            ticket.category = request.get("category").toString();
        }
        if (request.containsKey("department") && request.get("department") != null) {
            ticket.department = request.get("department").toString();
        }
        if (request.containsKey("assigneeAgentId") && request.get("assigneeAgentId") != null) {
            ticket.assigneeAgentId = Long.valueOf(request.get("assigneeAgentId").toString());
        }
        ticketMapper.updateById(ticket);
        addEvent(ticket.id, "ADMIN_UPDATE", from, ticket.status, "ADMIN",
            CurrentIdentity.currentIdOrDefault("ADMIN", 1L), "管理员更新工单");
        return toRecord(getEntity(id), true);
    }

    // ========== 超时预警 ==========

    public List<TicketRecord> timeoutWarnings() {
        return ticketMapper.selectList(new QueryWrapper<TicketEntity>()
                        .ne("status", TicketStatus.COMPLETED.name())
                        .ne("status", TicketStatus.REJECTED.name())
                        .ne("status", TicketStatus.ARCHIVED.name())
                        .le("timeout_at", LocalDateTime.now().plusHours(4))
                        .orderByAsc("timeout_at"))
                .stream().map(t -> toRecord(t, false)).toList();
    }

    // ========== 统计数据 ==========

    /**
     * 概览统计：今日咨询量、AI 回复率、工单完结率、满意度
     */
    public Map<String, Object> overview() {
        Map<String, Object> data = new HashMap<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        // 今日工单量
        Long todayTickets = ticketMapper.selectCount(
            new QueryWrapper<TicketEntity>().ge("created_at", todayStart).lt("created_at", tomorrowStart));
        data.put("todayTickets", todayTickets);

        // 总工单量
        Long totalTickets = ticketMapper.selectCount(new QueryWrapper<>());
        data.put("totalTickets", totalTickets);

        // 已完结工单
        Long completed = ticketMapper.selectCount(
            new QueryWrapper<TicketEntity>().eq("status", TicketStatus.COMPLETED.name()));
        data.put("completedTickets", completed);

        // 完结率
        double completionRate = totalTickets > 0 ? completed * 100.0 / totalTickets : 0;
        data.put("completionRate", Math.round(completionRate * 10) / 10.0);

        // 待处理
        Long pending = ticketMapper.selectCount(
            new QueryWrapper<TicketEntity>().eq("status", TicketStatus.ASSIGNED.name()));
        data.put("pendingTickets", pending);

        // 处理中
        Long processing = ticketMapper.selectCount(
            new QueryWrapper<TicketEntity>().in("status",
                TicketStatus.ACCEPTED.name(), TicketStatus.PROCESSING.name(),
                TicketStatus.FOLLOWING.name()));
        data.put("processingTickets", processing);

        return data;
    }

    /**
     * 工单趋势数据（按天统计，最近 N 天）
     */
    public List<Map<String, Object>> trend(int days) {
        // 简化处理：按天分组统计创建量和完成量
        LocalDateTime start = LocalDate.now().minusDays(days - 1).atStartOfDay();
        List<TicketEntity> all = ticketMapper.selectList(
            new QueryWrapper<TicketEntity>().ge("created_at", start));

        // 按天聚合
        Map<LocalDate, int[]> daily = new HashMap<>();
        for (int i = 0; i < days; i++) {
            daily.put(LocalDate.now().minusDays(days - 1 - i), new int[2]);
        }
        for (TicketEntity t : all) {
            LocalDate d = t.createdAt.toLocalDate();
            if (daily.containsKey(d)) daily.get(d)[0]++;
            if (t.completedAt != null) {
                LocalDate cd = t.completedAt.toLocalDate();
                if (daily.containsKey(cd)) daily.get(cd)[1]++;
            }
        }
        return daily.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> {
                Map<String, Object> m = new HashMap<>();
                m.put("date", e.getKey().toString());
                m.put("created", e.getValue()[0]);
                m.put("completed", e.getValue()[1]);
                return m;
            }).toList();
    }

    /**
     * 工单状态分布
     */
    public List<Map<String, Object>> statusDistribution() {
        // 使用 SQL GROUP BY 聚合，避免全表加载到内存
        QueryWrapper<TicketEntity> wrapper = new QueryWrapper<TicketEntity>()
                .select("status", "COUNT(*) AS cnt")
                .groupBy("status");
        return ticketMapper.selectMaps(wrapper).stream()
            .map(row -> {
                Map<String, Object> m = new HashMap<>();
                String status = String.valueOf(row.get("status"));
                m.put("name", statusLabel(status));
                m.put("value", row.get("cnt"));
                m.put("status", status);
                return m;
            }).toList();
    }

    /**
     * 工单分类分布
     */
    public List<Map<String, Object>> categoryDistribution() {
        // 使用 SQL GROUP BY 聚合，避免全表加载到内存
        QueryWrapper<TicketEntity> wrapper = new QueryWrapper<TicketEntity>()
                .select("category", "COUNT(*) AS cnt")
                .groupBy("category");
        return ticketMapper.selectMaps(wrapper).stream()
            .map(row -> {
                Map<String, Object> m = new HashMap<>();
                String cat = row.get("category") != null
                        && !String.valueOf(row.get("category")).isEmpty()
                        ? String.valueOf(row.get("category")) : "未分类";
                m.put("name", cat);
                m.put("value", row.get("cnt"));
                return m;
            }).toList();
    }

    // ========== 内部方法 ==========

    private TicketRecord updateStatus(Long id, TicketStatus status, String eventType, String remark) {
        TicketEntity ticket = getEntity(id);
        Long agentId = CurrentIdentity.currentIdOrDefault("AGENT", 1L);
        if (ticket.assigneeAgentId == null) ticket.assigneeAgentId = agentId;
        String from = ticket.status;
        ticket.status = status.name();
        if (status == TicketStatus.COMPLETED) ticket.completedAt = LocalDateTime.now();
        ticketMapper.updateById(ticket);
        addEvent(ticket.id, eventType, from, ticket.status, "AGENT", agentId, remark);
        return toRecord(getEntity(id), true);
    }

    private TicketEntity getEntity(Long id) {
        TicketEntity ticket = ticketMapper.selectById(id);
        if (ticket == null) throw new BusinessException(404, "工单不存在");
        return ticket;
    }

    private void addEvent(Long ticketId, String eventType, String fromStatus, String toStatus,
                          String operatorType, Long operatorId, String remark) {
        TicketEventEntity event = new TicketEventEntity();
        event.ticketId = ticketId;
        event.eventType = eventType;
        event.fromStatus = fromStatus;
        event.toStatus = toStatus;
        event.operatorType = operatorType;
        event.operatorId = operatorId;
        event.remark = remark;
        ticketEventMapper.insert(event);
    }

    private TicketRecord toRecord(TicketEntity ticket, boolean includeEvents) {
        TicketRecord record = new TicketRecord();
        record.setId(ticket.id);
        record.setTitle(ticket.title);
        record.setDescription(ticket.description);
        record.setCategory(ticket.category);
        record.setDepartment(ticket.department);
        record.setPriority(parseEnum(TicketPriority.class, ticket.priority, TicketPriority.NORMAL));
        record.setStatus(parseEnum(TicketStatus.class, ticket.status, TicketStatus.CREATED));
        record.setSource(parseEnum(TicketSource.class, ticket.source, TicketSource.USER_FORM));
        record.setUserId(ticket.userId);
        record.setAssigneeAgentId(ticket.assigneeAgentId);
        record.setConversationId(ticket.conversationId);
        record.setAttachmentUrls(ticket.attachmentUrls);
        record.setTimeoutAt(ticket.timeoutAt);
        record.setCompletedAt(ticket.completedAt);
        record.setCreatedAt(ticket.createdAt);
        record.setUpdatedAt(ticket.updatedAt);

        if (ticket.userId != null) {
            SysUser user = userMapper.selectById(ticket.userId);
            record.setRequesterName(user == null ? "用户" + ticket.userId : user.username);
        }
        if (ticket.assigneeAgentId != null) {
            SysAgent agent = agentMapper.selectById(ticket.assigneeAgentId);
            record.setAssignee(agent == null ? "客服" + ticket.assigneeAgentId :
                (StringUtils.hasText(agent.realName) ? agent.realName : agent.username));
        } else {
            record.setAssignee("待接单");
        }
        if (includeEvents) {
            record.setEvents(ticketEventMapper.selectList(new QueryWrapper<TicketEventEntity>()
                                .eq("ticket_id", ticket.id).orderByAsc("created_at"))
                    .stream().map(this::toEventRecord).toList());
        }
        return record;
    }

    private TicketEventRecord toEventRecord(TicketEventEntity event) {
        TicketEventRecord record = new TicketEventRecord();
        record.setId(event.id);
        record.setEventType(event.eventType);
        record.setFromStatus(event.fromStatus);
        record.setToStatus(event.toStatus);
        record.setOperatorType(event.operatorType);
        record.setOperatorId(event.operatorId);
        record.setRemark(event.remark);
        record.setCreatedAt(event.createdAt);
        return record;
    }

    private long priorityHours(String priority) {
        if (TicketPriority.URGENT.name().equals(priority)) return 4;
        if (TicketPriority.HIGH.name().equals(priority)) return 12;
        return 24;
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "CREATED" -> "已创建";
            case "ASSIGNED" -> "待接单";
            case "ACCEPTED" -> "已接单";
            case "PROCESSING" -> "处理中";
            case "FOLLOWING" -> "跟进中";
            case "COMPLETED" -> "已完结";
            case "REJECTED" -> "已驳回";
            case "ARCHIVED" -> "已归档";
            default -> status;
        };
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        if (!StringUtils.hasText(value)) return fallback;
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
}
