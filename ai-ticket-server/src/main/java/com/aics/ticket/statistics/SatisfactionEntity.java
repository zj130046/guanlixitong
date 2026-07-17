package com.aics.ticket.statistics;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("satisfaction")
public class SatisfactionEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long ticketId;
    public Long userId;
    public Integer score;
    public String comment;
    public LocalDateTime createdAt;
}
