-- V15: Add material_demand.upload and material_demand.edit permissions

INSERT INTO permissions (code, name, module, description) VALUES
('material_demand.upload', 'Upload Material Demand', 'material_demand', 'Upload material demand by week and factory'),
('material_demand.edit', 'Edit Material Demand', 'material_demand', 'Edit material demand quantities');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'admin' AND p.code IN ('material_demand.upload', 'material_demand.edit');
