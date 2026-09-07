-- ==========================================================
-- 数仓销售导入 - BI_DW 侧脚本（需用 BI_DW 身份执行）
-- 1. ODS_SALES_MAIN 新增 PROMOTION_NAME(促销名称) 列
-- 2. BILL_ID / ITEM_ID 取号序列(高位偏移, 避免与ETL源表ID撞号)
-- 3. 编辑定位索引(BILL_NO+ITEM_ID, 编辑功能行定位用)
-- 幂等，可重复执行
-- ==========================================================
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE;

-- 1. PROMOTION_NAME 列
DECLARE
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_cnt FROM user_tab_columns
   WHERE table_name = 'ODS_SALES_MAIN' AND column_name = 'PROMOTION_NAME';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE BI_DW.ODS_SALES_MAIN ADD PROMOTION_NAME VARCHAR2(1000)';
    EXECUTE IMMEDIATE 'COMMENT ON COLUMN BI_DW.ODS_SALES_MAIN.PROMOTION_NAME IS ''促销名称''';
    DBMS_OUTPUT.PUT_LINE('PROMOTION_NAME added');
  ELSE
    DBMS_OUTPUT.PUT_LINE('PROMOTION_NAME already exists, skip');
  END IF;
END;
/

-- 2. 取号序列：起始 = 现有MAX + 10亿(高位偏移防ETL撞号)，CACHE 1000 提升批量取号吞吐
DECLARE
  v_max      NUMBER;
  v_start    NUMBER;
  v_cnt      NUMBER;
BEGIN
  -- ITEM_ID 序列(每行一号)
  SELECT COUNT(*) INTO v_cnt FROM user_sequences WHERE sequence_name = 'SEQ_ODS_SALES_MAIN_ITEM';
  IF v_cnt = 0 THEN
    SELECT NVL(MAX(ITEM_ID), 0) INTO v_max FROM BI_DW.ODS_SALES_MAIN;
    v_start := v_max + 1000000000;
    EXECUTE IMMEDIATE 'CREATE SEQUENCE BI_DW.SEQ_ODS_SALES_MAIN_ITEM START WITH ' || v_start ||
                      ' INCREMENT BY 1 CACHE 1000 NOCYCLE';
    DBMS_OUTPUT.PUT_LINE('SEQ_ODS_SALES_MAIN_ITEM created, start=' || v_start);
  ELSE
    DBMS_OUTPUT.PUT_LINE('SEQ_ODS_SALES_MAIN_ITEM already exists, skip');
  END IF;

  -- BILL_ID 序列(按单据号分组共号)
  SELECT COUNT(*) INTO v_cnt FROM user_sequences WHERE sequence_name = 'SEQ_ODS_SALES_MAIN_BILL';
  IF v_cnt = 0 THEN
    SELECT NVL(MAX(BILL_ID), 0) INTO v_max FROM BI_DW.ODS_SALES_MAIN;
    v_start := v_max + 1000000000;
    EXECUTE IMMEDIATE 'CREATE SEQUENCE BI_DW.SEQ_ODS_SALES_MAIN_BILL START WITH ' || v_start ||
                      ' INCREMENT BY 1 CACHE 1000 NOCYCLE';
    DBMS_OUTPUT.PUT_LINE('SEQ_ODS_SALES_MAIN_BILL created, start=' || v_start);
  ELSE
    DBMS_OUTPUT.PUT_LINE('SEQ_ODS_SALES_MAIN_BILL already exists, skip');
  END IF;
END;
/

-- 3. 编辑定位索引(BILL_NO+ITEM_ID 为编辑功能的行定位键，避免大表编辑全表扫)
DECLARE
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_cnt FROM user_indexes WHERE index_name = 'IDX_SALES_MAIN_EDITKEY';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX BI_DW.IDX_SALES_MAIN_EDITKEY ON BI_DW.ODS_SALES_MAIN (BILL_NO, ITEM_ID) ONLINE';
    DBMS_OUTPUT.PUT_LINE('IDX_SALES_MAIN_EDITKEY created');
  ELSE
    DBMS_OUTPUT.PUT_LINE('IDX_SALES_MAIN_EDITKEY already exists, skip');
  END IF;
END;
/

COMMIT;
EXIT;
