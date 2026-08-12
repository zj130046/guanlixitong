package com.aics.ticket.ticket;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ticket")
public class TicketEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String title;
    public String description;
    public String category;
    public String department;
    public String priority;
    public String status;
    public String source;
    public Long userId;
    public Long assigneeAgentId;
    public Long conversationId;
    /** 附件 URL 列表，JSON 数组字符串，如 ["url1","url2"] */
    public String attachmentUrls;
    public LocalDateTime timeoutAt;
    public LocalDateTime completedAt;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
