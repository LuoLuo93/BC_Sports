-- ============================================================
-- 贴纸打印 - 新增「矫正尺码组维护」菜单 + 按钮权限
-- 父级: STICKER_DIR (贴纸目录,已在线上数据中存在)
-- ============================================================

-- 1. 菜单
INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('STICKER_SIZE_GROUP', 'STICKER_DIR', '矫正尺码组维护', 'bi-rulers', 1, '/sticker/size-group', 'sticker:size-group:query', 7, 1, 1, '矫正尺码组维护(按品牌+类别隔离)', null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 2. 按钮权限
INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('STICKER_SIZE_GROUP_ADD', 'STICKER_SIZE_GROUP', '新增', null, 2, null, 'sticker:size-group:add', 1, 1, 0, null, null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('STICKER_SIZE_GROUP_EDIT', 'STICKER_SIZE_GROUP', '编辑', null, 2, null, 'sticker:size-group:edit', 2, 1, 0, null, null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('STICKER_SIZE_GROUP_DEL', 'STICKER_SIZE_GROUP', '删除', null, 2, null, 'sticker:size-group:delete', 3, 1, 0, null, null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 3. 绑定到 admin 角色 (role_id=1)
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT rawtohex(sys_guid()), '1', id, SYSTIMESTAMP, 'admin' FROM bc_sports_sys_menu
WHERE id IN ('STICKER_SIZE_GROUP', 'STICKER_SIZE_GROUP_ADD', 'STICKER_SIZE_GROUP_EDIT', 'STICKER_SIZE_GROUP_DEL');

COMMIT;
