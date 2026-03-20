-- V23: material_demand 新增欄位（現有庫存、預計進廠日、採購量）、待確認表、權限 confirm_data_send_erp、角色 採購主管

-- 1. material_demand 新增欄位：需求日之後 現有庫存、預計進廠日；預計庫存量之後 採購量
-- 欄位順序：... demand_date, current_stock, expected_arrival_date, expected_delivery, demand_quantity, estimated_inventory, purchase_quantity
ALTER TABLE material_demand
    ADD COLUMN current_stock DECIMAL(10,2) NULL DEFAULT NULL AFTER demand_date,
    ADD COLUMN expected_arrival_date DATE NULL DEFAULT NULL AFTER current_stock,
    ADD COLUMN purchase_quantity DECIMAL(10,2) NULL DEFAULT NULL AFTER estimated_inventory;

-- 2. 待確認送出 ERP 記錄表（有編輯儲存則寫入，送出天心 ERP 後刪除）
CREATE TABLE material_demand_pending_confirm (
    week_start DATE NOT NULL,
    factory VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (week_start, factory)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 權限：物料需求群組新增 confirm_data_send_erp
INSERT INTO permissions (code, name, module, description) VALUES
('confirm_data_send_erp', 'Confirm Data Send ERP', 'material_demand', 'Confirm material demand and send to Tien-Sin ERP');

-- 4. 角色：採購主管（權限同採購 + confirm_data_send_erp）
INSERT INTO roles (code, name, description, is_system, is_active) VALUES
('procurement_supervisor', '採購主管', '週排程、半成品設定、物料需求、物料採購，並可確認送出天心ERP', FALSE, TRUE);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'procurement_supervisor' AND p.module IN ('semi_product', 'material_demand', 'material_purchase');

-- 5. admin 也擁有 confirm_data_send_erp
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'admin' AND p.code = 'confirm_data_send_erp';

-- 6. 預設採購主管帳號：pursup / pursup123 (bcrypt cost 10)
INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count)
SELECT 'pursup', 'pursup@sinker.local', '$2b$10$WLPaHJfSQw9KWrB49foJIu8aInpf7XP9Tf4w3Aoae.I4UcyuvIo/.', '採購主管',
       (SELECT id FROM roles WHERE code = 'procurement_supervisor'), TRUE, FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.username = 'pursup');

UPDATE users u
INNER JOIN roles r ON r.code = 'procurement_supervisor'
SET u.hashed_password = '$2b$10$WLPaHJfSQw9KWrB49foJIu8aInpf7XP9Tf4w3Aoae.I4UcyuvIo/.',
    u.full_name = '採購主管',
    u.role_id = r.id
WHERE u.username = 'pursup';
