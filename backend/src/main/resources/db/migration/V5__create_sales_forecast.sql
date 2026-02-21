-- V5: Create sales_forecast table

CREATE TABLE sales_forecast (
    id                  INT             NOT NULL AUTO_INCREMENT,
    month               VARCHAR(7)      NOT NULL,
    channel             VARCHAR(50)     NOT NULL,
    category            VARCHAR(100)    NULL,
    spec                VARCHAR(200)    NULL,
    product_code        VARCHAR(50)     NOT NULL,
    product_name        VARCHAR(200)    NULL,
    warehouse_location  VARCHAR(50)     NULL,
    quantity            DECIMAL(10,2)   NOT NULL,
    version             VARCHAR(100)    NOT NULL,
    is_modified         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_month_channel (month, channel),
    INDEX idx_product_code (product_code),
    INDEX idx_version (version),
    INDEX idx_month_channel_product (month, channel, product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
