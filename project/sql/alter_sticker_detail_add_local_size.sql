-- ============================================================
-- 贴纸打印申请单明细表 - 新增品牌ID + 本地尺码相关字段
-- 说明：
--   brand_id          品牌ID(原表只有 brand_name,补齐 ID 与 kind_id 对齐)
--   local_group_id    本地尺码组ID
--   local_group_name  本地尺码组名称快照
--   local_size_id     本地尺码ID(精确匹配,容错改名)
--   local_size_name   本地尺码名称
-- ============================================================

ALTER TABLE sticker_print_order_detail ADD brand_id          VARCHAR2(50);
ALTER TABLE sticker_print_order_detail ADD local_group_id    VARCHAR2(32);
ALTER TABLE sticker_print_order_detail ADD local_group_name  VARCHAR2(100);
ALTER TABLE sticker_print_order_detail ADD local_size_id     VARCHAR2(32);
ALTER TABLE sticker_print_order_detail ADD local_size_name   VARCHAR2(50);

COMMENT ON COLUMN sticker_print_order_detail.brand_id         IS '品牌ID(关联 ERP M_DIM.ID, DIM1)';
COMMENT ON COLUMN sticker_print_order_detail.local_group_id   IS '本地尺码组ID';
COMMENT ON COLUMN sticker_print_order_detail.local_group_name IS '本地尺码组名称快照';
COMMENT ON COLUMN sticker_print_order_detail.local_size_id    IS '本地尺码ID';
COMMENT ON COLUMN sticker_print_order_detail.local_size_name  IS '本地尺码名称';
