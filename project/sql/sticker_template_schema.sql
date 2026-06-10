-- ============================================================
-- 标签模板管理 - Oracle DDL + 菜单权限
-- 执行时间：2026-06-10
-- ============================================================

-- ============================================================
-- 1. 建表：sticker_template（标签模板）
-- ============================================================
CREATE TABLE sticker_template (
    id              VARCHAR2(32)    NOT NULL,
    template_name   VARCHAR2(100)   NOT NULL,
    label_width     NUMBER(6,2)     NOT NULL,       -- 标签宽度 mm
    label_height    NUMBER(6,2)     NOT NULL,       -- 标签高度 mm
    dpi             NUMBER(4)       DEFAULT 203,    -- 打印 DPI（203/300）
    template_data   CLOB,                           -- 模板元素 JSON
    is_default      NUMBER(1)       DEFAULT 0,      -- 是否默认模板 0否 1是
    status          NUMBER(1)       DEFAULT 1,      -- 状态 0禁用 1启用
    create_time     TIMESTAMP       DEFAULT SYSTIMESTAMP,
    update_time     TIMESTAMP,
    create_by       VARCHAR2(50),
    update_by       VARCHAR2(50),
    deleted         NUMBER(1)       DEFAULT 0,
    CONSTRAINT pk_sticker_template PRIMARY KEY (id)
);

COMMENT ON TABLE  sticker_template                IS '标签模板';
COMMENT ON COLUMN sticker_template.template_name   IS '模板名称';
COMMENT ON COLUMN sticker_template.label_width     IS '标签宽度(mm)';
COMMENT ON COLUMN sticker_template.label_height    IS '标签高度(mm)';
COMMENT ON COLUMN sticker_template.dpi             IS '打印DPI';
COMMENT ON COLUMN sticker_template.template_data   IS '模板元素JSON';
COMMENT ON COLUMN sticker_template.is_default      IS '是否默认模板 0否 1是';
COMMENT ON COLUMN sticker_template.status          IS '状态 0禁用 1启用';

CREATE INDEX idx_sticker_template_name ON sticker_template (template_name);


-- ============================================================
-- 2. 修改表：sticker_print_order 添加 template_id 字段
-- ============================================================
ALTER TABLE sticker_print_order ADD template_id VARCHAR2(32);
COMMENT ON COLUMN sticker_print_order.template_id IS '使用的标签模板ID';


-- ============================================================
-- 3. 菜单权限 SQL
-- ============================================================

-- 3.1 菜单：标签模板管理（放在 STICKER_DIR 目录下，sort=3 排在贴纸资料维护后面）
INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('STICKER_TEMPLATE', 'STICKER_DIR', '标签模板管理', 'bi-tag', 1, '/sticker/template', 'sticker:template:query', 3, 1, 1, '标签模板设计与管理', null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 3.2 按钮权限
INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('STICKER_TEMPLATE_ADD', 'STICKER_TEMPLATE', '新增', null, 2, null, 'sticker:template:add', 1, 1, 0, null, null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('STICKER_TEMPLATE_EDIT', 'STICKER_TEMPLATE', '编辑', null, 2, null, 'sticker:template:edit', 2, 1, 0, null, null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('STICKER_TEMPLATE_DEL', 'STICKER_TEMPLATE', '删除', null, 2, null, 'sticker:template:delete', 3, 1, 0, null, null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 3.3 绑定到 admin 角色 (role_id=1)
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT rawtohex(sys_guid()), '1', id, SYSTIMESTAMP, 'admin' FROM bc_sports_sys_menu
WHERE id IN ('STICKER_TEMPLATE', 'STICKER_TEMPLATE_ADD', 'STICKER_TEMPLATE_EDIT', 'STICKER_TEMPLATE_DEL');

COMMIT;
