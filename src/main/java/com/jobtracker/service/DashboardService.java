package com.jobtracker.service;

import com.jobtracker.dto.StatusCountResponse;
import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final JobApplicationRepository jobApplicationRepository;
    private final CurrentAccountProvider currentAccountProvider;

    @Autowired
    public DashboardService(JobApplicationRepository jobApplicationRepository,
                            CurrentAccountProvider currentAccountProvider) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.currentAccountProvider = currentAccountProvider;
    }

    public DashboardService(JobApplicationRepository jobApplicationRepository) {
        this(jobApplicationRepository, new CurrentAccountProvider());
    }

    public StatusCountResponse getStatusCounts() {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        long accountId = currentAccountProvider.requireAccountId();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, jobApplicationRepository
                    .countByUserAccountIdAndStatus(accountId, status));
        }
        return new StatusCountResponse(jobApplicationRepository.countByUserAccountId(accountId), counts);
    }
}
