package com.jobtracker.service;

import com.jobtracker.dto.JobApplicationRequest;
import com.jobtracker.dto.JobApplicationResponse;
import com.jobtracker.entity.ApplicationStatusHistory;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.exception.ApplicationNotFoundException;
import com.jobtracker.repository.ApplicationStatusHistoryRepository;
import com.jobtracker.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {
    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private ApplicationStatusHistoryRepository historyRepository;

    private JobApplicationService service;

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(jobApplicationRepository, historyRepository);
    }

    @Test
    void createSavesApplicationAndInitialStatusHistory() {
        when(jobApplicationRepository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        JobApplicationRequest request = request(ApplicationStatus.APPLIED);

        JobApplicationResponse response = service.create(request);

        assertEquals("Example Company", response.companyName());
        assertEquals(ApplicationStatus.APPLIED, response.status());
        ArgumentCaptor<ApplicationStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertNull(historyCaptor.getValue().getPreviousStatus());
        assertEquals(ApplicationStatus.APPLIED, historyCaptor.getValue().getNewStatus());
    }

    @Test
    void updateStatusRecordsTheChange() {
        JobApplication application = application(ApplicationStatus.INTERVIEW);
        when(jobApplicationRepository.findById(7L)).thenReturn(Optional.of(application));
        when(jobApplicationRepository.saveAndFlush(application)).thenReturn(application);

        JobApplicationResponse response = service.updateStatus(7L, ApplicationStatus.SELECTED);

        assertEquals(ApplicationStatus.SELECTED, response.status());
        ArgumentCaptor<ApplicationStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(ApplicationStatus.INTERVIEW, historyCaptor.getValue().getPreviousStatus());
        assertEquals(ApplicationStatus.SELECTED, historyCaptor.getValue().getNewStatus());
    }

    @Test
    void updateStatusDoesNotRecordAChangeWhenStatusIsUnchanged() {
        JobApplication application = application(ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findById(7L)).thenReturn(Optional.of(application));
        when(jobApplicationRepository.saveAndFlush(application)).thenReturn(application);

        service.updateStatus(7L, ApplicationStatus.APPLIED);

        verify(historyRepository, never()).save(any(ApplicationStatusHistory.class));
    }

    @Test
    void findByIdThrowsWhenApplicationDoesNotExist() {
        when(jobApplicationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class, () -> service.findById(404L));
    }

    @Test
    void deleteRemovesStatusHistoryBeforeApplication() {
        JobApplication application = application(ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findById(7L)).thenReturn(Optional.of(application));

        service.delete(7L);

        verify(historyRepository).deleteAllByJobApplicationId(7L);
        verify(jobApplicationRepository).delete(application);
    }

    @Test
    void findAllAppliesPageAndSizeAndRejectsInvalidPageSize() {
        when(jobApplicationRepository.findAll(
                ArgumentMatchers.<Specification<JobApplication>>any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.findAll("Example", "applied", 1, 15);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jobApplicationRepository).findAll(
                ArgumentMatchers.<Specification<JobApplication>>any(), pageableCaptor.capture());
        assertEquals(1, pageableCaptor.getValue().getPageNumber());
        assertEquals(15, pageableCaptor.getValue().getPageSize());
        assertThrows(IllegalArgumentException.class, () -> service.findAll(null, null, 0, 101));
    }

    private JobApplicationRequest request(ApplicationStatus status) {
        return new JobApplicationRequest("Example Company", "Java Developer",
                new BigDecimal("8.50"), "Bengaluru", status,
                LocalDate.of(2026, 11, 26));
    }

    private JobApplication application(ApplicationStatus status) {
        return new JobApplication("Example Company", "Java Developer",
                new BigDecimal("8.50"), "Bengaluru", status,
                LocalDate.of(2026, 11, 26));
    }
}
