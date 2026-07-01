-- ==========================================================
-- 零售主管管理 - 菜单初始化
-- 库: 主库 Oracle (BC_ERP_MANAGE)，菜单表 bc_sports_sys_menu
-- 前置条件：BI_DIR（BI管理目录菜单）已存在
-- ==========================================================

-- 1. 菜单项（挂在 BI管理 目录下）
INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('BI_RETAIL_SUPERVISOR', 'BI_DIR', '零售主管管理', 'bi-people', 1, '/bi/retail-supervisor', 'bi:retail-supervisor:query', 20, 1, 1, '零售主管信息管理', null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 2. 绑定到 admin 角色
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (rawtohex(sys_guid()), '1', 'BI_RETAIL_SUPERVISOR', SYSTIMESTAMP, 'admin');

COMMIT;