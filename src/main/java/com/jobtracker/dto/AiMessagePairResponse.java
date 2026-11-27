package com.jobtracker.dto;

public record AiMessagePairResponse(
        AiConversationResponse conversation,
        AiMessageResponse userMessage,
        AiMessageResponse assistantMessage) {
}
