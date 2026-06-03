-- ERP 店仓管理菜单初始化脚本
-- 数据源：bjerp C_STORE（只读）
-- 菜单类型: 0-目录, 1-菜单, 2-按钮

-- 主菜单（替换原 ERP 店铺管理 和 ERP 仓库管理）
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, create_time, update_time, deleted)
SELECT 'BI_ERP_STORE', 'BI_DIR', 'ERP 店仓管理', 'bi-shop', 1, '/bi/erp-store', 'bi:erpStore:query', 10, 1, 1, 'ERP 店仓信息管理（数据源：bjerp）', SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BI_ERP_STORE');

-- 为超级管理员角色分配 ERP 店仓管理菜单权限
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'BI_ERP_STORE', SYSTIMESTAMP, 'admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BI_ERP_STORE');

-- 禁用原 ERP 店铺管理 和 ERP 仓库管理 菜单（软删除）
UPDATE bc_sports_sys_menu SET deleted = 1, update_time = SYSTIMESTAMP WHERE id = 'BI_ERP_SHOP';
UPDATE bc_sports_sys_menu SET deleted = 1, update_time = SYSTIMESTAMP WHERE id = 'BI_ERP_WAREHOUSE';

COMMIT;
