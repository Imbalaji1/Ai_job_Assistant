package com.jobtracker.dto;

import com.jobtracker.enums.AiConversationPurpose;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiChatCreateRequest(
        @NotNull AiConversationPurpose purpose,
        @Size(max = 160) String title,
        Long resumeVersionId,
        Long jobApplicationId) {
}
