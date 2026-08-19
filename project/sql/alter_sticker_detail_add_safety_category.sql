-- ============================================================
-- 贴纸打印申请单明细表 - 新增安全类别字段
-- 来源: ERP M_PRODUCT.SAFETY_CATEGORY (VARCHAR2(2000))
-- 快照语义: 选货时从 ERP 带入,之后 ERP 修改不自动同步
-- ============================================================

ALTER TABLE sticker_print_order_detail ADD safety_category VARCHAR2(2000);

COMMENT ON COLUMN sticker_print_order_detail.safety_category IS '安全类别(ERP M_PRODUCT.SAFETY_CATEGORY 快照)';
