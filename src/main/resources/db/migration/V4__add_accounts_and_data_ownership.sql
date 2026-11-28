CREATE TABLE user_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_user_accounts_email UNIQUE (email)
);

ALTER TABLE job_applications ADD COLUMN user_account_id BIGINT NULL;
CREATE INDEX idx_job_applications_user_account ON job_applications (user_account_id);
ALTER TABLE job_applications
    ADD CONSTRAINT fk_job_applications_user_account
        FOREIGN KEY (user_account_id) REFERENCES user_accounts (id) ON DELETE CASCADE;

ALTER TABLE resume_versions ADD COLUMN user_account_id BIGINT NULL;
CREATE INDEX idx_resume_versions_user_account ON resume_versions (user_account_id);
ALTER TABLE resume_versions
    ADD CONSTRAINT fk_resume_versions_user_account
        FOREIGN KEY (user_account_id) REFERENCES user_accounts (id) ON DELETE CASCADE;

ALTER TABLE ai_conversations ADD COLUMN user_account_id BIGINT NULL;
CREATE INDEX idx_ai_conversations_user_account ON ai_conversations (user_account_id);
ALTER TABLE ai_conversations
    ADD CONSTRAINT fk_ai_conversations_user_account
        FOREIGN KEY (user_account_id) REFERENCES user_accounts (id) ON DELETE CASCADE;

ALTER TABLE activity_events ADD COLUMN user_account_id BIGINT NULL;
CREATE INDEX idx_activity_events_user_account ON activity_events (user_account_id);
ALTER TABLE activity_events
    ADD CONSTRAINT fk_activity_events_user_account
        FOREIGN KEY (user_account_id) REFERENCES user_accounts (id) ON DELETE CASCADE;

ALTER TABLE user_profile ADD COLUMN user_account_id BIGINT NULL;
CREATE UNIQUE INDEX uq_user_profile_user_account ON user_profile (user_account_id);
ALTER TABLE user_profile
    ADD CONSTRAINT fk_user_profile_user_account
        FOREIGN KEY (user_account_id) REFERENCES user_accounts (id) ON DELETE CASCADE;
