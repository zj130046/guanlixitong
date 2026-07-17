package com.aics.ticket.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("chat_message")
public class ChatMessageEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long conversationId;
    public String senderType;
    public Long senderId;
    public String content;
    public LocalDateTime createdAt;
}
