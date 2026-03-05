-- V3: Create sales_channels_users and login_logs tables

CREATE TABLE sales_channels_users (
    id          INT           NOT NULL AUTO_INCREMENT,
    user_id     INT           NOT NULL,
    channel     VARCHAR(50)   NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_sales_channels_users_pair (user_id, channel),
    INDEX idx_sales_channels_users_user_id (user_id),
    CONSTRAINT fk_sales_channels_users_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE login_logs (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    user_id         INT           NULL,
    username        VARCHAR(50)   NOT NULL,
    login_type      ENUM('success', 'failed') NOT NULL,
    ip_address      VARCHAR(45)   NULL,
    user_agent      TEXT          NULL,
    failed_reason   VARCHAR(255)  NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_login_logs_user_id (user_id),
    INDEX idx_login_logs_username (username),
    INDEX idx_login_logs_created_at (created_at),
    INDEX idx_login_logs_login_type (login_type),
    INDEX idx_login_logs_user_id_created_at (user_id, created_at),
    CONSTRAINT fk_login_logs_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
