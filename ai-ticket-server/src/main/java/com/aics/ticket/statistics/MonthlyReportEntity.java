package com.aics.ticket.statistics;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度服务报表实体（对应 monthly_report 表）
 */
@TableName("monthly_report")
public class MonthlyReportEntity {

    @TableId(type = IdType.AUTO)
    public Long id;

    /** 报表月份，格式 yyyy-MM */
    public String reportMonth;

    /** 当月咨询总量 */
    public Integer consultationCount;

    /** AI 自动回复率（百分比） */
    public BigDecimal aiReplyRate;

    /** 人工转接率（百分比） */
    public BigDecimal manualTransferRate;

    /** 工单完结率（百分比） */
    public BigDecimal ticketCompletionRate;

    /** 平均满意度评分 */
    public BigDecimal satisfactionScore;

    public LocalDateTime createdAt;
}
