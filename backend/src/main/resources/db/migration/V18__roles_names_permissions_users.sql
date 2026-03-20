-- V18: Role names to Chinese, role permissions by role, seed users sales/pm/pur

-- 1. 角色名稱改為中文
UPDATE roles SET name = '系統管理員', description = '全部權限' WHERE code = 'admin';
UPDATE roles SET name = '業務', description = '銷售預測（不含關帳後編輯）' WHERE code = 'sales';
UPDATE roles SET name = '生管', description = '銷售預測、預測設定、生產表單、庫存銷量預估量整合表單' WHERE code = 'production_planner';
UPDATE roles SET name = '採購', description = '週排程、半成品設定、物料需求、物料採購' WHERE code = 'procurement';

-- 2. 業務 (sales): 銷售預測全部權限（不含 Update After Closed）+ 預測設定「檢視」以利共同編輯界面選月份
DELETE rp FROM role_permissions rp INNER JOIN roles r ON rp.role_id = r.id WHERE r.code = 'sales';
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'sales' AND p.code IN (
  'sales_forecast.view', 'sales_forecast.view_own', 'sales_forecast.upload',
  'sales_forecast.create', 'sales_forecast.edit', 'sales_forecast.delete',
  'sales_forecast_config.view'
);

-- 3. 生管 (production_planner): 銷售預設僅保留 sales_forecast.update_after_closed，並加入週排程；保留其他既有模組權限
DELETE rp FROM role_permissions rp INNER JOIN roles r ON rp.role_id = r.id WHERE r.code = 'production_planner';
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'production_planner' AND (
  p.module IN ('sales_forecast_config', 'production_plan', 'inventory', 'weekly_schedule')
  OR p.code = 'sales_forecast.update_after_closed'
);

-- 4. 採購 (procurement): 去除週排程相關權限，保留半成品設定、物料需求、物料採購
DELETE rp FROM role_permissions rp INNER JOIN roles r ON rp.role_id = r.id WHERE r.code = 'procurement';
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'procurement' AND p.module IN ('semi_product', 'material_demand', 'material_purchase');

-- 5. 使用者帳號 (若已存在則不重複插入)
-- 密碼: sales123, pm123, pur123 (bcrypt cost 10)
INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count)
SELECT 'sales', 'sales@sinker.local', '$2b$10$CeUITJUBjATZzx/ABbsbaOyua0raWA51O8u4EwBv9IpC3F9p0jctC', '業務', r.id, TRUE, FALSE, 0
FROM roles r
WHERE r.code = 'sales' AND NOT EXISTS (SELECT 1 FROM users u WHERE u.username = 'sales');

INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count)
SELECT 'pm', 'pm@sinker.local', '$2b$10$hMm3ssEM/L741ZBaKRTwBub9AgkMdDJqkey3sdN3.gfWmE5A/.UKq', '生管', r.id, TRUE, FALSE, 0
FROM roles r
WHERE r.code = 'production_planner' AND NOT EXISTS (SELECT 1 FROM users u WHERE u.username = 'pm');

INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count)
SELECT 'pur', 'pur@sinker.local', '$2b$10$HEbkh7A3P7WZ2cTnvjaGzuqRS17nQdezSTsB5nXZA0XIFK4nQ/dqK', '採購', r.id, TRUE, FALSE, 0
FROM roles r
WHERE r.code = 'procurement' AND NOT EXISTS (SELECT 1 FROM users u WHERE u.username = 'pur');

-- 若使用者已存在，更新密碼與角色以符合本 migration
UPDATE users u INNER JOIN roles r ON r.code = 'sales' SET u.hashed_password = '$2b$10$CeUITJUBjATZzx/ABbsbaOyua0raWA51O8u4EwBv9IpC3F9p0jctC', u.full_name = '業務', u.role_id = r.id WHERE u.username = 'sales';
UPDATE users u INNER JOIN roles r ON r.code = 'production_planner' SET u.hashed_password = '$2b$10$hMm3ssEM/L741ZBaKRTwBub9AgkMdDJqkey3sdN3.gfWmE5A/.UKq', u.full_name = '生管', u.role_id = r.id WHERE u.username = 'pm';
UPDATE users u INNER JOIN roles r ON r.code = 'procurement' SET u.hashed_password = '$2b$10$HEbkh7A3P7WZ2cTnvjaGzuqRS17nQdezSTsB5nXZA0XIFK4nQ/dqK', u.full_name = '採購', u.role_id = r.id WHERE u.username = 'pur';
