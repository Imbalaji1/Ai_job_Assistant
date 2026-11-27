package com.jobtracker.repository;

import com.jobtracker.entity.ApplicationResumeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationResumeSubmissionRepository
        extends JpaRepository<ApplicationResumeSubmission, Long> {
    List<ApplicationResumeSubmission> findAllByJobApplicationIdOrderBySubmittedAtDescIdDesc(
            Long jobApplicationId);
}
