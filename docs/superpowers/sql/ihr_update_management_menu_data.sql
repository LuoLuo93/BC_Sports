-- ======================================================
-- IHR 人员调整管理 - 菜单权限脚本
-- 执行目标：Oracle 主数据库
-- ======================================================

BEGIN
    DELETE FROM bc_sports_sys_menu WHERE id = 'IHR_UPDATE_MANAGEMENT';
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_UPDATE_MANAGEMENT', 'IHR_DIR', '人员调整管理', 'bi-arrow-repeat', 1, '/ihr/adjustment-management', 'ihr:update:query',
        4, 1, 1, '查看调整员工及企微同步状态', NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    DELETE FROM bc_sports_sys_menu WHERE id = 'IHR_UPDATE_SYNC';
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'IHR_UPDATE_SYNC', 'IHR_UPDATE_MANAGEMENT', '同步操作', NULL, 2, NULL, 'ihr:update:sync',
        1, 1, 0, NULL, NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    DELETE FROM bc_sports_sys_role_menu WHERE role_id = '1'
        AND menu_id IN ('IHR_UPDATE_MANAGEMENT', 'IHR_UPDATE_SYNC');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', 'IHR_UPDATE_MANAGEMENT', SYSTIMESTAMP, 'admin');
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES (SYS_GUID(), '1', 'IHR_UPDATE_SYNC',          SYSTIMESTAMP, 'admin');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('IHR 人员调整管理菜单权限部署完成');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('部署失败: ' || SQLERRM);
        RAISE;
END;
/
