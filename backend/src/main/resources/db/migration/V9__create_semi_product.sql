-- V9: Create semi_product_advance_purchase table
-- Stores semi-product advance purchase configuration data
-- Contains product codes, names, and the number of days in advance that materials need to be purchased
-- This data is essential for calculating material demand dates in production planning workflow

CREATE TABLE semi_product_advance_purchase (
    id                INT             NOT NULL AUTO_INCREMENT,
    product_code      VARCHAR(50)     NOT NULL,
    product_name      VARCHAR(200)    NOT NULL,
    advance_days      INT             NOT NULL,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- Unique constraint on product_code to prevent duplicate entries
    UNIQUE KEY uk_semi_product_code (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
