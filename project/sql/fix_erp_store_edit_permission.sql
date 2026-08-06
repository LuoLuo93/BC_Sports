-- ============================================================
-- 修复：补充 ERP 店仓管理 缺失的「编辑」按钮权限
-- 背景：init_erp_store.sql 只建了 query 菜单，漏建 edit 按钮，
--       导致非 admin 用户（如 hut）即使有管理员角色也看不到编辑按钮。
-- ============================================================

-- 1. 插入编辑按钮权限（menu_type=2，挂在 BI_ERP_STORE 下）
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, update_time, deleted)
SELECT 'BI_ERP_STORE_EDIT', 'BI_ERP_STORE', '店仓编辑', NULL, 2, NULL, 'bi:erpStore:edit', 0, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BI_ERP_STORE_EDIT');

-- 2. 把编辑权限分配给所有已拥有店仓查询权限的角色（覆盖 hut 等所有角色，不限 role 1）
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), rm.role_id, 'BI_ERP_STORE_EDIT', SYSTIMESTAMP, 'admin'
FROM bc_sports_sys_role_menu rm
WHERE rm.menu_id = 'BI_ERP_STORE'
  AND NOT EXISTS (
    SELECT 1 FROM bc_sports_sys_role_menu rm2
    WHERE rm2.role_id = rm.role_id AND rm2.menu_id = 'BI_ERP_STORE_EDIT'
  );

COMMIT;
