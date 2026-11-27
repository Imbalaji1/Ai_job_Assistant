package com.jobtracker.controller;

import com.jobtracker.dto.ResumeSubmissionRequest;
import com.jobtracker.dto.ResumeSubmissionResponse;
import com.jobtracker.service.ResumeApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications/{applicationId}/resume-submissions")
public class ApplicationResumeController {
    private final ResumeApplicationService resumeApplicationService;

    public ApplicationResumeController(ResumeApplicationService resumeApplicationService) {
        this.resumeApplicationService = resumeApplicationService;
    }

    @GetMapping
    public List<ResumeSubmissionResponse> findSubmissions(
            @PathVariable("applicationId") Long applicationId) {
        return resumeApplicationService.findSubmissions(applicationId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeSubmissionResponse linkExistingVersion(
            @PathVariable("applicationId") Long applicationId,
            @Valid @RequestBody ResumeSubmissionRequest request) {
        return resumeApplicationService.linkExistingVersion(applicationId,
                request.resumeVersionId());
    }
}
