-- V11: Create material_purchase table
-- Stores weekly material purchase planning data per factory
-- Contains product quantities, semi-product information, BOM calculations (kg per box, basket quantity, barrels required), and ERP integration status
-- Supports material purchase workflow and ERP order triggering

CREATE TABLE material_purchase (
    id                    INT             NOT NULL AUTO_INCREMENT,
    week_start            DATE            NOT NULL,
    factory               VARCHAR(50)     NOT NULL,
    product_code          VARCHAR(50)     NOT NULL,
    product_name          VARCHAR(200)    NOT NULL,
    quantity              DECIMAL(10,2)   NOT NULL,
    semi_product_name     VARCHAR(200)    NOT NULL,
    semi_product_code     VARCHAR(100)    NOT NULL,
    kg_per_box            DECIMAL(10,2)   NOT NULL DEFAULT 0,
    basket_quantity       DECIMAL(10,2)   NOT NULL DEFAULT 0,
    boxes_per_barrel      DECIMAL(10,2)   NOT NULL DEFAULT 0,
    required_barrels      DECIMAL(10,2)   NOT NULL DEFAULT 0,
    is_erp_triggered      BOOLEAN         NOT NULL DEFAULT FALSE,
    erp_order_no          VARCHAR(100)    NULL,
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- Composite index for week+factory queries (primary query pattern for weekly purchase retrieval)
    INDEX idx_material_purchase_week_factory (week_start, factory),
    -- Index for product-based queries (e.g., purchase history for specific products)
    INDEX idx_material_purchase_product (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
