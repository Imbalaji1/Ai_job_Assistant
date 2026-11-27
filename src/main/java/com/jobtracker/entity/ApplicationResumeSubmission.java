package com.jobtracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "application_resume_submissions")
public class ApplicationResumeSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_application_id", nullable = false)
    private JobApplication jobApplication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_version_id")
    private ResumeVersion resumeVersion;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    protected ApplicationResumeSubmission() {
    }

    public ApplicationResumeSubmission(JobApplication jobApplication,
                                       ResumeVersion resumeVersion) {
        this.jobApplication = jobApplication;
        this.resumeVersion = resumeVersion;
    }

    @PrePersist
    void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public JobApplication getJobApplication() { return jobApplication; }
    public ResumeVersion getResumeVersion() { return resumeVersion; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
