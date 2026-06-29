-- ==========================================================
-- 客户首次添加导入 - 菜单初始化
-- 库: 主库 Oracle (BC_ERP_MANAGE)，菜单表 bc_sports_sys_menu
-- 前置条件：BI_DIR（BI管理目录菜单）已存在
-- ==========================================================

-- 1. 菜单项（挂在 BI管理 目录下）
INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('BI_FIRST_ADD', 'BI_DIR', '客户首次添加导入', 'bi-upload', 1, '/bi/first-add', 'bi:first-add:import', 10, 1, 1, '上传Excel批量导入客户首次添加记录', null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 2. 绑定到 admin 角色
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (rawtohex(sys_guid()), '1', 'BI_FIRST_ADD', SYSTIMESTAMP, 'admin');

COMMIT;
