CREATE TABLE erp_purchase_order_detail (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    week_start DATE          NOT NULL,
    factory    VARCHAR(50)   NOT NULL,
    po_no      VARCHAR(100)  NOT NULL,
    itm        INT           NOT NULL COMMENT '行項目序號',
    prd_no     VARCHAR(100)  NOT NULL COMMENT '品號',
    prd_name   VARCHAR(200)  NOT NULL COMMENT '品名',
    wh         VARCHAR(50)   NULL     COMMENT '倉庫代碼',
    qty        DECIMAL(10,2) NOT NULL COMMENT '採購數量',
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_erp_pod_week_factory (week_start, factory)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
