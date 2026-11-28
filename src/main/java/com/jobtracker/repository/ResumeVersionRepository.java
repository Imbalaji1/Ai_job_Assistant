package com.jobtracker.repository;

import com.jobtracker.entity.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {
    List<ResumeVersion> findAllByUserAccountIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
            Long userAccountId);

    Optional<ResumeVersion> findByIdAndUserAccountIdAndDeletedAtIsNull(Long id,
                                                                        Long userAccountId);

    @Modifying
    @Query("update ResumeVersion resume set resume.userAccountId = :userAccountId "
            + "where resume.userAccountId is null")
    int assignUnownedRecords(@Param("userAccountId") Long userAccountId);
}
