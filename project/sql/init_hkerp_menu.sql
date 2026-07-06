-- ======================================================
-- BC体育 - 旧版HK ERP同步 菜单初始化脚本
-- ======================================================
-- 功能：新增"HK ERP同步"一级目录 + "职员同步"菜单 + 同步按钮权限
--       并为管理员角色(Role ID = 1)授权
-- 移植自 interfaceForHK，直写 HKERP 库 Bas_Personnel 表的链路
-- 前置条件：bc_sports_sys_menu / bc_sports_sys_role_menu 表已存在
-- 幂等：所有 INSERT 使用 WHERE NOT EXISTS，可重复执行

-- 1. 一级目录：HK ERP同步
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, create_time, update_time, deleted)
SELECT 'HKERP_DIR', '0', 'HK ERP同步', 'bi-arrow-left-right', 0, '/hkerp', NULL, 35, 1, 1, '旧版HK ERP职员资料直写同步（移植自interfaceForHK）', SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'HKERP_DIR');

-- 2. 菜单：职员同步
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, create_time, update_time, deleted)
SELECT 'HKERP_PERSONNEL', 'HKERP_DIR', '职员同步', 'bi-people', 1, '/hkerp/personnel-sync', 'hk:personnel:query', 1, 1, 1, 'HK ERP入职/变更/离职同步', SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'HKERP_PERSONNEL');

-- 3. 按钮：手动同步权限
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, create_time, update_time, deleted)
SELECT 'HKERP_PERSONNEL_SYNC', 'HKERP_PERSONNEL', '手动同步', NULL, 2, NULL, 'hk:personnel:sync', 1, 1, 0, '触发HK ERP入职/变更/离职同步及Token刷新', SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'HKERP_PERSONNEL_SYNC');

-- 4. 为管理员角色 (Role ID = 1) 授权
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'HKERP_DIR', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'HKERP_DIR');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'HKERP_PERSONNEL', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'HKERP_PERSONNEL');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'HKERP_PERSONNEL_SYNC', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'HKERP_PERSONNEL_SYNC');

COMMIT;

-- ======================================================
-- 对应的定时任务（ScheduleJob）建议配置：
-- 在"运维监控 > 定时任务"页面新增以下任务，cron 沿用源项目：
--   1. hkerp.token.refresh    cron: 0 0 */2 * * ?        （每2小时刷新Token，源项目未配置定时，按需）
--   2. hkerp.onboarding.sync  cron: 0 30 11,12,18 * * ?  （源项目 insertNewPersonnel）
--   3. hkerp.update.sync      cron: 0 10 12,6,0 * * ?    （源项目 updatePersonnel）
-- 任务 bean/方法 已在 ScheduleTaskRegistry 注册，前端只需配置 cron 即可。
-- ======================================================
