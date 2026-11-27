package com.jobtracker.dto;

import com.jobtracker.enums.ResumeKeepChoice;
import jakarta.validation.constraints.NotNull;

public record ResumeRetentionRequest(@NotNull ResumeKeepChoice keep) {
}
