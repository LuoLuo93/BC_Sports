-- ==========================================================
-- 数仓销售查看 - 菜单脚本（查询 BI_DW.ODS_SALES_MAIN）
-- 菜单: 挂在 BI管理(BI_DIR) 下, 仅查询权限, 无按钮权限
-- 第1部分: 用 BC_SPORTS 身份执行(菜单表在 bc_sports schema)
-- 第2部分: 用 BI_DW 身份执行(可选索引, 加速提交时间/单据号查询)
-- ==========================================================
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE;

-- ==========================================================
-- 1. 页面菜单 (menu_type=1) + 授权给超管 role_id='1'
-- ==========================================================

-- 1.1 页面菜单
INSERT INTO BC_SPORTS_SYS_MENU
  (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE,
   DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
SELECT 'BI_DW_SALES', 'BI_DIR', '数仓销售查看', 'bi-table', 1,
       '/bi/dw-sales', 'bi:dw-sales:query', 50, 1, 1,
       '销售主明细ODS合并表查询(ODS_SALES_MAIN)', NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM BC_SPORTS_SYS_MENU WHERE ID = 'BI_DW_SALES');

-- 1.2 编辑按钮权限 (menu_type=2, 不在侧边栏显示 visible=0)
INSERT INTO BC_SPORTS_SYS_MENU
  (ID, PARENT_ID, MENU_NAME, ICON, MENU_TYPE, PATH, PERMISSION, SORT, STATUS, VISIBLE,
   DESCRIPTION, ICON_COLOR, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED)
SELECT 'BI_DW_SALES_EDIT', 'BI_DW_SALES', '编辑', NULL, 2,
       NULL, 'bi:dw-sales:edit', 1, 1, 0,
       '编辑销售明细归属维度字段', NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM BC_SPORTS_SYS_MENU WHERE ID = 'BI_DW_SALES_EDIT');

-- 1.3 授权给超管角色 (role_id='1')
INSERT INTO BC_SPORTS_SYS_ROLE_MENU (ID, ROLE_ID, MENU_ID, CREATE_TIME, CREATE_BY)
SELECT RAWTOHEX(SYS_GUID()), '1', 'BI_DW_SALES', SYSTIMESTAMP, 'admin'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM BC_SPORTS_SYS_ROLE_MENU WHERE ROLE_ID = '1' AND MENU_ID = 'BI_DW_SALES');

INSERT INTO BC_SPORTS_SYS_ROLE_MENU (ID, ROLE_ID, MENU_ID, CREATE_TIME, CREATE_BY)
SELECT RAWTOHEX(SYS_GUID()), '1', 'BI_DW_SALES_EDIT', SYSTIMESTAMP, 'admin'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM BC_SPORTS_SYS_ROLE_MENU WHERE ROLE_ID = '1' AND MENU_ID = 'BI_DW_SALES_EDIT');

COMMIT;

-- ==========================================================
-- 2. 可选: 查询索引(BI_DW schema 执行, 用 BI_DW 身份)
--    ODS_SALES_MAIN 为 CTAS 新建表无索引, 明细数据量大,
--    按提交时间范围/单据号查询前建议执行, 否则全表扫
-- ==========================================================
-- CREATE INDEX BI_DW.IDX_ODS_SALES_MAIN_TIME ON BI_DW.ODS_SALES_MAIN (BILL_TIME, BILL_NO) ONLINE;

EXIT;
