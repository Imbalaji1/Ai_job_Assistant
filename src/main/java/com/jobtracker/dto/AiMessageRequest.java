package com.jobtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiMessageRequest(
        @NotBlank @Size(max = 12000) String message) {
}
