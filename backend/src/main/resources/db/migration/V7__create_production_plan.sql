-- V7: Create production_plan table
-- Stores annual production plans organized by product, channel, and month
-- Supports monthly allocation data (Feb-Dec) stored as JSON for flexible planning

CREATE TABLE production_plan (
    id                    INT             NOT NULL AUTO_INCREMENT,
    year                  INT             NOT NULL,
    product_code          VARCHAR(50)     NOT NULL,
    product_name          VARCHAR(200)    NOT NULL,
    category              VARCHAR(100)    NOT NULL,
    spec                  VARCHAR(200)    NOT NULL,
    warehouse_location    VARCHAR(50)     NOT NULL,
    channel               VARCHAR(50)     NOT NULL,
    monthly_allocation    JSON            NOT NULL DEFAULT ('{}'),
    buffer_quantity       DECIMAL(10,2)   NOT NULL DEFAULT 0,
    total_quantity        DECIMAL(10,2)   NOT NULL DEFAULT 0,
    original_forecast     DECIMAL(10,2)   NOT NULL DEFAULT 0,
    difference            DECIMAL(10,2)   NOT NULL DEFAULT 0,
    remarks               TEXT            NULL,
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- Composite index for year+product queries (primary query pattern)
    INDEX idx_production_plan_year_product (year, product_code),
    -- Unique constraint to prevent duplicate channel entries for same year/product
    UNIQUE KEY uk_production_plan_year_product_channel (year, product_code, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
