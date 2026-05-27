-- ======================================================
-- BC体育 - 系统监控菜单初始化脚本
-- ======================================================
-- 前置条件：MONITOR_DIR（运维监控）目录已存在
-- 功能：新增"系统监控"菜单 + 为管理员角色授权

BEGIN
    -- 插入系统监控菜单（在运维监控目录下，sort=2 排在定时任务之后）
    INSERT INTO bc_sports_sys_menu (
        id, parent_id, menu_name, icon, menu_type, path, permission,
        sort, status, visible, description, icon_color,
        create_time, update_time, create_by, update_by, deleted
    ) VALUES (
        'MONITOR_SYSTEM', 'MONITOR_DIR', '系统监控', 'bi-heart-pulse', 1, '/monitor/system', 'monitor:system:query',
        2, 1, 1, '应用健康状态、JVM监控、日志管理', NULL,
        SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
    );

    -- 为管理员角色 (Role ID = 1) 授权
    INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
    VALUES (SYS_GUID(), '1', 'MONITOR_SYSTEM', SYSTIMESTAMP, 'admin');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('系统监控菜单初始化成功！');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/
