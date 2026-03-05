-- V17: Production form buffer/remarks per year+product
CREATE TABLE production_form (
    id                INT             NOT NULL AUTO_INCREMENT,
    year              INT             NOT NULL,
    product_code      VARCHAR(50)     NOT NULL,
    buffer_quantity   DECIMAL(10,2)   NOT NULL DEFAULT 0,
    remarks           TEXT            NULL,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_production_form_year_product (year, product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
