-- ============================================================
-- 揽众资料 菜单 + 按钮权限 + 角色授权
-- ============================================================

-- 1. 页面菜单（挂在 ERP_DIR 目录下）
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission,
    sort, status, visible, description, create_time, update_time, create_by, update_by, deleted)
SELECT 'LZ_CUSTOMER', 'ERP_DIR', '揽众资料', 'bi-coin', 1, '/erp/lz-customer', 'erp:lzCustomer:query',
    15, 1, 1, '揽众客户押金资料管理', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'LZ_CUSTOMER');

-- 2. 按钮权限：新增
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission,
    sort, status, visible, description, create_time, update_time, create_by, update_by, deleted)
SELECT 'LZ_CUSTOMER_ADD', 'LZ_CUSTOMER', '新增', NULL, 2, NULL, 'erp:lzCustomer:add',
    1, 1, 0, NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'LZ_CUSTOMER_ADD');

-- 3. 按钮权限：编辑
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission,
    sort, status, visible, description, create_time, update_time, create_by, update_by, deleted)
SELECT 'LZ_CUSTOMER_EDIT', 'LZ_CUSTOMER', '编辑', NULL, 2, NULL, 'erp:lzCustomer:edit',
    2, 1, 0, NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'LZ_CUSTOMER_EDIT');

-- 4. 按钮权限：删除
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission,
    sort, status, visible, description, create_time, update_time, create_by, update_by, deleted)
SELECT 'LZ_CUSTOMER_DELETE', 'LZ_CUSTOMER', '删除', NULL, 2, NULL, 'erp:lzCustomer:delete',
    3, 1, 0, NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'LZ_CUSTOMER_DELETE');

-- 5. 授权给超管角色 (role_id='1')
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'LZ_CUSTOMER', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'LZ_CUSTOMER');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'LZ_CUSTOMER_ADD', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'LZ_CUSTOMER_ADD');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'LZ_CUSTOMER_EDIT', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'LZ_CUSTOMER_EDIT');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'LZ_CUSTOMER_DELETE', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'LZ_CUSTOMER_DELETE');
