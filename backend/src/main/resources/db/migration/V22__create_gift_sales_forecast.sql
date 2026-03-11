-- V22: 禮品銷售預估量 - 表結構與銷售預估量表單相同

CREATE TABLE gift_sales_forecast (
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

CREATE TABLE gift_sales_forecast_version_reason (
    id          INT             NOT NULL AUTO_INCREMENT,
    month       VARCHAR(7)      NOT NULL,
    channel     VARCHAR(50)     NOT NULL,
    version     VARCHAR(100)    NOT NULL,
    change_reason TEXT          NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_gift_sales_forecast_version_reason (month, channel, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
