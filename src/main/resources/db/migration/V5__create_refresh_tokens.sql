CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_account_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    INDEX idx_refresh_tokens_user_expiry (user_account_id, expires_at),
    CONSTRAINT fk_refresh_tokens_user_account
        FOREIGN KEY (user_account_id) REFERENCES user_accounts (id) ON DELETE CASCADE
);
