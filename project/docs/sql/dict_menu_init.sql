-- 数据字典菜单 + 权限按钮
-- 菜单项挂在系统管理 (SYS_DIR) 下，sort=5 排在部门管理后面

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted)
VALUES ('SYS_DICT', 'SYS_DIR', '数据字典', 'bi-journal-text', 1, '/system/dict', 'system:dict:query', 5, 1, 1, '数据字典类型与键值对管理', NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, update_time, create_by, update_by, deleted)
VALUES ('SYS_DICT_ADD', 'SYS_DICT', '字典新增', NULL, 2, NULL, 'system:dict:add', 1, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, update_time, create_by, update_by, deleted)
VALUES ('SYS_DICT_EDIT', 'SYS_DICT', '字典修改', NULL, 2, NULL, 'system:dict:edit', 2, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, update_time, create_by, update_by, deleted)
VALUES ('SYS_DICT_DEL', 'SYS_DICT', '字典删除', NULL, 2, NULL, 'system:dict:delete', 3, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 给 admin 角色(id=1) 绑定字典菜单权限
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6', '1', 'SYS_DICT', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D7', '1', 'SYS_DICT_ADD', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D8', '1', 'SYS_DICT_EDIT', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D9', '1', 'SYS_DICT_DEL', SYSTIMESTAMP, 'admin');

COMMIT;
