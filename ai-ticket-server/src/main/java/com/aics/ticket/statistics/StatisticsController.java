package com.aics.ticket.statistics;

import com.aics.ticket.agent.SysAgent;
import com.aics.ticket.agent.mapper.SysAgentMapper;
import com.aics.ticket.chat.ChatConversationEntity;
import com.aics.ticket.chat.mapper.ChatConversationMapper;
import com.aics.ticket.chat.mapper.ChatMessageMapper;
import com.aics.ticket.common.ApiResponse;
import com.aics.ticket.kb.FaqEntry;
import com.aics.ticket.kb.mapper.FaqEntryMapper;
import com.aics.ticket.statistics.mapper.SatisfactionMapper;
import com.aics.ticket.ticket.TicketEntity;
import com.aics.ticket.ticket.mapper.TicketMapper;
import com.aics.ticket.user.SysUser;
import com.aics.ticket.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final TicketMapper ticketMapper;
    private final SatisfactionMapper satisfactionMapper;
    private final SysUserMapper userMapper;
    private final SysAgentMapper agentMapper;
    private final FaqEntryMapper faqEntryMapper;

    public StatisticsController(ChatConversationMapper conversationMapper,
                                ChatMessageMapper messageMapper,
                                TicketMapper ticketMapper,
                                SatisfactionMapper satisfactionMapper,
                                SysUserMapper userMapper,
                                SysAgentMapper agentMapper,
                                FaqEntryMapper faqEntryMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.ticketMapper = ticketMapper;
        this.satisfactionMapper = satisfactionMapper;
        this.userMapper = userMapper;
        this.agentMapper = agentMapper;
        this.faqEntryMapper = faqEntryMapper;
    }

    // ========== 总览 ==========

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Long todayConsultations = conversationMapper.selectCount(
            new QueryWrapper<ChatConversationEntity>().ge("created_at", startOfDay));
        Long totalConversations = conversationMapper.selectCount(new QueryWrapper<>());
        Long transferredConversations = conversationMapper.selectCount(
            new QueryWrapper<ChatConversationEntity>().isNotNull("transferred_ticket_id"));
        Long totalTickets = ticketMapper.selectCount(new QueryWrapper<>());
        Long completedTickets = ticketMapper.selectCount(
            new QueryWrapper<TicketEntity>().eq("status", "COMPLETED"));
        Long pendingTickets = ticketMapper.selectCount(
            new QueryWrapper<TicketEntity>().in("status", "CREATED", "ASSIGNED"));
        Long processingTickets = ticketMapper.selectCount(
            new QueryWrapper<TicketEntity>().in("status", "ACCEPTED", "PROCESSING", "FOLLOWING"));
        Long userCount = userMapper.selectCount(new QueryWrapper<SysUser>().eq("status", "ACTIVE"));
        Long agentCount = agentMapper.selectCount(new QueryWrapper<SysAgent>().eq("status", "ACTIVE"));
        Long onlineAgents = agentMapper.selectCount(
            new QueryWrapper<SysAgent>().eq("online_status", "ONLINE"));
        Long faqCount = faqEntryMapper.selectCount(new QueryWrapper<FaqEntry>().eq("enabled", 1));

        // AI 回复率 = (总会话 - 转人工) / 总会话
        double aiReplyRate = totalConversations == 0 ? 0 :
            (totalConversations - transferredConversations) * 100.0 / totalConversations;
        double manualTransferRate = totalConversations == 0 ? 0 :
            transferredConversations * 100.0 / totalConversations;
        double ticketCompletionRate = totalTickets == 0 ? 0 :
            completedTickets * 100.0 / totalTickets;

        Map<String, Object> data = new HashMap<>();
        data.put("todayConsultations", todayConsultations);
        data.put("totalConversations", totalConversations);
        data.put("aiReplyRate", round1(aiReplyRate));
        data.put("manualTransferRate", round1(manualTransferRate));
        data.put("ticketCompletionRate", round1(ticketCompletionRate));
        data.put("satisfactionScore", averageScore());
        data.put("totalTickets", totalTickets);
        data.put("pendingTickets", pendingTickets);
        data.put("processingTickets", processingTickets);
        data.put("completedTickets", completedTickets);
        data.put("userCount", userCount);
        data.put("agentCount", agentCount);
        data.put("onlineAgents", onlineAgents);
        data.put("faqCount", faqCount);
        return ApiResponse.ok(data);
    }

    // ========== 咨询量趋势 ==========

    @GetMapping("/consultation-trend")
    public ApiResponse<List<Map<String, Object>>> consultationTrend(
            @RequestParam(defaultValue = "7") int days) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1).atStartOfDay();
        List<ChatConversationEntity> all = conversationMapper.selectList(
            new QueryWrapper<ChatConversationEntity>().ge("created_at", start));

        Map<LocalDate, int[]> daily = new HashMap<>();
        for (int i = 0; i < days; i++) {
            daily.put(LocalDate.now().minusDays(days - 1 - i), new int[2]);
        }
        for (ChatConversationEntity c : all) {
            LocalDate d = c.createdAt.toLocalDate();
            if (daily.containsKey(d)) {
                daily.get(d)[0]++;
                if (c.transferredTicketId != null) daily.get(d)[1]++;
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        daily.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> {
                Map<String, Object> m = new HashMap<>();
                m.put("date", e.getKey().toString());
                m.put("total", e.getValue()[0]);
                m.put("transferred", e.getValue()[1]);
                m.put("aiReplied", e.getValue()[0] - e.getValue()[1]);
                result.add(m);
            });
        return ApiResponse.ok(result);
    }

    // ========== 工单趋势 ==========

    @GetMapping("/ticket-trend")
    public ApiResponse<List<Map<String, Object>>> ticketTrend(
            @RequestParam(defaultValue = "7") int days) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1).atStartOfDay();
        List<TicketEntity> all = ticketMapper.selectList(
            new QueryWrapper<TicketEntity>().ge("created_at", start));

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
        List<Map<String, Object>> result = new ArrayList<>();
        daily.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> {
                Map<String, Object> m = new HashMap<>();
                m.put("date", e.getKey().toString());
                m.put("created", e.getValue()[0]);
                m.put("completed", e.getValue()[1]);
                result.add(m);
            });
        return ApiResponse.ok(result);
    }

    // ========== 工单状态分布 ==========

    @GetMapping("/ticket-status-distribution")
    public ApiResponse<List<Map<String, Object>>> ticketStatusDistribution() {
        List<TicketEntity> all = ticketMapper.selectList(new QueryWrapper<>());
        Map<String, Integer> countMap = new HashMap<>();
        for (TicketEntity t : all) {
            countMap.merge(t.status, 1, Integer::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        countMap.forEach((k, v) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", statusLabel(k));
            m.put("value", v);
            m.put("status", k);
            result.add(m);
        });
        return ApiResponse.ok(result);
    }

    // ========== 工单分类分布 ==========

    @GetMapping("/ticket-category-distribution")
    public ApiResponse<List<Map<String, Object>>> ticketCategoryDistribution() {
        List<TicketEntity> all = ticketMapper.selectList(new QueryWrapper<>());
        Map<String, Integer> countMap = new HashMap<>();
        for (TicketEntity t : all) {
            String cat = t.category == null || t.category.isEmpty() ? "未分类" : t.category;
            countMap.merge(cat, 1, Integer::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        countMap.forEach((k, v) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", k);
            m.put("value", v);
            result.add(m);
        });
        result.sort((a, b) -> Integer.compare((Integer) b.get("value"), (Integer) a.get("value")));
        return ApiResponse.ok(result);
    }

    // ========== 坐席工作量排行 ==========

    @GetMapping("/agent-ranking")
    public ApiResponse<List<Map<String, Object>>> agentRanking(
            @RequestParam(defaultValue = "10") int limit) {
        List<SysAgent> agents = agentMapper.selectList(new QueryWrapper<SysAgent>().eq("status", "ACTIVE"));
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (SysAgent agent : agents) {
            Long processed = ticketMapper.selectCount(
                new QueryWrapper<TicketEntity>()
                    .eq("assignee_agent_id", agent.id)
                    .eq("status", "COMPLETED"));
            Long total = ticketMapper.selectCount(
                new QueryWrapper<TicketEntity>().eq("assignee_agent_id", agent.id));
            Map<String, Object> m = new HashMap<>();
            m.put("agentId", agent.id);
            m.put("agentName", agent.realName != null && !agent.realName.isEmpty() ? agent.realName : agent.username);
            m.put("completed", processed);
            m.put("total", total);
            ranking.add(m);
        }
        ranking.sort(Comparator.comparingLong(m -> -((Number) m.get("completed")).longValue()));
        if (ranking.size() > limit) ranking = ranking.subList(0, limit);
        return ApiResponse.ok(ranking);
    }

    // ========== 满意度统计 ==========

    @GetMapping("/satisfaction-stats")
    public ApiResponse<Map<String, Object>> satisfactionStats() {
        List<SatisfactionEntity> all = satisfactionMapper.selectList(new QueryWrapper<>());
        int total = all.size();
        double avg = averageScore();

        // 各星级分布
        int[] distribution = new int[5];
        for (SatisfactionEntity s : all) {
            if (s.score >= 1 && s.score <= 5) distribution[s.score - 1]++;
        }

        List<Map<String, Object>> distList = new ArrayList<>();
        for (int i = 4; i >= 0; i--) {
            Map<String, Object> m = new HashMap<>();
            m.put("score", i + 1);
            m.put("count", distribution[i]);
            m.put("label", (i + 1) + "星");
            distList.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("averageScore", avg);
        result.put("distribution", distList);
        return ApiResponse.ok(result);
    }

    // ========== 满意度提交 ==========

    @PostMapping("/satisfaction")
    public ApiResponse<SatisfactionEntity> submitSatisfaction(@RequestBody SatisfactionEntity request) {
        satisfactionMapper.insert(request);
        return ApiResponse.ok(request);
    }

    // ========== 内部方法 ==========

    private double averageScore() {
        Object value = satisfactionMapper.selectObjs(
            new QueryWrapper<SatisfactionEntity>().select("AVG(score)"))
            .stream().findFirst().orElse(null);
        if (value == null) return 0;
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(1, java.math.RoundingMode.HALF_UP).doubleValue();
        }
        return Math.round(Double.parseDouble(value.toString()) * 10.0) / 10.0;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
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
}
