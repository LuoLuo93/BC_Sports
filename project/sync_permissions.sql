-- ======================================================
-- BC体育 - ERP权限系统全量同步脚本 (最终版)
-- ======================================================
-- 功能：
-- 1. 恢复管理员密码为 admin123 (匹配当前环境哈希)
-- 2. 补全 BI 模块增删改功能权限
-- 3. 隐藏所有按钮项，精简侧边栏菜单
-- 4. 自动为管理员角色 (Role 1) 授权

BEGIN
    -- 1. 账号环境同步
    UPDATE bc_sports_sys_user 
    SET password = '27320b4545dca744e3b3018cabb23257', 
        salt = 'bcsport',
        status = 1
    WHERE username = 'admin';

    -- 2. BI 核心菜单标识符对齐
    UPDATE bc_sports_sys_menu SET permission = 'bi:brand:query', visible = 1 WHERE id = '31';
    UPDATE bc_sports_sys_menu SET permission = 'bi:region:query', visible = 1 WHERE id = '32';

    -- 3. 补全按钮权限 (Type 2, Visible 0)
    -- 品牌新增/修改/删除
    DELETE FROM bc_sports_sys_menu WHERE id IN ('311', '312', '313');
    INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible)
    VALUES ('311', '31', '品牌新增', 'bi-plus', 2, NULL, 'bi:brand:add', 1, 1, 0);
    INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible)
    VALUES ('312', '31', '品牌修改', 'bi-pencil', 2, NULL, 'bi:brand:edit', 2, 1, 0);
    INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible)
    VALUES ('313', '31', '品牌删除', 'bi-trash', 2, NULL, 'bi:brand:delete', 3, 1, 0);

    -- 地区新增/修改/删除
    DELETE FROM bc_sports_sys_menu WHERE id IN ('321', '322', '323');
    INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible)
    VALUES ('321', '32', '地区新增', 'bi-plus', 2, NULL, 'bi:region:add', 1, 1, 0);
    INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible)
    VALUES ('322', '32', '地区修改', 'bi-pencil', 2, NULL, 'bi:region:edit', 2, 1, 0);
    INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible)
    VALUES ('323', '32', '地区删除', 'bi-trash', 2, NULL, 'bi:region:delete', 3, 1, 0);

    -- 4. 自动归集权限至管理员 (Role 1)
    DELETE FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id IN ('311', '312', '313', '321', '322', '323');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', '311', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', '312', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', '313', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', '321', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', '322', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', '323', SYSTIMESTAMP, 'admin');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('全系统逻辑同步成功！');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/
