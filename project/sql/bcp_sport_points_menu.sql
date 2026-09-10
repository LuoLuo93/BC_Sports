-- ============================================================
-- BC好玩家 - 菜单初始化脚本 —— 主库 Oracle 执行
-- 内容：新增"BC好玩家"一级目录 + "运动积分导入"菜单 + 导入按钮权限，
--       并为管理员角色(Role ID = 1)授权
-- 前置条件：bc_sports_sys_menu / bc_sports_sys_role_menu 表已存在，
--           bcp_sport_points_schema.sql 已执行
-- 幂等：所有 INSERT 使用 WHERE NOT EXISTS，可重复执行
-- ============================================================

-- 1. 一级目录：BC好玩家
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted)
SELECT 'BCP_DIR', '0', 'BC好玩家', 'bi-trophy', 0, '/bcp', NULL, 40, 1, 1, 'BC好玩家运营数据', 'orange', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BCP_DIR');

-- 2. 菜单：运动积分导入
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted)
SELECT 'BCP_SPORT_POINTS', 'BCP_DIR', '运动积分导入', 'bi-clipboard-data', 1, '/bcp/sport-points', 'bcp:sport-points:query', 1, 1, 1, 'Excel批量导入BC好玩家运动积分（sporter/points）', NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BCP_SPORT_POINTS');

-- 3. 按钮：导入权限
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted)
SELECT 'BCP_SPORT_POINTS_IMPORT', 'BCP_SPORT_POINTS', '导入运动积分', NULL, 2, NULL, 'bcp:sport-points:import', 1, 1, 0, '上传Excel批量导入运动积分', NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BCP_SPORT_POINTS_IMPORT');

-- 4. 为管理员角色 (Role ID = 1) 授权
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT RAWTOHEX(SYS_GUID()), '1', 'BCP_DIR', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BCP_DIR');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT RAWTOHEX(SYS_GUID()), '1', 'BCP_SPORT_POINTS', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BCP_SPORT_POINTS');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT RAWTOHEX(SYS_GUID()), '1', 'BCP_SPORT_POINTS_IMPORT', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BCP_SPORT_POINTS_IMPORT');

COMMIT;
