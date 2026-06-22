-- 企业微信 - 客户联系成员、群聊管理、朋友圈管理 菜单初始化
-- 前置条件：QYWX_DIR（企业微信目录菜单）已存在

-- 1. 客户联系成员
INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('QYWX_FOLLOW_USER', 'QYWX_DIR', '客户联系成员', 'bi-people', 1, '/qywx/follow-user', 'qywx:follow:query', 2, 1, 1, '配置了客户联系功能的成员列表', null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 2. 群聊管理
INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('QYWX_GROUP_CHAT', 'QYWX_DIR', '群聊管理', 'bi-chat-dots', 1, '/qywx/group-chat', 'qywx:groupchat:query', 3, 1, 1, '企业微信群聊列表', null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 3. 朋友圈管理
INSERT INTO BC_SPORTS_SYS_MENU (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE, DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
VALUES ('QYWX_MOMENT', 'QYWX_DIR', '朋友圈管理', 'bi-image', 1, '/qywx/moment', 'qywx:moment:query', 4, 1, 1, '企业微信朋友圈列表', null, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 绑定到 admin 角色
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (rawtohex(sys_guid()), '1', 'QYWX_FOLLOW_USER', SYSTIMESTAMP, 'admin');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (rawtohex(sys_guid()), '1', 'QYWX_GROUP_CHAT', SYSTIMESTAMP, 'admin');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (rawtohex(sys_guid()), '1', 'QYWX_MOMENT', SYSTIMESTAMP, 'admin');

COMMIT;
