package com.jobtracker.dto;

import com.jobtracker.entity.AiConversation;
import com.jobtracker.enums.AiConversationPurpose;

import java.time.LocalDateTime;

public record AiConversationResponse(
        Long id,
        String title,
        AiConversationPurpose purpose,
        Long resumeVersionId,
        Long jobApplicationId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AiConversationResponse from(AiConversation conversation) {
        return new AiConversationResponse(conversation.getId(), conversation.getTitle(),
                conversation.getPurpose(),
                conversation.getResumeVersion() == null
                        ? null : conversation.getResumeVersion().getId(),
                conversation.getJobApplication() == null
                        ? null : conversation.getJobApplication().getId(),
                conversation.getCreatedAt(), conversation.getUpdatedAt());
    }
}
