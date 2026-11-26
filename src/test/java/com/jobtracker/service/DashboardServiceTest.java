package com.jobtracker.service;

import com.jobtracker.dto.StatusCountResponse;
import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.repository.JobApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Test
    void returnsTotalAndCountsForEveryStatus() {
        when(jobApplicationRepository.count()).thenReturn(3L);
        when(jobApplicationRepository.countByStatus(ApplicationStatus.APPLIED)).thenReturn(2L);
        when(jobApplicationRepository.countByStatus(ApplicationStatus.INTERVIEW)).thenReturn(1L);
        when(jobApplicationRepository.countByStatus(ApplicationStatus.SELECTED)).thenReturn(0L);
        when(jobApplicationRepository.countByStatus(ApplicationStatus.REJECTED)).thenReturn(0L);
        when(jobApplicationRepository.countByStatus(ApplicationStatus.WITHDRAWN)).thenReturn(0L);

        StatusCountResponse response = new DashboardService(jobApplicationRepository).getStatusCounts();

        assertEquals(3L, response.totalApplications());
        assertEquals(2L, response.countsByStatus().get(ApplicationStatus.APPLIED));
        assertEquals(1L, response.countsByStatus().get(ApplicationStatus.INTERVIEW));
        assertEquals(0L, response.countsByStatus().get(ApplicationStatus.WITHDRAWN));
        assertEquals(5, response.countsByStatus().size());
    }
}
