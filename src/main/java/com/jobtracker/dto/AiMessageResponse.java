package com.jobtracker.dto;

import com.jobtracker.entity.AiMessage;
import com.jobtracker.enums.AiMessageRole;

import java.time.LocalDateTime;

public record AiMessageResponse(
        Long id,
        AiMessageRole role,
        String content,
        LocalDateTime createdAt) {

    public static AiMessageResponse from(AiMessage message) {
        return new AiMessageResponse(message.getId(), message.getRole(),
                message.getContent(), message.getCreatedAt());
    }
}
