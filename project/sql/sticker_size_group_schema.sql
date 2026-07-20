-- ============================================================
-- 贴纸本地尺码组 - Oracle 建表脚本
-- 说明：部分商品尺码不走伯俊 ERP，在本地维护尺码组(按品牌+类别隔离)
--       一个品牌+类别下可有多个尺码组(一对多)，一个尺码组下有多个尺码明细
-- ============================================================

-- 1. 尺码组主表
CREATE TABLE sticker_size_group (
    id              VARCHAR2(32)  NOT NULL,
    group_code      VARCHAR2(50)  NOT NULL,
    group_name      VARCHAR2(100) NOT NULL,
    brand_id        VARCHAR2(50),
    brand_name      VARCHAR2(100),
    kind_id         VARCHAR2(50),
    kind_name       VARCHAR2(100),
    status          NUMBER(1)     DEFAULT 1 NOT NULL,
    sort            NUMBER(6)     DEFAULT 0 NOT NULL,
    remark          VARCHAR2(500),
    create_time     TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    update_time     TIMESTAMP,
    create_by       VARCHAR2(50),
    update_by       VARCHAR2(50),
    deleted         NUMBER(1)     DEFAULT 0 NOT NULL,
    CONSTRAINT pk_sticker_size_group PRIMARY KEY (id)
);

COMMENT ON TABLE  sticker_size_group IS '贴纸本地尺码组(按品牌+类别隔离)';
COMMENT ON COLUMN sticker_size_group.id          IS '主键';
COMMENT ON COLUMN sticker_size_group.group_code  IS '尺码组编码(同品牌+类别下唯一)';
COMMENT ON COLUMN sticker_size_group.group_name  IS '尺码组名称';
COMMENT ON COLUMN sticker_size_group.brand_id    IS '品牌ID(关联 ERP M_DIM.ID, DIM1)';
COMMENT ON COLUMN sticker_size_group.brand_name  IS '品牌名称快照';
COMMENT ON COLUMN sticker_size_group.kind_id     IS '类别ID(关联 ERP M_DIM.ID, DIM4)';
COMMENT ON COLUMN sticker_size_group.kind_name   IS '类别名称快照';
COMMENT ON COLUMN sticker_size_group.status      IS '状态 1启用 0停用';
COMMENT ON COLUMN sticker_size_group.sort        IS '排序';
COMMENT ON COLUMN sticker_size_group.remark      IS '备注';
COMMENT ON COLUMN sticker_size_group.create_time IS '创建时间';
COMMENT ON COLUMN sticker_size_group.update_time IS '更新时间';
COMMENT ON COLUMN sticker_size_group.create_by   IS '创建人';
COMMENT ON COLUMN sticker_size_group.update_by   IS '更新人';
COMMENT ON COLUMN sticker_size_group.deleted     IS '是否删除 0否 1是';

CREATE INDEX idx_sticker_size_group_brand ON sticker_size_group (brand_id, kind_id, deleted);
CREATE UNIQUE INDEX uk_sticker_size_group_code ON sticker_size_group (brand_id, kind_id, group_code, deleted);

-- 2. 尺码明细表
CREATE TABLE sticker_sizes (
    id              VARCHAR2(32)  NOT NULL,
    group_id        VARCHAR2(32)  NOT NULL,
    size_code       VARCHAR2(50),
    size_name       VARCHAR2(50)  NOT NULL,
    sort            NUMBER(6)     DEFAULT 0 NOT NULL,
    create_time     TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    deleted         NUMBER(1)     DEFAULT 0 NOT NULL,
    CONSTRAINT pk_sticker_sizes PRIMARY KEY (id)
);

COMMENT ON TABLE  sticker_sizes IS '贴纸本地尺码明细';
COMMENT ON COLUMN sticker_sizes.id          IS '主键';
COMMENT ON COLUMN sticker_sizes.group_id    IS '尺码组ID';
COMMENT ON COLUMN sticker_sizes.size_code   IS '尺码编码';
COMMENT ON COLUMN sticker_sizes.size_name   IS '尺码名称';
COMMENT ON COLUMN sticker_sizes.sort        IS '排序';
COMMENT ON COLUMN sticker_sizes.create_time IS '创建时间';
COMMENT ON COLUMN sticker_sizes.deleted     IS '是否删除 0否 1是';

CREATE INDEX idx_sticker_sizes_group ON sticker_sizes (group_id, deleted);
