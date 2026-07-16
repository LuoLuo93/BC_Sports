-- ============================================================
-- 贴纸打印明细表增加 product_id 和 barcode 列
-- 数据库: BC_ERP_MANAGE (Oracle 192.168.104.167)
-- 表:   sticker_print_order_detail
-- 日期: 2026-07-16
-- 说明: 货品搜索返回 PRODUCT_ID(伯俊商品ID)，选尺码时按 productId
--       查 M_PRODUCT_ALIAS 拿到每个尺码对应的条码(barcode)，
--       保存明细时需将这两个字段一起入库。
-- ============================================================

ALTER TABLE sticker_print_order_detail ADD (product_id VARCHAR2(64));
ALTER TABLE sticker_print_order_detail ADD (barcode VARCHAR2(100));

-- 验证
SELECT column_name, data_type, data_length
FROM user_tab_columns
WHERE table_name = 'STICKER_PRINT_ORDER_DETAIL'
  AND column_name IN ('PRODUCT_ID', 'BARCODE');
