package com.jobtracker.dto;

import jakarta.validation.constraints.NotNull;

public record ResumeSubmissionRequest(@NotNull Long resumeVersionId) {
}
