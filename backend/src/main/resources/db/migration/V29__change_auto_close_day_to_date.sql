-- V29: Replace auto_close_day (INT) with auto_close_date (DATE)

ALTER TABLE sales_forecast_config
    DROP CONSTRAINT chk_auto_close_day,
    DROP INDEX idx_sales_forecast_config_auto_close_day,
    ADD COLUMN auto_close_date DATE NOT NULL DEFAULT '2099-12-31' AFTER month,
    DROP COLUMN auto_close_day;

CREATE INDEX idx_sales_forecast_config_auto_close_date ON sales_forecast_config (auto_close_date);
