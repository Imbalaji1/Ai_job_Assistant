package com.jobtracker.service;

import com.jobtracker.dto.StatusCountResponse;
import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final JobApplicationRepository jobApplicationRepository;

    public DashboardService(JobApplicationRepository jobApplicationRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
    }

    public StatusCountResponse getStatusCounts() {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, jobApplicationRepository.countByStatus(status));
        }
        return new StatusCountResponse(jobApplicationRepository.count(), counts);
    }
}
