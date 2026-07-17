package com.aics.ticket.chat;

import jakarta.validation.constraints.NotBlank;

public class ChatMessageRequest {

    private Long conversationId;

    @NotBlank
    private String message;

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
