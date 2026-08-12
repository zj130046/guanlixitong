package com.aics.ticket.statistics;

import com.aics.ticket.agent.SysAgent;
import com.aics.ticket.agent.mapper.SysAgentMapper;
import com.aics.ticket.chat.ChatConversationEntity;
import com.aics.ticket.chat.mapper.ChatConversationMapper;
import com.aics.ticket.common.ApiResponse;
import com.aics.ticket.kb.FaqEntry;
import com.aics.ticket.kb.mapper.FaqEntryMapper;
import com.aics.ticket.statistics.mapper.MonthlyReportMapper;
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
    private final TicketMapper ticketMapper;
    private final SatisfactionMapper satisfactionMapper;
    private final SysUserMapper userMapper;
    private final SysAgentMapper agentMapper;
    private final FaqEntryMapper faqEntryMapper;
    private final MonthlyReportMapper monthlyReportMapper;

    public StatisticsController(ChatConversationMapper conversationMapper,
                                TicketMapper ticketMapper,
                                SatisfactionMapper satisfactionMapper,
                                SysUserMapper userMapper,
                                SysAgentMapper agentMapper,
                                FaqEntryMapper faqEntryMapper,
                                MonthlyReportMapper monthlyReportMapper) {
        this.conversationMapper = conversationMapper;
        this.ticketMapper = ticketMapper;
        this.satisfactionMapper = satisfactionMapper;
        this.userMapper = userMapper;
        this.agentMapper = agentMapper;
        this.faqEntryMapper = faqEntryMapper;
        this.monthlyReportMapper = monthlyReportMapper;
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
        // SQL GROUP BY 聚合，避免全表加载
        QueryWrapper<TicketEntity> wrapper = new QueryWrapper<TicketEntity>()
                .select("status", "COUNT(*) AS cnt").groupBy("status");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : ticketMapper.selectMaps(wrapper)) {
            Map<String, Object> m = new HashMap<>();
            String status = String.valueOf(row.get("status"));
            m.put("name", statusLabel(status));
            m.put("value", row.get("cnt"));
            m.put("status", status);
            result.add(m);
        }
        return ApiResponse.ok(result);
    }

    // ========== 工单分类分布 ==========

    @GetMapping("/ticket-category-distribution")
    public ApiResponse<List<Map<String, Object>>> ticketCategoryDistribution() {
        // SQL GROUP BY 聚合，避免全表加载
        QueryWrapper<TicketEntity> wrapper = new QueryWrapper<TicketEntity>()
                .select("category", "COUNT(*) AS cnt").groupBy("category");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : ticketMapper.selectMaps(wrapper)) {
            Map<String, Object> m = new HashMap<>();
            String cat = row.get("category") != null
                    && !String.valueOf(row.get("category")).isEmpty()
                    ? String.valueOf(row.get("category")) : "未分类";
            m.put("name", cat);
            m.put("value", row.get("cnt"));
            result.add(m);
        }
        result.sort((a, b) -> Integer.compare((Integer) b.get("value"), (Integer) a.get("value")));
        return ApiResponse.ok(result);
    }

    // ========== 坐席工作量排行 ==========

    @GetMapping("/agent-ranking")
    public ApiResponse<List<Map<String, Object>>> agentRanking(
            @RequestParam(defaultValue = "10") int limit) {
        // 一次 SQL GROUP BY 查询替代 N+1
        QueryWrapper<TicketEntity> statsWrapper = new QueryWrapper<TicketEntity>()
                .select("assignee_agent_id",
                        "COUNT(*) AS total",
                        "SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) AS completed")
                .isNotNull("assignee_agent_id")
                .groupBy("assignee_agent_id");
        Map<Long, Map<String, Object>> statsMap = new HashMap<>();
        for (Map<String, Object> row : ticketMapper.selectMaps(statsWrapper)) {
            Long agentId = row.get("assignee_agent_id") != null
                    ? Long.valueOf(row.get("assignee_agent_id").toString()) : null;
            if (agentId != null) {
                Map<String, Object> s = new HashMap<>();
                s.put("completed", row.get("completed"));
                s.put("total", row.get("total"));
                statsMap.put(agentId, s);
            }
        }

        // 加载活跃坐席的基本信息
        List<SysAgent> agents = agentMapper.selectList(new QueryWrapper<SysAgent>().eq("status", "ACTIVE"));
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (SysAgent agent : agents) {
            Map<String, Object> stats = statsMap.getOrDefault(agent.id, Map.of("completed", 0L, "total", 0L));
            Map<String, Object> m = new HashMap<>();
            m.put("agentId", agent.id);
            m.put("agentName", agent.realName != null && !agent.realName.isEmpty() ? agent.realName : agent.username);
            m.put("completed", stats.get("completed"));
            m.put("total", stats.get("total"));
            ranking.add(m);
        }
        ranking.sort(Comparator.comparingLong(m -> -((Number) m.get("completed")).longValue()));
        if (ranking.size() > limit) ranking = ranking.subList(0, limit);
        return ApiResponse.ok(ranking);
    }

    // ========== 满意度统计 ==========

    @GetMapping("/satisfaction-stats")
    public ApiResponse<Map<String, Object>> satisfactionStats() {
        // SQL 聚合替代全表加载
        List<Map<String, Object>> scoreRows = satisfactionMapper.selectMaps(
                new QueryWrapper<SatisfactionEntity>()
                        .select("score", "COUNT(*) AS cnt")
                        .groupBy("score"));

        int total = 0;
        int[] distribution = new int[5];
        for (Map<String, Object> row : scoreRows) {
            Number score = (Number) row.get("score");
            Number cnt = (Number) row.get("cnt");
            if (score != null && cnt != null) {
                int s = score.intValue();
                int c = cnt.intValue();
                total += c;
                if (s >= 1 && s <= 5) distribution[s - 1] += c;
            }
        }

        double avg = averageScore();

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
        if (request.ticketId == null) {
            return ApiResponse.fail(400, "工单ID不能为空");
        }
        if (request.score == null || request.score < 1 || request.score > 5) {
            return ApiResponse.fail(400, "评分必须在1-5之间");
        }
        // 从会话获取当前用户ID
        request.userId = com.aics.ticket.auth.CurrentIdentity.currentIdOrDefault("USER", null);
        // 防止重复评价
        if (satisfactionMapper.selectCount(
                new QueryWrapper<SatisfactionEntity>()
                        .eq("ticket_id", request.ticketId)
                        .eq("user_id", request.userId)) > 0) {
            return ApiResponse.fail(400, "您已评价过该工单");
        }
        request.createdAt = LocalDateTime.now();
        satisfactionMapper.insert(request);
        return ApiResponse.ok(request);
    }

    // ========== 月度报表 ==========

    /**
     * 查询月度报表列表（按月份倒序）
     */
    @GetMapping("/monthly-reports")
    public ApiResponse<List<MonthlyReportEntity>> monthlyReports() {
        List<MonthlyReportEntity> reports = monthlyReportMapper.selectList(
            new QueryWrapper<MonthlyReportEntity>().orderByDesc("report_month"));
        return ApiResponse.ok(reports);
    }

    /**
     * 生成本月服务报表（如有则更新）
     */
    @PostMapping("/monthly-reports/generate")
    public ApiResponse<MonthlyReportEntity> generateMonthlyReport() {
        String month = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));

        // 查询是否已有本月报表
        MonthlyReportEntity report = monthlyReportMapper.selectOne(
            new QueryWrapper<MonthlyReportEntity>().eq("report_month", month));

        // 计算本月数据
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

        Long monthConsultations = conversationMapper.selectCount(
            new QueryWrapper<ChatConversationEntity>()
                .ge("created_at", startOfMonth).lt("created_at", endOfMonth));
        Long monthTransferred = conversationMapper.selectCount(
            new QueryWrapper<ChatConversationEntity>()
                .isNotNull("transferred_ticket_id")
                .ge("created_at", startOfMonth).lt("created_at", endOfMonth));
        Long monthTickets = ticketMapper.selectCount(
            new QueryWrapper<TicketEntity>()
                .ge("created_at", startOfMonth).lt("created_at", endOfMonth));
        Long monthCompleted = ticketMapper.selectCount(
            new QueryWrapper<TicketEntity>()
                .eq("status", "COMPLETED")
                .ge("completed_at", startOfMonth).lt("completed_at", endOfMonth));

        double aiRate = monthConsultations == 0 ? 0 :
            (monthConsultations - monthTransferred) * 100.0 / monthConsultations;
        double transferRate = monthConsultations == 0 ? 0 :
            monthTransferred * 100.0 / monthConsultations;
        double completionRate = monthTickets == 0 ? 0 :
            monthCompleted * 100.0 / monthTickets;
        double avgScore = averageScoreForMonth(startOfMonth, endOfMonth);

        if (report == null) {
            report = new MonthlyReportEntity();
            report.reportMonth = month;
        }
        report.consultationCount = monthConsultations.intValue();
        report.aiReplyRate = BigDecimal.valueOf(Math.round(aiRate * 10.0) / 10.0);
        report.manualTransferRate = BigDecimal.valueOf(Math.round(transferRate * 10.0) / 10.0);
        report.ticketCompletionRate = BigDecimal.valueOf(Math.round(completionRate * 10.0) / 10.0);
        report.satisfactionScore = BigDecimal.valueOf(avgScore);

        if (report.id == null) {
            monthlyReportMapper.insert(report);
        } else {
            monthlyReportMapper.updateById(report);
        }
        return ApiResponse.ok(report);
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

    /** 指定月份的满意度均分 */
    private double averageScoreForMonth(LocalDateTime start, LocalDateTime end) {
        Object value = satisfactionMapper.selectObjs(
            new QueryWrapper<SatisfactionEntity>().select("AVG(score)")
                .ge("created_at", start).lt("created_at", end))
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
