package com.jobtracker.entity;

import com.jobtracker.enums.ApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications")
public class JobApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 120)
    private String companyName;

    @Column(name = "job_role", nullable = false, length = 120)
    private String jobRole;

    @Column(name = "salary_lpa", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaryLpa;

    @Column(nullable = false, length = 160)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplicationStatus status;

    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;

    @Column(name = "interview_date")
    private LocalDate interviewDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected JobApplication() {
    }

    public JobApplication(String companyName, String jobRole, BigDecimal salaryLpa,
                          String location, ApplicationStatus status, LocalDate appliedDate) {
        this(companyName, jobRole, salaryLpa, location, status, appliedDate, null);
    }

    public JobApplication(String companyName, String jobRole, BigDecimal salaryLpa,
                          String location, ApplicationStatus status, LocalDate appliedDate,
                          LocalDate interviewDate) {
        this.companyName = companyName;
        this.jobRole = jobRole;
        this.salaryLpa = salaryLpa;
        this.location = location;
        this.status = status;
        this.appliedDate = appliedDate;
        this.interviewDate = interviewDate;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getJobRole() { return jobRole; }
    public void setJobRole(String jobRole) { this.jobRole = jobRole; }
    public BigDecimal getSalaryLpa() { return salaryLpa; }
    public void setSalaryLpa(BigDecimal salaryLpa) { this.salaryLpa = salaryLpa; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public LocalDate getAppliedDate() { return appliedDate; }
    public void setAppliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; }
    public LocalDate getInterviewDate() { return interviewDate; }
    public void setInterviewDate(LocalDate interviewDate) { this.interviewDate = interviewDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
