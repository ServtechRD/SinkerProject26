-- V24: 總體銷售預估量表單版本（以月份關帳為第一版，後續為編輯儲存版）

CREATE TABLE sales_forecast_form_version (
    id            INT             NOT NULL AUTO_INCREMENT,
    month         VARCHAR(7)      NOT NULL,
    version_no    INT             NOT NULL,
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_reason TEXT            NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_form_version_month_no (month, version_no),
    INDEX idx_form_version_month (month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE sales_forecast
    ADD COLUMN form_version_no INT NULL DEFAULT NULL AFTER version,
    ADD INDEX idx_sales_forecast_form_version (month, form_version_no);
