-- 贴纸打印 - 新增「Agent 监控」菜单

-- 1. 菜单
INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('STICKER_AGENT', 'STICKER_DIR', 'Agent 监控', 'bi-pc-display', 1, '/sticker/agent', 'sticker:agent:query', 6, 1, 1, '打印代理监控', null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 2. 绑定到 admin 角色
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT rawtohex(sys_guid()), '1', id, SYSTIMESTAMP, 'admin' FROM bc_sports_sys_menu
WHERE id = 'STICKER_AGENT';

COMMIT;
