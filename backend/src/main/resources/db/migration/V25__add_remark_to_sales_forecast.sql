-- V25: Add per-row remark to sales_forecast (form summary row 備註, independent from version change_reason)

ALTER TABLE sales_forecast
    ADD COLUMN remark TEXT NULL AFTER quantity;
