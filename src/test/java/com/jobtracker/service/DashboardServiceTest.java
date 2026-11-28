package com.jobtracker.service;

import com.jobtracker.TestSecurityContext;
import com.jobtracker.dto.StatusCountResponse;
import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.repository.JobApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @AfterEach
    void clearSecurityContext() {
        TestSecurityContext.clear();
    }

    @Test
    void returnsTotalAndCountsForEveryStatus() {
        TestSecurityContext.setAccountId(1L);
        when(jobApplicationRepository.countByUserAccountId(1L)).thenReturn(3L);
        when(jobApplicationRepository.countByUserAccountIdAndStatus(1L, ApplicationStatus.APPLIED))
                .thenReturn(2L);
        when(jobApplicationRepository.countByUserAccountIdAndStatus(1L, ApplicationStatus.INTERVIEW))
                .thenReturn(1L);
        when(jobApplicationRepository.countByUserAccountIdAndStatus(1L, ApplicationStatus.SELECTED))
                .thenReturn(0L);
        when(jobApplicationRepository.countByUserAccountIdAndStatus(1L, ApplicationStatus.REJECTED))
                .thenReturn(0L);
        when(jobApplicationRepository.countByUserAccountIdAndStatus(1L, ApplicationStatus.WITHDRAWN))
                .thenReturn(0L);

        StatusCountResponse response = new DashboardService(jobApplicationRepository).getStatusCounts();

        assertEquals(3L, response.totalApplications());
        assertEquals(2L, response.countsByStatus().get(ApplicationStatus.APPLIED));
        assertEquals(1L, response.countsByStatus().get(ApplicationStatus.INTERVIEW));
        assertEquals(0L, response.countsByStatus().get(ApplicationStatus.WITHDRAWN));
        assertEquals(5, response.countsByStatus().size());
    }
}
