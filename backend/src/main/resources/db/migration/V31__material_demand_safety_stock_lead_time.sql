-- V31: material_demand 新增安全庫存量、標準交期（由 PDCA API 回傳）
ALTER TABLE material_demand
    ADD COLUMN safety_stock DECIMAL(10,2) NULL DEFAULT NULL AFTER current_stock,
    ADD COLUMN standard_lead_time INT NULL DEFAULT NULL AFTER safety_stock;
