-- ============================================================
-- 品牌模板关系 - 批量导入按钮权限 + 超管授权
-- 幂等：重复执行不会重复插入
-- ============================================================

-- 1. 新增「导入」按钮权限，挂在品牌模板关系页面菜单下
--    parent_id 通过子查询动态取现有页面菜单 id（permission = 'sticker:brand-template:query' 且 menu_type=1）
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission,
    sort, status, visible, description, create_time, update_time, create_by, update_by, deleted)
SELECT 'BRAND_TEMPLATE_IMPORT',
       (SELECT id FROM bc_sports_sys_menu
          WHERE permission = 'sticker:brand-template:query' AND menu_type = 1
            AND deleted = 0
          FETCH FIRST 1 ROWS ONLY),
       '导入', NULL, 2, NULL, 'sticker:brand-template:import',
       5, 1, 0, '品牌模板关系批量导入',
       SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BRAND_TEMPLATE_IMPORT');

-- 2. 授权给超管角色 (role_id='1')
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'BRAND_TEMPLATE_IMPORT', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BRAND_TEMPLATE_IMPORT');
