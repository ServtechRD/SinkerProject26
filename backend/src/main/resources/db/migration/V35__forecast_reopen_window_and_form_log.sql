-- V35: R4 補登期間、表單異動紀錄、權限 35「開放補登」

CREATE TABLE sales_forecast_reopen_window (
    id             INT          NOT NULL AUTO_INCREMENT,
    month          VARCHAR(7)   NOT NULL,
    channel        VARCHAR(50)  NOT NULL,
    expires_at     DATETIME     NOT NULL,
    reason         VARCHAR(200) NOT NULL,
    opened_by      BIGINT       NOT NULL,
    opened_at      DATETIME     NOT NULL,
    ended_at       DATETIME     NULL,
    ended_by       BIGINT       NULL,
    merged_version VARCHAR(20)  NULL,
    PRIMARY KEY (id),
    KEY idx_reopen_window_month_channel (month, channel, ended_at),
    KEY idx_reopen_window_expires (ended_at, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sales_forecast_form_log (
    id          INT          NOT NULL AUTO_INCREMENT,
    month       VARCHAR(7)   NOT NULL,
    channel     VARCHAR(50)  NULL,
    action      VARCHAR(30)  NOT NULL,
    operator_id BIGINT       NULL,
    detail      VARCHAR(500) NULL,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_form_log_month (month, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO permissions (code, name, module, description) VALUES
('sales_forecast_config.reopen', '開放補登', 'sales_forecast_config', '已結束新增的月份，對指定通路開放補登');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('production_planner', 'admin') AND p.code = 'sales_forecast_config.reopen';
