CREATE TABLE resume_versions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_version_id BIGINT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data LONGBLOB NULL,
    source VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_resume_versions_parent (parent_version_id),
    INDEX idx_resume_versions_created (created_at),
    CONSTRAINT fk_resume_version_parent
        FOREIGN KEY (parent_version_id)
        REFERENCES resume_versions (id)
        ON DELETE SET NULL
);

CREATE TABLE application_resume_submissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_application_id BIGINT NOT NULL,
    resume_version_id BIGINT NULL,
    submitted_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_resume_submission_application (job_application_id),
    INDEX idx_resume_submission_version (resume_version_id),
    CONSTRAINT fk_resume_submission_application
        FOREIGN KEY (job_application_id)
        REFERENCES job_applications (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_resume_submission_version
        FOREIGN KEY (resume_version_id)
        REFERENCES resume_versions (id)
        ON DELETE SET NULL
);

CREATE TABLE ai_conversations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(160) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    resume_version_id BIGINT NULL,
    job_application_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_ai_conversations_updated (updated_at),
    INDEX idx_ai_conversations_resume (resume_version_id),
    INDEX idx_ai_conversations_application (job_application_id),
    CONSTRAINT fk_ai_conversation_resume
        FOREIGN KEY (resume_version_id)
        REFERENCES resume_versions (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_ai_conversation_application
        FOREIGN KEY (job_application_id)
        REFERENCES job_applications (id)
        ON DELETE SET NULL
);

CREATE TABLE ai_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    message_role VARCHAR(16) NOT NULL,
    content LONGTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_ai_messages_conversation_time (conversation_id, created_at),
    CONSTRAINT fk_ai_message_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES ai_conversations (id)
        ON DELETE CASCADE
);

CREATE TABLE activity_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(40) NOT NULL,
    summary VARCHAR(512) NOT NULL,
    related_entity_type VARCHAR(40) NULL,
    related_entity_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_activity_events_type_time (event_type, created_at),
    INDEX idx_activity_events_time (created_at)
);

CREATE TABLE user_profile (
    id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone_number VARCHAR(32) NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);
