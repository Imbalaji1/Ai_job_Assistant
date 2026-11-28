package com.jobtracker.service;

import com.jobtracker.dto.JobApplicationRequest;
import com.jobtracker.dto.JobApplicationResponse;
import com.jobtracker.dto.ResumeSubmissionResponse;
import com.jobtracker.entity.ApplicationResumeSubmission;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.ResumeVersion;
import com.jobtracker.exception.ApplicationNotFoundException;
import com.jobtracker.repository.ApplicationResumeSubmissionRepository;
import com.jobtracker.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResumeApplicationService {
    private final ResumeService resumeService;
    private final JobApplicationService jobApplicationService;
    private final JobApplicationRepository jobApplicationRepository;
    private final ApplicationResumeSubmissionRepository submissionRepository;
    private final ActivityHistoryService historyService;
    private final CurrentAccountProvider currentAccountProvider;

    public ResumeApplicationService(ResumeService resumeService,
                                    JobApplicationService jobApplicationService,
                                    JobApplicationRepository jobApplicationRepository,
                                    ApplicationResumeSubmissionRepository submissionRepository,
                                    ActivityHistoryService historyService,
                                    CurrentAccountProvider currentAccountProvider) {
        this.resumeService = resumeService;
        this.jobApplicationService = jobApplicationService;
        this.jobApplicationRepository = jobApplicationRepository;
        this.submissionRepository = submissionRepository;
        this.historyService = historyService;
        this.currentAccountProvider = currentAccountProvider;
    }

    @Transactional
    public JobApplicationResponse createApplication(Long resumeVersionId,
                                                   JobApplicationRequest request) {
        ResumeVersion resume = resumeService.getActiveVersion(resumeVersionId);
        JobApplicationResponse response = jobApplicationService.create(request);
        JobApplication application = jobApplicationRepository.findByIdAndUserAccountId(
                        response.id(), currentAccountProvider.requireAccountId())
                .orElseThrow(() -> new ApplicationNotFoundException(response.id()));
        submissionRepository.save(new ApplicationResumeSubmission(application, resume));
        historyService.record("RESUME_LINKED_TO_APPLICATION",
                "Linked resume " + resume.getOriginalFilename() + " to "
                        + application.getCompanyName() + " — " + application.getJobRole(),
                "APPLICATION", application.getId());
        return response;
    }

    @Transactional(readOnly = true)
    public List<ResumeSubmissionResponse> findSubmissions(Long applicationId) {
        requireApplication(applicationId);
        return submissionRepository
                .findAllByJobApplicationIdOrderBySubmittedAtDescIdDesc(applicationId)
                .stream().map(ResumeSubmissionResponse::from).toList();
    }

    @Transactional
    public ResumeSubmissionResponse linkExistingVersion(Long applicationId,
                                                        Long resumeVersionId) {
        JobApplication application = requireApplication(applicationId);
        ResumeVersion resume = resumeService.getActiveVersion(resumeVersionId);
        ApplicationResumeSubmission submission = submissionRepository.save(
                new ApplicationResumeSubmission(application, resume));
        historyService.record("RESUME_LINKED_TO_APPLICATION",
                "Linked resume " + resume.getOriginalFilename() + " to "
                        + application.getCompanyName() + " — " + application.getJobRole(),
                "APPLICATION", applicationId);
        return ResumeSubmissionResponse.from(submission);
    }

    private JobApplication requireApplication(Long applicationId) {
        return jobApplicationRepository.findByIdAndUserAccountId(
                        applicationId, currentAccountProvider.requireAccountId())
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
    }
}
