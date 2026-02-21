-- V4: Create sales_forecast_config table

CREATE TABLE sales_forecast_config (
    id              INT           NOT NULL AUTO_INCREMENT,
    month           VARCHAR(7)    NOT NULL,
    auto_close_day  INT           NOT NULL DEFAULT 10,
    is_closed       BOOLEAN       NOT NULL DEFAULT FALSE,
    closed_at       TIMESTAMP     NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_sales_forecast_config_month (month),
    INDEX idx_sales_forecast_config_is_closed (is_closed),
    INDEX idx_sales_forecast_config_auto_close_day (auto_close_day),
    CONSTRAINT chk_auto_close_day CHECK (auto_close_day BETWEEN 1 AND 31)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
