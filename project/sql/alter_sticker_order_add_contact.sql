-- ============================================================
-- 贴纸打印申请单主表 - 新增联系人/联系电话/收货地址
-- 用途: 下发打印时生成"首张"标签(第一张打这三个信息,第二张起打贴纸)
-- 三者全空时不生成首张任务
-- ============================================================

ALTER TABLE sticker_print_order ADD contact_person    VARCHAR2(50);
ALTER TABLE sticker_print_order ADD contact_phone     VARCHAR2(50);
ALTER TABLE sticker_print_order ADD delivery_address  VARCHAR2(500);

COMMENT ON COLUMN sticker_print_order.contact_person   IS '联系人(首张打印)';
COMMENT ON COLUMN sticker_print_order.contact_phone    IS '联系电话(首张打印)';
COMMENT ON COLUMN sticker_print_order.delivery_address IS '收货地址(首张打印)';
