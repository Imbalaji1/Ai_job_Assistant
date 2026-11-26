package com.jobtracker.dto;

import com.jobtracker.enums.ApplicationStatus;

import java.time.LocalDateTime;

public record ApplicationStatusHistoryResponse(
        Long id,
        ApplicationStatus previousStatus,
        ApplicationStatus newStatus,
        LocalDateTime changedAt) {
}
