-- 物料需求待確認流程：0 待審核、1 審核完成（可送天心 ERP）、2 退回

ALTER TABLE material_demand_pending_confirm
    ADD COLUMN status TINYINT NOT NULL DEFAULT 0
        COMMENT '0待審核 1審核完成 2退回'
        AFTER factory;
