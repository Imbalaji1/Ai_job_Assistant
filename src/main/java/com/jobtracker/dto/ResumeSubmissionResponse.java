package com.jobtracker.dto;

import com.jobtracker.entity.ApplicationResumeSubmission;

import java.time.LocalDateTime;

public record ResumeSubmissionResponse(
        Long id,
        ResumeVersionResponse resumeVersion,
        LocalDateTime submittedAt) {

    public static ResumeSubmissionResponse from(ApplicationResumeSubmission submission) {
        ResumeVersionResponse version = submission.getResumeVersion() == null ? null
                : ResumeVersionResponse.from(submission.getResumeVersion());
        return new ResumeSubmissionResponse(submission.getId(), version,
                submission.getSubmittedAt());
    }
}
