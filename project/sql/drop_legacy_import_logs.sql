-- ============================================================
-- R01 阶段 5：DROP 旧 7 张导入日志表 + 清理搬迁按钮权限
-- 前置条件（必须全部满足后才执行本脚本）：
--   1. 新版应用已上线，全部 7 个导入模块日志均写/读 BC_SPORTS_SYS_IMPORT_LOG；
--   2. 历史搬迁已完成并核对（阶段 3 收尾搬迁后：新表各模块 LEGACY_ID 非空行数 == 旧表行数，下方核对查询）；
--   3. 旧表已静置一个版本周期无人反馈日志缺失。
-- 执行位置：主库(BC_SPORTS, Oracle)。企微库的 Bas_FirstAdd_ImportLog 单独联系 DBA 处理（见文末）。
-- 幂等性：核对查询只读；DROP 前有存在性判断，可重复执行。
-- ============================================================
SET SERVEROUTPUT ON;

-- ========== 第 1 步：DROP 前行数核对（只读，人工比对输出） ==========
-- 新表各模块搬迁行数：
SELECT IMPORT_TYPE, COUNT(*) AS migrated
  FROM BC_SPORTS_SYS_IMPORT_LOG
 WHERE LEGACY_ID IS NOT NULL
 GROUP BY IMPORT_TYPE
 ORDER BY IMPORT_TYPE;

-- 旧表行数（6 张主库表；企微库那张单独查）：
SELECT 'GOODS_OLD_NEW' t, COUNT(*) FROM BC_SPORTS_GOODS_IMPORT_LOG
UNION ALL SELECT 'SALES_BUDGET', COUNT(*) FROM BC_SPORTS_BUDGET_LOG
UNION ALL SELECT 'DW_SALES', COUNT(*) FROM BC_SPORTS_DW_SALES_IMPORT_LOG
UNION ALL SELECT 'ESTIMATED_COST', COUNT(*) FROM BC_SPORTS_ESTIMATED_COST_IMPORT_LOG
UNION ALL SELECT 'STICKER_DATA', COUNT(*) FROM STICKER_DATA_IMPORT_LOG
UNION ALL SELECT 'STICKER_SIZE_GROUP', COUNT(*) FROM STICKER_SIZE_GROUP_IMPORT_LOG;
-- 期望：每个模块新表 migrated == 旧表行数（STICKER_DATA 旧表为空属正常）。
-- 企微库核对：SELECT COUNT(*) FROM Bas_FirstAdd_ImportLog;  应等于新表 BAS_FIRST_ADD 的 migrated。

-- ========== 第 2 步：DROP 旧 6 张主库日志表 ==========
DECLARE
  PROCEDURE drop_if_exists(p_table VARCHAR2) IS
    v_cnt NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_cnt FROM user_tables WHERE table_name = p_table;
    IF v_cnt > 0 THEN
      EXECUTE IMMEDIATE 'DROP TABLE ' || p_table;
      DBMS_OUTPUT.PUT_LINE('已 DROP: ' || p_table);
    ELSE
      DBMS_OUTPUT.PUT_LINE('不存在, 跳过: ' || p_table);
    END IF;
  END;
BEGIN
  drop_if_exists('BC_SPORTS_GOODS_IMPORT_LOG');
  drop_if_exists('BC_SPORTS_BUDGET_LOG');
  drop_if_exists('BC_SPORTS_DW_SALES_IMPORT_LOG');
  drop_if_exists('BC_SPORTS_ESTIMATED_COST_IMPORT_LOG');
  drop_if_exists('STICKER_DATA_IMPORT_LOG');
  drop_if_exists('STICKER_SIZE_GROUP_IMPORT_LOG');
END;
/

-- ========== 第 3 步：清理一次性搬迁按钮权限（代码已随阶段 4 删除） ==========
DELETE FROM BC_SPORTS_SYS_ROLE_MENU WHERE MENU_ID = 'SYS_IMPORT_LOG_MIGRATE';
DELETE FROM BC_SPORTS_SYS_MENU WHERE ID = 'SYS_IMPORT_LOG_MIGRATE';
COMMIT;
DBMS_OUTPUT.PUT_LINE('搬迁按钮权限已清理');

-- ============================================================
-- 第 4 步（企微库 BC_SPORTS_QYWX, SQL Server 单独执行）：
--   Bas_FirstAdd_ImportLog 历史已迁入统一表，核对无误后：
--   -- SQL Server: SELECT COUNT(*) FROM Bas_FirstAdd_ImportLog;  对数后
--   -- DROP TABLE Bas_FirstAdd_ImportLog;
-- ============================================================
