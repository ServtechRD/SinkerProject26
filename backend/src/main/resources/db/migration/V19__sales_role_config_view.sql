-- V19: 業務角色可檢視預測設定（銷售預估量-共同編輯界面需月份列表）
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'sales' AND p.code = 'sales_forecast_config.view';
