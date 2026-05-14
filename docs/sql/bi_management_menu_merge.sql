-- ============================================================
-- 基础数据管理菜单合并迁移脚本
-- 将 品牌/地区/渠道类型/渠道性质 4个菜单合并为1个"基础数据管理"菜单
-- ============================================================

-- 1. 隐藏4个独立菜单（保留记录，仅设为不可见）
UPDATE bc_sports_sys_menu SET visible = 0
WHERE id IN ('BI_BRAND', 'BI_REGION', 'BI_CHANNEL_TYPE', 'BI_CHANNEL_NATURE');

-- 2. 新增统一菜单（parent_id 指向 BI_DIR）
INSERT INTO bc_sports_sys_menu (
    id, parent_id, menu_name, icon, menu_type, path, permission,
    sort, status, visible, description, icon_color,
    create_time, update_time, create_by, update_by, deleted
) VALUES (
    'BI_MANAGEMENT', 'BI_DIR', '基础数据管理', 'bi-database', 1, '/bi/management', 'bi:brand:query',
    1, 1, 1, '品牌/地区/渠道类型/渠道性质统一管理', NULL,
    SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
);

-- 3. 将新菜单分配给超级管理员角色 (role_id = '1')
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES ('BI_MGMT_ROLE_01', '1', 'BI_MANAGEMENT', SYSTIMESTAMP, 'admin');

-- 4. 将新菜单分配给普通用户角色 (role_id = '2043948036631126017')
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES ('BI_MGMT_ROLE_U01', '2043948036631126017', 'BI_MANAGEMENT', SYSTIMESTAMP, 'admin');

COMMIT;
