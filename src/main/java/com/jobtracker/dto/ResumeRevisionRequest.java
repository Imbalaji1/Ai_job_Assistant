package com.jobtracker.dto;

import jakarta.validation.constraints.Size;

public record ResumeRevisionRequest(
        @Size(max = 4000) String instructions) {
}
