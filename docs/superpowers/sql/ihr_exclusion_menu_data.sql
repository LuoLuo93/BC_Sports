-- ======================================================
-- IHR 员工排除管理 - 菜单权限脚本
-- 执行目标：Oracle 主数据库
-- 说明：
--   menu_type = 1  目录/页面菜单（侧边栏可见）
--   menu_type = 2  按钮权限（侧边栏不可见, visible=0）
--   入职排除和离职排除共用同一套权限 (ihr:exclusion:*)
-- ======================================================

BEGIN
    -- ========================================
    -- 1. IHR管理 父目录菜单
    -- ========================================
    DELETE FROM bc_sports_sys_menu WHERE id = 'IHR_DIR';
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_DIR', '0', 'IHR管理', 'bi-people', 1, '/ihr', NULL,
        90, 1, 1, 'IHR员工数据管理', NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- ========================================
    -- 2. 入职排除 页面菜单
    -- ========================================
    DELETE FROM bc_sports_sys_menu WHERE id = 'IHR_ONBOARDING_EXCLUSION';
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_ONBOARDING_EXCLUSION', 'IHR_DIR', '入职排除', 'bi-person-plus', 1, '/ihr/onboarding-exclusion', 'ihr:exclusion:query',
        1, 1, 1, '管理不入企微的入职员工', NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- ========================================
    -- 3. 离职排除 页面菜单
    -- ========================================
    DELETE FROM bc_sports_sys_menu WHERE id = 'IHR_LEAVING_EXCLUSION';
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_LEAVING_EXCLUSION', 'IHR_DIR', '离职排除', 'bi-person-dash', 1, '/ihr/leaving-exclusion', 'ihr:exclusion:query',
        2, 1, 1, '管理不从企微移除的离职员工', NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- ========================================
    -- 4. 按钮权限（挂在入职排除页面下，两个页面共用）
    --    两个页面 Controller 使用相同的 @RequiresPermissions
    -- ========================================

    -- 新增
    DELETE FROM bc_sports_sys_menu WHERE id = 'IHR_EXCLUSION_ADD';
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_EXCLUSION_ADD', 'IHR_ONBOARDING_EXCLUSION', '新增', NULL, 2, NULL, 'ihr:exclusion:add',
        1, 1, 0, NULL, NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- 编辑（含批量启用/禁用）
    DELETE FROM bc_sports_sys_menu WHERE id = 'IHR_EXCLUSION_EDIT';
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_EXCLUSION_EDIT', 'IHR_ONBOARDING_EXCLUSION', '编辑', NULL, 2, NULL, 'ihr:exclusion:edit',
        2, 1, 0, NULL, NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- 删除（含批量删除）
    DELETE FROM bc_sports_sys_menu WHERE id = 'IHR_EXCLUSION_DELETE';
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_EXCLUSION_DELETE', 'IHR_ONBOARDING_EXCLUSION', '删除', NULL, 2, NULL, 'ihr:exclusion:delete',
        3, 1, 0, NULL, NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- ========================================
    -- 5. 为超级管理员角色 (role_id=1) 分配权限
    -- ========================================
    DELETE FROM bc_sports_sys_role_menu WHERE role_id = '1'
        AND menu_id IN ('IHR_DIR', 'IHR_ONBOARDING_EXCLUSION', 'IHR_LEAVING_EXCLUSION',
                        'IHR_EXCLUSION_ADD', 'IHR_EXCLUSION_EDIT', 'IHR_EXCLUSION_DELETE');

    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', 'IHR_DIR',                    SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', 'IHR_ONBOARDING_EXCLUSION',   SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', 'IHR_LEAVING_EXCLUSION',      SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', 'IHR_EXCLUSION_ADD',          SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', 'IHR_EXCLUSION_EDIT',         SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', 'IHR_EXCLUSION_DELETE',       SYSTIMESTAMP, 'admin');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('========================================');
    DBMS_OUTPUT.PUT_LINE('IHR 排除管理菜单权限部署完成');
    DBMS_OUTPUT.PUT_LINE('========================================');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('部署失败: ' || SQLERRM);
        RAISE;
END;
/
