-- V14: Add sales_forecast.update_after_closed permission and version change reason table

-- New permission for editing after month closed
INSERT INTO permissions (code, name, module, description) VALUES
('sales_forecast.update_after_closed', 'Update After Closed', 'sales_forecast', 'Edit/create forecast in closed month by copying to new version');

-- Table to store change reason per version (month + channel + version)
CREATE TABLE sales_forecast_version_reason (
    id          INT             NOT NULL AUTO_INCREMENT,
    month       VARCHAR(7)      NOT NULL,
    channel     VARCHAR(50)     NOT NULL,
    version     VARCHAR(100)    NOT NULL,
    change_reason TEXT          NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sales_forecast_version_reason (month, channel, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Grant new permission to admin role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'admin' AND p.code = 'sales_forecast.update_after_closed';
