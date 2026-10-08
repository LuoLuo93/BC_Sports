-- ==========================================================
-- 数仓库存查询 - 菜单脚本（查询 BI_DW.DWD_STOCK_DAILY）
-- 菜单: 挂在 BI管理(BI_DIR) 下, 仅查询权限, 无按钮权限
-- 用 BC_SPORTS 身份执行(菜单表在 bc_sports schema)
-- 幂等: WHERE NOT EXISTS, 重跑跳过
-- ==========================================================
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE;

-- 1. 页面菜单 (menu_type=1)
INSERT INTO BC_SPORTS_SYS_MENU
  (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE,
   DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
SELECT 'BI_DW_STOCK', 'BI_DIR', '数仓库存查询', 'bi-archive', 1,
       '/bi/dw-stock', 'bi:dw-stock:query', 52, 1, 1,
       '每日库存查询(DWD_STOCK_DAILY,月结期末+流水累加口径,T+1)', NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM BC_SPORTS_SYS_MENU WHERE ID = 'BI_DW_STOCK');

-- 2. 授权给超管角色 (role_id='1')
INSERT INTO BC_SPORTS_SYS_ROLE_MENU (ID, ROLE_ID, MENU_ID, CREATE_TIME, CREATE_BY)
SELECT RAWTOHEX(SYS_GUID()), '1', 'BI_DW_STOCK', SYSTIMESTAMP, 'admin'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM BC_SPORTS_SYS_ROLE_MENU WHERE ROLE_ID = '1' AND MENU_ID = 'BI_DW_STOCK');

COMMIT;

EXIT;
