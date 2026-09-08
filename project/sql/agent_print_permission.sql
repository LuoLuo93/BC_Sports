-- ============================================================
-- 打印任务下发按钮权限 — 配合 PrintController/AgentController 权限注解收口
-- 按钮权限 agent:print:dispatch(下发/取消/补打) 挂在「Agent 监控」(STICKER_AGENT)菜单下。
-- Agent 监控页查询类接口复用现有 sticker:agent:query(建菜单时已授权，无需新增)。
-- 幂等：已存在则跳过。主库(BC_SPORTS)执行。
-- ============================================================

-- 1. 按钮权限 (menu_type=2, 不在侧边栏显示 visible=0)
INSERT INTO BC_SPORTS_SYS_MENU
  (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE,
   DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
SELECT 'STICKER_AGENT_PRINT_DISPATCH', 'STICKER_AGENT', '打印下发/取消/补打', NULL, 2,
       NULL, 'agent:print:dispatch', 1, 1, 0,
       '下发打印任务、取消任务、补打(写操作)', NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM BC_SPORTS_SYS_MENU WHERE ID = 'STICKER_AGENT_PRINT_DISPATCH');

-- 2. 授权给超管角色 (role_id='1')
INSERT INTO BC_SPORTS_SYS_ROLE_MENU (ID, ROLE_ID, MENU_ID, CREATE_TIME, CREATE_BY)
SELECT RAWTOHEX(SYS_GUID()), '1', 'STICKER_AGENT_PRINT_DISPATCH', SYSTIMESTAMP, 'admin'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM BC_SPORTS_SYS_ROLE_MENU
                    WHERE ROLE_ID = '1' AND MENU_ID = 'STICKER_AGENT_PRINT_DISPATCH');

COMMIT;
