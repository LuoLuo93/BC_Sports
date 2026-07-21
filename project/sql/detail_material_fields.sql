-- 打印申请单明细：材质字段拆分（ERP真实字段接入）
-- 原 material_composition(材质成分) 占位常量，现拆为4个独立字段对接 ERP FABCODE/FABELEMENT/ACCODE/ACCELEMENT
-- 执行前确认无在途打印任务

-- 1. 新增4列
ALTER TABLE sticker_print_order_detail ADD (
    fab_code      VARCHAR2(100),
    fab_element   VARCHAR2(200),
    ac_code       VARCHAR2(100),
    acc_element   VARCHAR2(200)
);
COMMENT ON COLUMN sticker_print_order_detail.fab_code    IS '面料编码(ERP FABCODE)';
COMMENT ON COLUMN sticker_print_order_detail.fab_element IS '面料成分(ERP FABELEMENT)';
COMMENT ON COLUMN sticker_print_order_detail.ac_code     IS '辅料编码(ERP ACCODE)';
COMMENT ON COLUMN sticker_print_order_detail.acc_element IS '辅料成分(ERP ACCELEMENT)';

-- 2. 删除旧的材质成分占位列
ALTER TABLE sticker_print_order_detail DROP COLUMN material_composition;
