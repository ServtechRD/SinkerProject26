-- V16: Add material_purchase.upload and material_purchase.edit permissions

INSERT INTO permissions (code, name, module, description) VALUES
('material_purchase.upload', 'Upload Material Purchase', 'material_purchase', 'Upload material purchase by week and factory'),
('material_purchase.edit', 'Edit Material Purchase', 'material_purchase', 'Edit material purchase quantities');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'admin' AND p.code IN ('material_purchase.upload', 'material_purchase.edit');
