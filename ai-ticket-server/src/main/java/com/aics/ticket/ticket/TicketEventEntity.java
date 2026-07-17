package com.aics.ticket.ticket;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ticket_event")
public class TicketEventEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long ticketId;
    public String eventType;
    public String fromStatus;
    public String toStatus;
    public String operatorType;
    public Long operatorId;
    public String remark;
    public LocalDateTime createdAt;
}
