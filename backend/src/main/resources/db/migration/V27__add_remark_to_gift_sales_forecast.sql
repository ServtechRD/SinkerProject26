-- V27: Add per-row remark to gift_sales_forecast

ALTER TABLE gift_sales_forecast
    ADD COLUMN remark TEXT DEFAULT NULL AFTER quantity;
