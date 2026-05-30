-- ============================================================
-- 贴纸打印申请单 - Oracle 建表脚本
-- ============================================================

-- 1. 主表
CREATE TABLE sticker_print_order (
    id              VARCHAR2(32)  NOT NULL,
    order_no        VARCHAR2(30)  NOT NULL,
    status          NUMBER(1)     DEFAULT 0 NOT NULL,
    applicant       VARCHAR2(50),
    reviewer        VARCHAR2(50),
    review_time     TIMESTAMP,
    review_remark   VARCHAR2(500),
    remark          VARCHAR2(500),
    create_time     TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    update_time     TIMESTAMP,
    create_by       VARCHAR2(50),
    update_by       VARCHAR2(50),
    deleted         NUMBER(1)     DEFAULT 0 NOT NULL,
    print_time      TIMESTAMP,
    print_by        VARCHAR2(50),
    CONSTRAINT pk_sticker_print_order PRIMARY KEY (id)
);

COMMENT ON TABLE  sticker_print_order IS '贴纸打印申请单';
COMMENT ON COLUMN sticker_print_order.id             IS '主键';
COMMENT ON COLUMN sticker_print_order.order_no        IS '申请单号';
COMMENT ON COLUMN sticker_print_order.status          IS '状态 0草稿 1待审核 2已审核 3已驳回';
COMMENT ON COLUMN sticker_print_order.applicant       IS '申请人';
COMMENT ON COLUMN sticker_print_order.reviewer        IS '审核人';
COMMENT ON COLUMN sticker_print_order.review_time     IS '审核时间';
COMMENT ON COLUMN sticker_print_order.review_remark   IS '审核意见';
COMMENT ON COLUMN sticker_print_order.remark          IS '备注';
COMMENT ON COLUMN sticker_print_order.create_time     IS '创建时间';
COMMENT ON COLUMN sticker_print_order.update_time     IS '更新时间';
COMMENT ON COLUMN sticker_print_order.create_by       IS '创建人';
COMMENT ON COLUMN sticker_print_order.update_by       IS '更新人';
COMMENT ON COLUMN sticker_print_order.deleted         IS '是否删除 0否 1是';
COMMENT ON COLUMN sticker_print_order.print_time      IS '打印时间';
COMMENT ON COLUMN sticker_print_order.print_by        IS '打印人';

-- 2. 明细表
CREATE TABLE sticker_print_order_detail (
    id                  VARCHAR2(32)    NOT NULL,
    order_id            VARCHAR2(32)    NOT NULL,
    material_number     VARCHAR2(100),
    material_name       VARCHAR2(200),
    style_number        VARCHAR2(100),
    color               VARCHAR2(50),
    execution_standard  VARCHAR2(200),
    size_code           VARCHAR2(50),
    size_name           VARCHAR2(50),
    size_group          VARCHAR2(200),
    ean13               VARCHAR2(20),
    brand_name          VARCHAR2(100),
    price               NUMBER(12,5),
    print_qty           NUMBER(6)       DEFAULT 0 NOT NULL,
    sort                NUMBER(6)       DEFAULT 0 NOT NULL,
    create_time         TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_sticker_print_order_detail PRIMARY KEY (id)
);

COMMENT ON TABLE  sticker_print_order_detail IS '贴纸打印申请单明细';
COMMENT ON COLUMN sticker_print_order_detail.id                 IS '主键';
COMMENT ON COLUMN sticker_print_order_detail.order_id           IS '申请单ID';
COMMENT ON COLUMN sticker_print_order_detail.material_number    IS '货号';
COMMENT ON COLUMN sticker_print_order_detail.material_name      IS '货品名称';
COMMENT ON COLUMN sticker_print_order_detail.style_number       IS '款号';
COMMENT ON COLUMN sticker_print_order_detail.color              IS '颜色';
COMMENT ON COLUMN sticker_print_order_detail.execution_standard IS '执行标准';
COMMENT ON COLUMN sticker_print_order_detail.size_code          IS '尺码编码';
COMMENT ON COLUMN sticker_print_order_detail.size_name          IS '尺码名称';
COMMENT ON COLUMN sticker_print_order_detail.size_group         IS '尺码组';
COMMENT ON COLUMN sticker_print_order_detail.ean13              IS 'EAN13条码';
COMMENT ON COLUMN sticker_print_order_detail.brand_name         IS '品牌名称';
COMMENT ON COLUMN sticker_print_order_detail.price              IS '价格';
COMMENT ON COLUMN sticker_print_order_detail.print_qty          IS '打印数量';
COMMENT ON COLUMN sticker_print_order_detail.sort               IS '排序';
COMMENT ON COLUMN sticker_print_order_detail.create_time        IS '创建时间';

-- 索引
CREATE INDEX idx_sticker_print_order_no   ON sticker_print_order (order_no);
CREATE INDEX idx_sticker_print_order_stat ON sticker_print_order (status, deleted);
CREATE INDEX idx_sticker_print_detail_oid ON sticker_print_order_detail (order_id);

-- 贴纸货品资料直接查询伯俊 ERP M_PRODUCT 表，无需本地建表
COMMENT ON COLUMN sticker_product_data.update_time        IS '更新时间';

CREATE UNIQUE INDEX uk_sticker_product_mn ON sticker_product_data (material_number);
