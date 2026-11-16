-- V36: 自動結束新增設定由日期改為日期＋時間（台灣時間），舊資料以當天 00:00 回填

ALTER TABLE sales_forecast_config
    ADD COLUMN auto_close_at DATETIME NOT NULL DEFAULT '2099-12-31 00:00:00' AFTER month;

UPDATE sales_forecast_config SET auto_close_at = TIMESTAMP(auto_close_date);

DROP INDEX idx_sales_forecast_config_auto_close_date ON sales_forecast_config;

ALTER TABLE sales_forecast_config DROP COLUMN auto_close_date;

CREATE INDEX idx_sales_forecast_config_auto_close_at ON sales_forecast_config (auto_close_at);
