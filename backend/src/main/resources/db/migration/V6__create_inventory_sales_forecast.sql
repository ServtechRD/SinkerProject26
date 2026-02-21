-- V6: Create inventory_sales_forecast table
-- Stores combined inventory, sales, and forecast data for production planning

CREATE TABLE inventory_sales_forecast (
    id                    INT             NOT NULL AUTO_INCREMENT,
    month                 VARCHAR(7)      NOT NULL,
    product_code          VARCHAR(50)     NOT NULL,
    product_name          VARCHAR(200)    NOT NULL,
    category              VARCHAR(100)    NOT NULL,
    spec                  VARCHAR(200)    NOT NULL,
    warehouse_location    VARCHAR(50)     NOT NULL,
    sales_quantity        DECIMAL(10,2)   NOT NULL DEFAULT 0,
    inventory_balance     DECIMAL(10,2)   NOT NULL DEFAULT 0,
    forecast_quantity     DECIMAL(10,2)   NOT NULL DEFAULT 0,
    production_subtotal   DECIMAL(10,2)   NOT NULL DEFAULT 0,
    modified_subtotal     DECIMAL(10,2)   NULL,
    version               VARCHAR(100)    NOT NULL,
    query_start_date      VARCHAR(10)     NOT NULL,
    query_end_date        VARCHAR(10)     NOT NULL,
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- Composite index for month+product queries (primary query pattern)
    INDEX idx_inventory_forecast_month_product (month, product_code),
    -- Index for version-based queries
    INDEX idx_inventory_forecast_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
