package com.jobtracker.dto;

import com.jobtracker.enums.ApplicationStatus;

import java.util.Map;

public record StatusCountResponse(long totalApplications,
                                 Map<ApplicationStatus, Long> countsByStatus) {
}
