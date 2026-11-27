package com.jobtracker.repository;

import com.jobtracker.entity.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {
    List<ResumeVersion> findAllByDeletedAtIsNullOrderByCreatedAtDescIdDesc();

    Optional<ResumeVersion> findByIdAndDeletedAtIsNull(Long id);
}
