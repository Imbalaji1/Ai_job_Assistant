CREATE TABLE job_applications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_name VARCHAR(120) NOT NULL,
    job_role VARCHAR(120) NOT NULL,
    salary_lpa DECIMAL(12, 2) NOT NULL,
    location VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    applied_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_job_applications_status (status),
    INDEX idx_job_applications_company (company_name),
    INDEX idx_job_applications_applied_date (applied_date)
);

CREATE TABLE application_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_application_id BIGINT NOT NULL,
    previous_status VARCHAR(32) NULL,
    new_status VARCHAR(32) NOT NULL,
    changed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_status_history_application_time (job_application_id, changed_at),
    CONSTRAINT fk_status_history_job_application
        FOREIGN KEY (job_application_id)
        REFERENCES job_applications (id)
        ON DELETE CASCADE
);
