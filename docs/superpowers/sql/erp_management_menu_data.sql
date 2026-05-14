-- ======================================================
-- ERP管理 - 菜单权限脚本
-- 执行目标：Oracle 主数据库
-- 说明：
--   menu_type = 0  目录（侧边栏可见）
--   menu_type = 1  页面菜单（侧边栏可见）
--   menu_type = 2  按钮权限（侧边栏不可见, visible=0）
-- ======================================================

BEGIN
    -- ========================================
    -- 1. ERP管理 目录
    -- ========================================
    DELETE FROM bc_sports_sys_menu WHERE id = 'ERP_DIR';
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'ERP_DIR', '0', 'ERP管理', 'bi-building', 0, '/erp', NULL,
        25, 1, 1, 'ERP系统管理', NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- ========================================
    -- 2. 人员管理 页面菜单
    -- ========================================
    DELETE FROM bc_sports_sys_menu WHERE id = 'ERP_EMPLOYEE_MANAGEMENT';
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'ERP_EMPLOYEE_MANAGEMENT', 'ERP_DIR', '人员管理', 'bi-people', 1, '/erp/employee-management', 'erp:employee:query',
        1, 1, 1, 'ERP人员同步管理（终端店铺员工）', NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- ========================================
    -- 3. 同步按钮权限
    -- ========================================
    DELETE FROM bc_sports_sys_menu WHERE id = 'ERP_EMPLOYEE_SYNC';
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'ERP_EMPLOYEE_SYNC', 'ERP_EMPLOYEE_MANAGEMENT', '同步操作', NULL, 2, NULL, 'erp:employee:sync',
        1, 1, 0, NULL, NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- ========================================
    -- 4. 为超级管理员角色 (role_id=1) 分配权限
    -- ========================================
    DELETE FROM bc_sports_sys_role_menu WHERE role_id = '1'
        AND menu_id IN ('ERP_DIR', 'ERP_EMPLOYEE_MANAGEMENT', 'ERP_EMPLOYEE_SYNC');

    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', 'ERP_DIR', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', 'ERP_EMPLOYEE_MANAGEMENT', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', 'ERP_EMPLOYEE_SYNC', SYSTIMESTAMP, 'admin');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('ERP管理菜单权限部署完成');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('部署失败: ' || SQLERRM);
        RAISE;
END;
/
