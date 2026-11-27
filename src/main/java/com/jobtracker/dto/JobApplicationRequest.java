package com.jobtracker.dto;

import com.jobtracker.enums.ApplicationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record JobApplicationRequest(
        @NotBlank @Size(max = 120) String companyName,
        @NotBlank @Size(max = 120) String jobRole,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal salaryLpa,
        @NotBlank @Size(max = 160) String location,
        @NotNull ApplicationStatus status,
        @NotNull LocalDate appliedDate,
        LocalDate interviewDate) {

    public JobApplicationRequest(String companyName, String jobRole, BigDecimal salaryLpa,
                                 String location, ApplicationStatus status,
                                 LocalDate appliedDate) {
        this(companyName, jobRole, salaryLpa, location, status, appliedDate, null);
    }
}
