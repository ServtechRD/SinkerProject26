-- V8: Create production_weekly_schedule table
-- Stores factory production schedules organized by week (starting Monday)
-- Contains demand dates and product quantities for weekly production planning
-- Each week_start+factory combination is replaced on upload (delete-insert pattern)

CREATE TABLE production_weekly_schedule (
    id                    INT             NOT NULL AUTO_INCREMENT,
    week_start            DATE            NOT NULL,
    factory               VARCHAR(50)     NOT NULL,
    demand_date           DATE            NOT NULL,
    product_code          VARCHAR(50)     NOT NULL,
    product_name          VARCHAR(200)    NOT NULL,
    warehouse_location    VARCHAR(50)     NOT NULL,
    quantity              DECIMAL(10,2)   NOT NULL DEFAULT 0,
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- Composite index for week+factory queries (primary query pattern for schedule retrieval)
    INDEX idx_weekly_schedule_week_factory (week_start, factory),
    -- Index for product-based queries (e.g., product demand across weeks)
    INDEX idx_weekly_schedule_product (product_code),
    -- Ensure week_start is always Monday (DAYOFWEEK: 1=Sunday, 2=Monday, ..., 7=Saturday)
    CONSTRAINT chk_weekly_schedule_monday CHECK (DAYOFWEEK(week_start) = 2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
