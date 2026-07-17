package com.aics.ticket.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("chat_conversation")
public class ChatConversationEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long userId;
    public String status;
    public Long transferredTicketId;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
