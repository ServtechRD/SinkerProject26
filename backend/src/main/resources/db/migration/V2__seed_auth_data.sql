-- V2: Seed roles, permissions, admin user, and role-permission mappings

-- Seed roles
INSERT INTO roles (code, name, description, is_system, is_active) VALUES
('admin',               'Administrator',        'Full system access',                          TRUE, TRUE),
('sales',               'Sales',                'Sales forecast management',                   TRUE, TRUE),
('production_planner',  'Production Planner',   'Production planning and scheduling',          TRUE, TRUE),
('procurement',         'Procurement',          'Material procurement and ERP integration',    TRUE, TRUE);

-- Seed permissions
-- Module: user
INSERT INTO permissions (code, name, module, description) VALUES
('user.view',   'View Users',   'user', 'View user list and details'),
('user.create', 'Create Users', 'user', 'Create new users'),
('user.edit',   'Edit Users',   'user', 'Edit existing users'),
('user.delete', 'Delete Users', 'user', 'Delete users');

-- Module: role
INSERT INTO permissions (code, name, module, description) VALUES
('role.view',   'View Roles',   'role', 'View role list and details'),
('role.create', 'Create Roles', 'role', 'Create new roles'),
('role.edit',   'Edit Roles',   'role', 'Edit existing roles'),
('role.delete', 'Delete Roles', 'role', 'Delete roles');

-- Module: sales_forecast
INSERT INTO permissions (code, name, module, description) VALUES
('sales_forecast.view',      'View All Forecasts',   'sales_forecast', 'View all channel forecasts'),
('sales_forecast.view_own',  'View Own Forecasts',   'sales_forecast', 'View own channel forecasts'),
('sales_forecast.upload',    'Upload Forecasts',     'sales_forecast', 'Upload Excel forecasts'),
('sales_forecast.create',    'Create Forecast Item', 'sales_forecast', 'Add single forecast item'),
('sales_forecast.edit',      'Edit Forecast Item',   'sales_forecast', 'Edit forecast item'),
('sales_forecast.delete',    'Delete Forecast Item', 'sales_forecast', 'Delete forecast item');

-- Module: sales_forecast_config
INSERT INTO permissions (code, name, module, description) VALUES
('sales_forecast_config.view', 'View Forecast Config', 'sales_forecast_config', 'View forecast month configuration'),
('sales_forecast_config.edit', 'Edit Forecast Config', 'sales_forecast_config', 'Edit forecast month configuration');

-- Module: production_plan
INSERT INTO permissions (code, name, module, description) VALUES
('production_plan.view', 'View Production Plan', 'production_plan', 'View annual production plan'),
('production_plan.edit', 'Edit Production Plan', 'production_plan', 'Edit annual production plan');

-- Module: inventory
INSERT INTO permissions (code, name, module, description) VALUES
('inventory.view', 'View Inventory Integration', 'inventory', 'View inventory/sales/forecast integration'),
('inventory.edit', 'Edit Inventory Integration', 'inventory', 'Edit modified subtotal');

-- Module: weekly_schedule
INSERT INTO permissions (code, name, module, description) VALUES
('weekly_schedule.view',   'View Weekly Schedule',   'weekly_schedule', 'View weekly production schedule'),
('weekly_schedule.upload', 'Upload Weekly Schedule', 'weekly_schedule', 'Upload weekly schedule Excel'),
('weekly_schedule.edit',   'Edit Weekly Schedule',   'weekly_schedule', 'Edit weekly schedule items');

-- Module: semi_product
INSERT INTO permissions (code, name, module, description) VALUES
('semi_product.view',   'View Semi-Product Settings',   'semi_product', 'View semi-product advance purchase settings'),
('semi_product.upload', 'Upload Semi-Product Settings', 'semi_product', 'Upload semi-product settings Excel'),
('semi_product.edit',   'Edit Semi-Product Settings',   'semi_product', 'Edit semi-product advance days');

-- Module: material_demand
INSERT INTO permissions (code, name, module, description) VALUES
('material_demand.view', 'View Material Demand', 'material_demand', 'View material demand requirements');

-- Module: material_purchase
INSERT INTO permissions (code, name, module, description) VALUES
('material_purchase.view',        'View Material Purchase',    'material_purchase', 'View material purchase list'),
('material_purchase.trigger_erp', 'Trigger ERP Purchase',      'material_purchase', 'Trigger ERP purchase order');

-- Seed admin user (password: admin123, bcrypt cost 10)
INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count)
VALUES ('admin', 'admin@sinker.local', '$2b$10$QaOvaUqnaUwacjBwj1SP5eF/UohQ/xC6WkTbWXYh1oRXRDLSg8tHS', 'System Administrator',
        (SELECT id FROM roles WHERE code = 'admin'), TRUE, FALSE, 0);

-- Map ALL permissions to admin role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'admin';
