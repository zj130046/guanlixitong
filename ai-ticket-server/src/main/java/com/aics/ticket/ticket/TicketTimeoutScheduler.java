package com.aics.ticket.ticket;

import com.aics.ticket.common.enums.TicketStatus;
import com.aics.ticket.ticket.mapper.TicketMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 工单超时自动检测定时任务
 * <p>
 * 每 30 分钟扫描一次未完结工单，对即将超时（1 小时内）和已超时的工单打印告警日志。
 * 后续可扩展为通过 WebSocket/SSE 推送实时告警通知。
 * </p>
 */
@Component
public class TicketTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(TicketTimeoutScheduler.class);

    private final TicketMapper ticketMapper;

    public TicketTimeoutScheduler(TicketMapper ticketMapper) {
        this.ticketMapper = ticketMapper;
    }

    /**
     * 每 30 分钟执行一次（在每小时的第 7 和 37 分钟触发，避免整点高峰）
     */
    @Scheduled(cron = "0 7,37 * * * *")
    public void checkTimeoutTickets() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime within1Hour = now.plusHours(1);

        // 1. 已超时但仍未完结的工单（超时告警）
        long overdueCount = ticketMapper.selectCount(new QueryWrapper<TicketEntity>()
                .ne("status", TicketStatus.COMPLETED.name())
                .ne("status", TicketStatus.REJECTED.name())
                .ne("status", TicketStatus.ARCHIVED.name())
                .le("timeout_at", now));
        if (overdueCount > 0) {
            log.warn("[超时告警] 已有 {} 个工单超过处理时限", overdueCount);
        }

        // 2. 即将在 1 小时内超时的工单（预警提示）
        long warningCount = ticketMapper.selectCount(new QueryWrapper<TicketEntity>()
                .ne("status", TicketStatus.COMPLETED.name())
                .ne("status", TicketStatus.REJECTED.name())
                .ne("status", TicketStatus.ARCHIVED.name())
                .gt("timeout_at", now)
                .le("timeout_at", within1Hour));
        if (warningCount > 0) {
            log.warn("[超时预警] {} 个工单将在 1 小时内超时", warningCount);
        }
    }
}
