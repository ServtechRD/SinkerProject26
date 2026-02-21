-- V10: Create material_demand table
-- Stores material demand data calculated from production schedules via PDCA integration
-- Contains weekly material requirements per factory including demand dates, quantities, expected deliveries, and inventory estimates
-- Data is populated by PDCA integration (T032) and used for material purchase planning

CREATE TABLE material_demand (
    id                    INT             NOT NULL AUTO_INCREMENT,
    week_start            DATE            NOT NULL,
    factory               VARCHAR(50)     NOT NULL,
    material_code         VARCHAR(50)     NOT NULL,
    material_name         VARCHAR(200)    NOT NULL,
    unit                  VARCHAR(20)     NOT NULL,
    last_purchase_date    DATE            NULL,
    demand_date           DATE            NOT NULL,
    expected_delivery     DECIMAL(10,2)   NOT NULL DEFAULT 0,
    demand_quantity       DECIMAL(10,2)   NOT NULL DEFAULT 0,
    estimated_inventory   DECIMAL(10,2)   NOT NULL DEFAULT 0,
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- Composite index for week+factory queries (primary query pattern for material demand retrieval)
    INDEX idx_material_demand_week_factory (week_start, factory),
    -- Index for material-based queries (e.g., material demand across weeks)
    INDEX idx_material_demand_code (material_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
