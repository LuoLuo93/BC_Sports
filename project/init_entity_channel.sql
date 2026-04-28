-- ========================================================
-- 实体渠道配置表初始化脚本
-- 表名: bc_sports_sys_entity_channel
-- 功能: 管理店铺、仓库、客户的渠道属性配置
-- 创建时间: 2026-04-15
-- ========================================================

-- 0. 创建序列
CREATE SEQUENCE SEQ_BC_SPORTS_SYS_ENTITY_CHANNEL START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- 1. 创建实体渠道配置表
CREATE TABLE bc_sports_sys_entity_channel (
    id                  VARCHAR2(32) PRIMARY KEY,
    external_id         VARCHAR2(32) NOT NULL,
    brand_id            VARCHAR2(64),
    entity_type         VARCHAR2(20) NOT NULL,
    entity_name         VARCHAR2(100),
    channel_type_id     VARCHAR2(32),
    channel_def_id      VARCHAR2(32),
    channel_nature_id   VARCHAR2(32),
    business_type_id    VARCHAR2(32),
    region_level1_id    VARCHAR2(32),
    region_level2_id    VARCHAR2(32),
    status              NUMBER(1) DEFAULT 1,
    sort                NUMBER(10) DEFAULT 0,
    create_time         DATE DEFAULT SYSDATE,
    update_time         DATE,
    create_by           VARCHAR2(32),
    update_by           VARCHAR2(32),
    deleted             NUMBER(1) DEFAULT 0,
    CONSTRAINT uk_external_type UNIQUE (external_id, entity_type)
);

-- 2. 添加表注释
COMMENT ON TABLE bc_sports_sys_entity_channel IS '实体渠道配置表（店铺/仓库/客户的渠道属性管理）';
COMMENT ON COLUMN bc_sports_sys_entity_channel.id IS '主键ID';
COMMENT ON COLUMN bc_sports_sys_entity_channel.external_id IS '外部实体ID（店铺/仓库/客户的原始ID）';
COMMENT ON COLUMN bc_sports_sys_entity_channel.brand_id IS '品牌ID';
COMMENT ON COLUMN bc_sports_sys_entity_channel.entity_type IS '实体类型：shop-店铺, stock-仓库, customer-客户';
COMMENT ON COLUMN bc_sports_sys_entity_channel.entity_name IS '实体名称（冗余存储）';
COMMENT ON COLUMN bc_sports_sys_entity_channel.channel_type_id IS '渠道类型ID（关联bc_sports_sys_channel_type父节点）';
COMMENT ON COLUMN bc_sports_sys_entity_channel.channel_def_id IS '渠道定义ID（关联bc_sports_sys_channel_type子节点）';
COMMENT ON COLUMN bc_sports_sys_entity_channel.channel_nature_id IS '渠道性质ID（关联bc_sports_sys_channel_nature父节点）';
COMMENT ON COLUMN bc_sports_sys_entity_channel.business_type_id IS '经营类型ID（关联bc_sports_sys_channel_nature子节点）';
COMMENT ON COLUMN bc_sports_sys_entity_channel.region_level1_id IS '一级地区ID（关联bc_sports_sys_region父节点）';
COMMENT ON COLUMN bc_sports_sys_entity_channel.region_level2_id IS '二级地区ID（关联bc_sports_sys_region子节点）';
COMMENT ON COLUMN bc_sports_sys_entity_channel.status IS '状态：0-禁用, 1-启用';
COMMENT ON COLUMN bc_sports_sys_entity_channel.sort IS '排序';
COMMENT ON COLUMN bc_sports_sys_entity_channel.create_time IS '创建时间';
COMMENT ON COLUMN bc_sports_sys_entity_channel.update_time IS '更新时间';
COMMENT ON COLUMN bc_sports_sys_entity_channel.create_by IS '创建人';
COMMENT ON COLUMN bc_sports_sys_entity_channel.update_by IS '更新人';
COMMENT ON COLUMN bc_sports_sys_entity_channel.deleted IS '逻辑删除：0-未删除, 1-已删除';

-- 3. 创建索引
CREATE INDEX idx_entity_channel_type ON bc_sports_sys_entity_channel(entity_type);
CREATE INDEX idx_entity_channel_external ON bc_sports_sys_entity_channel(external_id);
CREATE INDEX idx_entity_channel_status ON bc_sports_sys_entity_channel(status);
CREATE INDEX idx_entity_channel_deleted ON bc_sports_sys_entity_channel(deleted);
CREATE INDEX idx_entity_channel_region1 ON bc_sports_sys_entity_channel(region_level1_id);
CREATE INDEX idx_entity_channel_region2 ON bc_sports_sys_entity_channel(region_level2_id);

-- 4. 插入菜单权限数据
-- 注意：请根据实际的父菜单ID调整 parent_id 字段

-- 实体渠道配置菜单（类型1=菜单）
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, create_time) 
VALUES (
    'BI_ENTITY',                                    -- 菜单ID（请确保唯一）
    'BI_DIR',                                        -- 父菜单ID（请根据实际情况修改）
    '实体渠道配置',                             -- 菜单名称
    'bi bi-diagram-3',                          -- 图标
    1,                                          -- 菜单类型：1-菜单
    '/bi/entity-channel',                       -- 路由路径
    'bi:entity:query',                          -- 权限标识
    10,                                         -- 排序
    1,                                          -- 状态：1-启用
    1,                                          -- 可见：1-显示
    '管理店铺、仓库、客户的渠道属性配置',       -- 描述
    SYSDATE                                     -- 创建时间
);

-- 按钮权限（类型2=按钮，visible=0 隐藏不在菜单栏显示）
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, sort, status, visible, create_time) VALUES 
('10052', 'BI_ENTITY', '新增', 2, 'bi:entity:add', 2, 1, 0, SYSDATE),
('10053', 'BI_ENTITY', '编辑', 2, 'bi:entity:edit', 3, 1, 0, SYSDATE),
('10054', 'BI_ENTITY', '删除', 2, 'bi:entity:delete', 4, 1, 0, SYSDATE);

COMMIT;
