package com.jobtracker.entity;

import com.jobtracker.enums.ResumeSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_versions")
public class ResumeVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_account_id")
    private Long userAccountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_version_id")
    private ResumeVersion parentVersion;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Lob
    @Column(name = "file_data", columnDefinition = "LONGBLOB")
    private byte[] fileData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResumeSource source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected ResumeVersion() {
    }

    public ResumeVersion(ResumeVersion parentVersion, String originalFilename,
                         String contentType, long fileSize, byte[] fileData,
                         ResumeSource source) {
        this(parentVersion, originalFilename, contentType, fileSize, fileData, source, null);
    }

    public ResumeVersion(ResumeVersion parentVersion, String originalFilename,
                         String contentType, long fileSize, byte[] fileData,
                         ResumeSource source, Long userAccountId) {
        this.parentVersion = parentVersion;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.fileData = fileData;
        this.source = source;
        this.userAccountId = userAccountId;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void markDeleted(LocalDateTime deletedAt) {
        this.fileData = null;
        this.deletedAt = deletedAt;
    }

    public Long getId() { return id; }
    public Long getUserAccountId() { return userAccountId; }
    public void setUserAccountId(Long userAccountId) { this.userAccountId = userAccountId; }
    public ResumeVersion getParentVersion() { return parentVersion; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public byte[] getFileData() { return fileData; }
    public ResumeSource getSource() { return source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}
