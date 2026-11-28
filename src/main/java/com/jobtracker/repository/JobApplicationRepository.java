package com.jobtracker.repository;

import com.jobtracker.entity.JobApplication;
import com.jobtracker.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long>,
        JpaSpecificationExecutor<JobApplication> {
    long countByUserAccountIdAndStatus(Long userAccountId, ApplicationStatus status);

    long countByUserAccountId(Long userAccountId);

    java.util.Optional<JobApplication> findByIdAndUserAccountId(Long id, Long userAccountId);

    @Modifying
    @Query("update JobApplication application set application.userAccountId = :userAccountId "
            + "where application.userAccountId is null")
    int assignUnownedRecords(@Param("userAccountId") Long userAccountId);
}
