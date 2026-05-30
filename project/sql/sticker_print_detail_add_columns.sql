-- 贴纸打印申请单明细表 - 字段对齐ERP命名 (Oracle)

-- 1. 重命名已有列，对齐ERP字段名
ALTER TABLE sticker_print_order_detail RENAME COLUMN article_no TO style_number;
ALTER TABLE sticker_print_order_detail RENAME COLUMN article_name TO material_name;

-- 2. 添加缺失列
ALTER TABLE sticker_print_order_detail ADD (material_number VARCHAR2(100));
ALTER TABLE sticker_print_order_detail ADD (color VARCHAR2(50));
ALTER TABLE sticker_print_order_detail ADD (execution_standard VARCHAR2(200));

-- 3. 添加列注释
COMMENT ON COLUMN sticker_print_order_detail.style_number IS '款号';
COMMENT ON COLUMN sticker_print_order_detail.material_name IS '货品名称';
COMMENT ON COLUMN sticker_print_order_detail.material_number IS '货号';
COMMENT ON COLUMN sticker_print_order_detail.color IS '颜色';
COMMENT ON COLUMN sticker_print_order_detail.execution_standard IS '执行标准';
