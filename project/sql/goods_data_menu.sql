-- ==========================================================
-- 货品资料导入 - 菜单初始化
-- 库: 主库 Oracle (BC_ERP_MANAGE)
-- 前置条件：BI_DIR（BI管理目录菜单）已存在
-- ==========================================================

-- 1. 菜单项（挂在 BI管理 目录下）
INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('BI_GOODS_DATA', 'BI_DIR', '货品资料导入', 'bi-box-seam', 1, '/bi/goods-data', 'bi:goods-data:query', 30, 1, 1, 'Excel批量导入货品资料（品牌/货号/产品季/货品分类）', null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 2. 按钮权限
INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('BI_GOODS_DATA_IMPORT', 'BI_GOODS_DATA', '货品资料导入', '', 2, '', 'bi:goods-data:import', 1, 1, 0, '导入货品资料', null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 3. 绑定到 admin 角色
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (rawtohex(sys_guid()), '1', 'BI_GOODS_DATA', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (rawtohex(sys_guid()), '1', 'BI_GOODS_DATA_IMPORT', SYSTIMESTAMP, 'admin');

COMMIT;
