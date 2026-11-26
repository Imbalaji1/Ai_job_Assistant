package com.jobtracker.dto;

import com.jobtracker.enums.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record JobApplicationResponse(
        Long id,
        String companyName,
        String jobRole,
        BigDecimal salaryLpa,
        String location,
        ApplicationStatus status,
        LocalDate appliedDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
