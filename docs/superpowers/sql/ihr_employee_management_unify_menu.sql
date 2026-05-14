-- ======================================================
-- IHR 人员管理 - 三合一菜单合并脚本
-- 执行目标：Oracle 主数据库
-- 说明：将原来的人员入职管理/人员调整管理/人员离职管理
--       三个菜单合并为一个"人员管理"菜单，页签切换
-- ======================================================

BEGIN
    -- ========================================
    -- 1. 删除旧的三条页面菜单及子权限
    -- ========================================
    DELETE FROM bc_sports_sys_role_menu WHERE menu_id IN (
        'IHR_ONBOARDING_MANAGEMENT', 'IHR_ONBOARDING_SYNC',
        'IHR_UPDATE_MANAGEMENT', 'IHR_UPDATE_SYNC',
        'IHR_LEAVING_MANAGEMENT', 'IHR_LEAVING_SYNC'
    );

    DELETE FROM bc_sports_sys_menu WHERE id IN (
        'IHR_ONBOARDING_SYNC', 'IHR_UPDATE_SYNC', 'IHR_LEAVING_SYNC',
        'IHR_ONBOARDING_MANAGEMENT', 'IHR_UPDATE_MANAGEMENT', 'IHR_LEAVING_MANAGEMENT'
    );

    -- ========================================
    -- 2. 新建统一"人员管理"页面菜单
    -- ========================================
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_EMPLOYEE_MANAGEMENT', 'IHR_DIR', '人员管理', 'bi-people', 1, '/ihr/employee-management', 'ihr:onboarding:query',
        3, 1, 1, 'IHR人员入职/调整/离职管理（三合一）', NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- ========================================
    -- 3. 保留全部6个权限标识，确保Controller端点不403
    -- ========================================
    -- 入职查询/同步
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, menu_type, permission,
        sort, status, visible,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_ONBOARDING_QUERY', 'IHR_EMPLOYEE_MANAGEMENT', '入职查询', 2, 'ihr:onboarding:query',
        1, 1, 0,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, menu_type, permission,
        sort, status, visible,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_ONBOARDING_SYNC2', 'IHR_EMPLOYEE_MANAGEMENT', '入职同步', 2, 'ihr:onboarding:sync',
        2, 1, 0,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- 调整查询/同步
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, menu_type, permission,
        sort, status, visible,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_UPDATE_QUERY', 'IHR_EMPLOYEE_MANAGEMENT', '调整查询', 2, 'ihr:update:query',
        3, 1, 0,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, menu_type, permission,
        sort, status, visible,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_UPDATE_SYNC2', 'IHR_EMPLOYEE_MANAGEMENT', '调整同步', 2, 'ihr:update:sync',
        4, 1, 0,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- 离职查询/同步
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, menu_type, permission,
        sort, status, visible,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_LEAVING_QUERY', 'IHR_EMPLOYEE_MANAGEMENT', '离职查询', 2, 'ihr:leaving:query',
        5, 1, 0,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, menu_type, permission,
        sort, status, visible,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_LEAVING_SYNC2', 'IHR_EMPLOYEE_MANAGEMENT', '离职同步', 2, 'ihr:leaving:sync',
        6, 1, 0,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- ========================================
    -- 4. 为超级管理员角色 (role_id=1) 分配全部权限
    -- ========================================
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
    VALUES (SYS_GUID(), '1', 'IHR_EMPLOYEE_MANAGEMENT', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
    VALUES (SYS_GUID(), '1', 'IHR_ONBOARDING_QUERY', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
    VALUES (SYS_GUID(), '1', 'IHR_ONBOARDING_SYNC2', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
    VALUES (SYS_GUID(), '1', 'IHR_UPDATE_QUERY', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
    VALUES (SYS_GUID(), '1', 'IHR_UPDATE_SYNC2', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
    VALUES (SYS_GUID(), '1', 'IHR_LEAVING_QUERY', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
    VALUES (SYS_GUID(), '1', 'IHR_LEAVING_SYNC2', SYSTIMESTAMP, 'admin');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('IHR人员管理三合一菜单合并完成');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('合并失败: ' || SQLERRM);
        RAISE;
END;
/
