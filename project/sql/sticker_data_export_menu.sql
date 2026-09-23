-- ============================================================
-- 贴纸资料维护 - 导出按钮权限 + 角色授权
-- 挂在「贴纸资料维护」菜单(STICKER_DATA)下
-- 幂等: 已存在则跳过
-- ============================================================

-- 按钮权限：导出（控制列表页「导出Excel」按钮与 /api/sticker/data/export 接口）
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission,
    sort, status, visible, description, create_time, update_time, create_by, update_by, deleted)
SELECT 'STICKER_DATA_EXPORT', 'STICKER_DATA', '导出', NULL, 2, NULL, 'sticker:data:export',
    5, 1, 0, '按当前查询条件导出贴纸资料Excel，条件为空即全量', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'STICKER_DATA_EXPORT');

-- 授权给超管角色 (role_id='1')
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT rawtohex(sys_guid()), '1', 'STICKER_DATA_EXPORT', SYSTIMESTAMP, 'admin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'STICKER_DATA_EXPORT');

COMMIT;
