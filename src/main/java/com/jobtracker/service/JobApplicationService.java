package com.jobtracker.service;

import com.jobtracker.dto.ApplicationStatusHistoryResponse;
import com.jobtracker.dto.JobApplicationRequest;
import com.jobtracker.dto.JobApplicationResponse;
import com.jobtracker.entity.ApplicationStatusHistory;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.exception.ApplicationNotFoundException;
import com.jobtracker.repository.ApplicationStatusHistoryRepository;
import com.jobtracker.repository.JobApplicationRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class JobApplicationService {
    private static final int MAX_PAGE_SIZE = 100;

    private final JobApplicationRepository jobApplicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;

    public JobApplicationService(JobApplicationRepository jobApplicationRepository,
                                 ApplicationStatusHistoryRepository historyRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public JobApplicationResponse create(JobApplicationRequest request) {
        JobApplication application = new JobApplication(
                request.companyName().trim(), request.jobRole().trim(), request.salaryLpa(),
                request.location().trim(), request.status(), request.appliedDate());
        JobApplication saved = jobApplicationRepository.save(application);
        historyRepository.save(new ApplicationStatusHistory(saved, null, saved.getStatus()));
        return toResponse(saved);
    }

    public Page<JobApplicationResponse> findAll(String company, String statusValue,
                                                int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }

        ApplicationStatus status = parseOptionalStatus(statusValue);
        String companyFilter = company == null || company.isBlank() ? null : company.trim();

        Specification<JobApplication> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (companyFilter != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("companyName")),
                        "%" + companyFilter.toLowerCase(Locale.ROOT) + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("appliedDate"), Sort.Order.desc("id")));
        return jobApplicationRepository.findAll(specification, pageable).map(this::toResponse);
    }

    public JobApplicationResponse findById(Long id) {
        return toResponse(getApplication(id));
    }

    @Transactional
    public JobApplicationResponse update(Long id, JobApplicationRequest request) {
        JobApplication application = getApplication(id);
        ApplicationStatus previousStatus = application.getStatus();
        application.setCompanyName(request.companyName().trim());
        application.setJobRole(request.jobRole().trim());
        application.setSalaryLpa(request.salaryLpa());
        application.setLocation(request.location().trim());
        application.setAppliedDate(request.appliedDate());
        application.setStatus(request.status());

        JobApplication saved = jobApplicationRepository.saveAndFlush(application);
        saveStatusChange(saved, previousStatus, saved.getStatus());
        return toResponse(saved);
    }

    @Transactional
    public JobApplicationResponse updateStatus(Long id, ApplicationStatus newStatus) {
        JobApplication application = getApplication(id);
        ApplicationStatus previousStatus = application.getStatus();
        application.setStatus(newStatus);
        JobApplication saved = jobApplicationRepository.saveAndFlush(application);
        saveStatusChange(saved, previousStatus, saved.getStatus());
        return toResponse(saved);
    }

    public List<ApplicationStatusHistoryResponse> getStatusHistory(Long id) {
        getApplication(id);
        return historyRepository
                .findAllByJobApplicationIdOrderByChangedAtAscIdAsc(id)
                .stream()
                .map(history -> new ApplicationStatusHistoryResponse(
                        history.getId(), history.getPreviousStatus(),
                        history.getNewStatus(), history.getChangedAt()))
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        JobApplication application = getApplication(id);
        historyRepository.deleteAllByJobApplicationId(id);
        jobApplicationRepository.delete(application);
    }

    private void saveStatusChange(JobApplication application,
                                  ApplicationStatus previousStatus,
                                  ApplicationStatus newStatus) {
        if (previousStatus != newStatus) {
            historyRepository.save(new ApplicationStatusHistory(
                    application, previousStatus, newStatus));
        }
    }

    private JobApplication getApplication(Long id) {
        return jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    private ApplicationStatus parseOptionalStatus(String statusValue) {
        if (statusValue == null || statusValue.isBlank()) {
            return null;
        }
        return ApplicationStatus.fromValue(statusValue);
    }

    private JobApplicationResponse toResponse(JobApplication application) {
        return new JobApplicationResponse(
                application.getId(), application.getCompanyName(), application.getJobRole(),
                application.getSalaryLpa(), application.getLocation(), application.getStatus(),
                application.getAppliedDate(), application.getCreatedAt(), application.getUpdatedAt());
    }
}
