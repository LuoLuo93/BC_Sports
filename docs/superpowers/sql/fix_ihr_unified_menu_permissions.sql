-- ======================================================
-- IHR 三合一菜单 - 权限修复脚本
-- 执行目标：Oracle 主数据库
-- 说明：如果已执行旧版 ihr_employee_management_unify_menu.sql
--       （只有 ihr:onboarding:query/sync），需要补齐缺失权限
-- ======================================================

BEGIN
    -- 删除旧版只创建的2个权限
    DELETE FROM bc_sports_sys_role_menu WHERE menu_id IN ('IHR_EMPLOYEE_SYNC');
    DELETE FROM bc_sports_sys_menu WHERE id IN ('IHR_EMPLOYEE_SYNC');

    -- 补齐入职查询/同步（如果旧版页面菜单的 permission 字段不对也一并修复）
    UPDATE bc_sports_sys_menu SET permission = 'ihr:onboarding:query' WHERE id = 'IHR_EMPLOYEE_MANAGEMENT';

    -- 入职查询/同步
    INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, sort, status, visible, create_time, update_time, create_by, update_by, deleted)
    SELECT 'IHR_ONBOARDING_QUERY', 'IHR_EMPLOYEE_MANAGEMENT', '入职查询', 2, 'ihr:onboarding:query', 1, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'IHR_ONBOARDING_QUERY');

    INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, sort, status, visible, create_time, update_time, create_by, update_by, deleted)
    SELECT 'IHR_ONBOARDING_SYNC2', 'IHR_EMPLOYEE_MANAGEMENT', '入职同步', 2, 'ihr:onboarding:sync', 2, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'IHR_ONBOARDING_SYNC2');

    -- 调整查询/同步
    INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, sort, status, visible, create_time, update_time, create_by, update_by, deleted)
    SELECT 'IHR_UPDATE_QUERY', 'IHR_EMPLOYEE_MANAGEMENT', '调整查询', 2, 'ihr:update:query', 3, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'IHR_UPDATE_QUERY');

    INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, sort, status, visible, create_time, update_time, create_by, update_by, deleted)
    SELECT 'IHR_UPDATE_SYNC2', 'IHR_EMPLOYEE_MANAGEMENT', '调整同步', 2, 'ihr:update:sync', 4, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'IHR_UPDATE_SYNC2');

    -- 离职查询/同步
    INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, sort, status, visible, create_time, update_time, create_by, update_by, deleted)
    SELECT 'IHR_LEAVING_QUERY', 'IHR_EMPLOYEE_MANAGEMENT', '离职查询', 2, 'ihr:leaving:query', 5, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'IHR_LEAVING_QUERY');

    INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, sort, status, visible, create_time, update_time, create_by, update_by, deleted)
    SELECT 'IHR_LEAVING_SYNC2', 'IHR_EMPLOYEE_MANAGEMENT', '离职同步', 2, 'ihr:leaving:sync', 6, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'IHR_LEAVING_SYNC2');

    -- 为超级管理员补齐所有权限
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
    SELECT SYS_GUID(), '1', 'IHR_ONBOARDING_QUERY', SYSTIMESTAMP, 'admin'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'IHR_ONBOARDING_QUERY');

    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
    SELECT SYS_GUID(), '1', 'IHR_ONBOARDING_SYNC2', SYSTIMESTAMP, 'admin'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'IHR_ONBOARDING_SYNC2');

    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
    SELECT SYS_GUID(), '1', 'IHR_UPDATE_QUERY', SYSTIMESTAMP, 'admin'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'IHR_UPDATE_QUERY');

    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
    SELECT SYS_GUID(), '1', 'IHR_UPDATE_SYNC2', SYSTIMESTAMP, 'admin'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'IHR_UPDATE_SYNC2');

    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
    SELECT SYS_GUID(), '1', 'IHR_LEAVING_QUERY', SYSTIMESTAMP, 'admin'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'IHR_LEAVING_QUERY');

    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
    SELECT SYS_GUID(), '1', 'IHR_LEAVING_SYNC2', SYSTIMESTAMP, 'admin'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'IHR_LEAVING_SYNC2');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('IHR三合一菜单权限修复完成');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('修复失败: ' || SQLERRM);
        RAISE;
END;
/
