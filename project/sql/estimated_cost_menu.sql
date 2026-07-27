-- ============================================================
-- 预估成本管理 菜单 + 按钮权限 + 角色授权
-- 挂在 ERP_DIR 目录下
-- ============================================================

-- 1. 页面菜单
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission,
    sort, status, visible, description, create_time, update_time, create_by, update_by, deleted)
SELECT 'ERP_ESTIMATED_COST', 'ERP_DIR', '预估成本管理', 'bi-currency-yen', 1, '/erp/estimated-cost', 'erp:estimatedCost:query',
    20, 1, 1, '伯俊ERP货品预估成本管理与批量导入', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'ERP_ESTIMATED_COST');

-- 2. 按钮权限：编辑
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission,
    sort, status, visible, description, create_time, update_time, create_by, update_by, deleted)
SELECT 'ERP_ESTIMATED_COST_EDIT', 'ERP_ESTIMATED_COST', '编辑', NULL, 2, NULL, 'erp:estimatedCost:edit',
    1, 1, 0, NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'ERP_ESTIMATED_COST_EDIT');

-- 3. 按钮权限：导入
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission,
    sort, status, visible, description, create_time, update_time, create_by, update_by, deleted)
SELECT 'ERP_ESTIMATED_COST_IMPORT', 'ERP_ESTIMATED_COST', '导入', NULL, 2, NULL, 'erp:estimatedCost:import',
    2, 1, 0, NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'ERP_ESTIMATED_COST_IMPORT');

-- 4. 授权给超管角色 (role_id='1')
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'ERP_ESTIMATED_COST', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'ERP_ESTIMATED_COST');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'ERP_ESTIMATED_COST_EDIT', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'ERP_ESTIMATED_COST_EDIT');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'ERP_ESTIMATED_COST_IMPORT', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'ERP_ESTIMATED_COST_IMPORT');
