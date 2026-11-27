package com.jobtracker.dto;

import com.jobtracker.entity.ResumeVersion;
import com.jobtracker.enums.ResumeSource;

import java.time.LocalDateTime;

public record ResumeVersionResponse(
        Long id,
        Long parentVersionId,
        String originalFilename,
        String contentType,
        long fileSize,
        ResumeSource source,
        LocalDateTime createdAt,
        LocalDateTime deletedAt) {

    public static ResumeVersionResponse from(ResumeVersion version) {
        Long parentId = version.getParentVersion() == null
                ? null : version.getParentVersion().getId();
        return new ResumeVersionResponse(version.getId(), parentId,
                version.getOriginalFilename(), version.getContentType(),
                version.getFileSize(), version.getSource(), version.getCreatedAt(),
                version.getDeletedAt());
    }
}
