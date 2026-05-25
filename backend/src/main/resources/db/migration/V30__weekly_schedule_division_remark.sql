-- V30: Separate 區分 (division) from 庫位 (warehouse_location); store 備註 (remark)

ALTER TABLE production_weekly_schedule
    ADD COLUMN division VARCHAR(50) NULL AFTER product_name,
    ADD COLUMN remark TEXT NULL AFTER quantity;

ALTER TABLE production_weekly_schedule
    MODIFY COLUMN warehouse_location VARCHAR(50) NULL;
