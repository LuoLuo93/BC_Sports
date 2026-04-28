
-- 在 Oracle 主数据库中执行
-- 插入 IHR 排除管理菜单数据

DECLARE
    v_parent_id VARCHAR(64);
BEGIN
    -- 检查父菜单是否存在
    BEGIN
        SELECT id INTO v_parent_id FROM bc_sports_sys_menu WHERE id = 'ihr_menu_parent';
        DBMS_OUTPUT.PUT_LINE('父菜单已存在');
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            -- 插入 IHR 管理父菜单
            INSERT INTO bc_sports_sys_menu (
                id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, deleted
            ) VALUES (
                'ihr_menu_parent', '0', 'IHR管理', 'bi-people', 1, '/ihr', NULL, 100, 1, 1, SYSDATE, 0
            );
            DBMS_OUTPUT.PUT_LINE('父菜单创建成功');
    END;

    -- 插入入职排除子菜单
    BEGIN
        INSERT INTO bc_sports_sys_menu (
            id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, deleted
        ) VALUES (
            'ihr_onboarding_exclusion', 'ihr_menu_parent', '入职排除', 'bi-person-plus', 1, '/ihr/onboarding-exclusion', NULL, 1, 1, 1, SYSDATE, 0
        );
        DBMS_OUTPUT.PUT_LINE('入职排除菜单创建成功');
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            DBMS_OUTPUT.PUT_LINE('入职排除菜单已存在');
    END;

    -- 插入离职排除子菜单
    BEGIN
        INSERT INTO bc_sports_sys_menu (
            id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, deleted
        ) VALUES (
            'ihr_leaving_exclusion', 'ihr_menu_parent', '离职排除', 'bi-person-dash', 1, '/ihr/leaving-exclusion', NULL, 2, 1, 1, SYSDATE, 0
        );
        DBMS_OUTPUT.PUT_LINE('离职排除菜单创建成功');
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            DBMS_OUTPUT.PUT_LINE('离职排除菜单已存在');
    END;

    -- 插入按钮权限
    BEGIN
        INSERT INTO bc_sports_sys_menu (
            id, parent_id, menu_name, menu_type, permission, sort, status, visible, create_time, deleted
        ) VALUES (
            'ihr_exclusion_query', 'ihr_menu_parent', '查询', 2, 'ihr:exclusion:query', 1, 1, 0, SYSDATE, 0
        );
        DBMS_OUTPUT.PUT_LINE('查询权限创建成功');
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            DBMS_OUTPUT.PUT_LINE('查询权限已存在');
    END;

    BEGIN
        INSERT INTO bc_sports_sys_menu (
            id, parent_id, menu_name, menu_type, permission, sort, status, visible, create_time, deleted
        ) VALUES (
            'ihr_exclusion_add', 'ihr_menu_parent', '新增', 2, 'ihr:exclusion:add', 2, 1, 0, SYSDATE, 0
        );
        DBMS_OUTPUT.PUT_LINE('新增权限创建成功');
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            DBMS_OUTPUT.PUT_LINE('新增权限已存在');
    END;

    BEGIN
        INSERT INTO bc_sports_sys_menu (
            id, parent_id, menu_name, menu_type, permission, sort, status, visible, create_time, deleted
        ) VALUES (
            'ihr_exclusion_edit', 'ihr_menu_parent', '编辑', 2, 'ihr:exclusion:edit', 3, 1, 0, SYSDATE, 0
        );
        DBMS_OUTPUT.PUT_LINE('编辑权限创建成功');
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            DBMS_OUTPUT.PUT_LINE('编辑权限已存在');
    END;

    BEGIN
        INSERT INTO bc_sports_sys_menu (
            id, parent_id, menu_name, menu_type, permission, sort, status, visible, create_time, deleted
        ) VALUES (
            'ihr_exclusion_delete', 'ihr_menu_parent', '删除', 2, 'ihr:exclusion:delete', 4, 1, 0, SYSDATE, 0
        );
        DBMS_OUTPUT.PUT_LINE('删除权限创建成功');
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            DBMS_OUTPUT.PUT_LINE('删除权限已存在');
    END;

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('所有菜单数据创建完成');
END;
/
