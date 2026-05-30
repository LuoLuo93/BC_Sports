-- 添加缺失的数据库索引
-- 关联表索引
CREATE INDEX idx_user_role_user ON bc_sports_sys_user_role(user_id);
CREATE INDEX idx_user_role_role ON bc_sports_sys_user_role(role_id);
CREATE INDEX idx_role_menu_role ON bc_sports_sys_role_menu(role_id);
CREATE INDEX idx_role_menu_menu ON bc_sports_sys_role_menu(menu_id);

-- 用户表部门索引
CREATE INDEX idx_user_dept ON bc_sports_sys_user(dept_id);

-- 日志表索引（如果不存在）
CREATE INDEX idx_log_operation_time ON bc_sports_sys_log(operation_time);
CREATE INDEX idx_log_username ON bc_sports_sys_log(username);

COMMIT;
