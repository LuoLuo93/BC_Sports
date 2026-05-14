-- ======================================================
-- IHR 人员离职管理 - 菜单权限脚本
-- 执行目标：Oracle 主数据库
-- 说明：
--   menu_type = 1  页面菜单（侧边栏可见）
--   menu_type = 2  按钮权限（侧边栏不可见, visible=0）
-- ======================================================

BEGIN
    -- ========================================
    -- 1. 人员离职管理 页面菜单
    -- ========================================
    DELETE FROM bc_sports_sys_menu WHERE id = 'IHR_LEAVING_MANAGEMENT';
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_LEAVING_MANAGEMENT', 'IHR_DIR', '人员离职管理', 'bi-person-x', 1, '/ihr/leaving-management', 'ihr:leaving:query',
        5, 1, 1, '查看离职员工及企微同步状态', NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- ========================================
    -- 2. 同步按钮权限
    -- ========================================
    DELETE FROM bc_sports_sys_menu WHERE id = 'IHR_LEAVING_SYNC';
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_LEAVING_SYNC', 'IHR_LEAVING_MANAGEMENT', '同步操作', NULL, 2, NULL, 'ihr:leaving:sync',
        1, 1, 0, NULL, NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- ========================================
    -- 3. 为超级管理员角色 (role_id=1) 分配权限
    -- ========================================
    DELETE FROM bc_sports_sys_role_menu WHERE role_id = '1'
        AND menu_id IN ('IHR_LEAVING_MANAGEMENT', 'IHR_LEAVING_SYNC');

    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', 'IHR_LEAVING_MANAGEMENT', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', 'IHR_LEAVING_SYNC',          SYSTIMESTAMP, 'admin');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('IHR 人员离职管理菜单权限部署完成');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('部署失败: ' || SQLERRM);
        RAISE;
END;
/
