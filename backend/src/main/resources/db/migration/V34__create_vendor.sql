-- V34: Create vendor table for ERP vendor master data sync (廠商資料同步)

CREATE TABLE vendor (
    id            INT             NOT NULL AUTO_INCREMENT,
    code          VARCHAR(50)     NOT NULL,
    name          VARCHAR(500)    NOT NULL,
    sys_date      DATETIME        NULL,
    modify_date   DATETIME        NULL,
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vendor_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
