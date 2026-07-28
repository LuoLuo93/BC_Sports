-- ========================================================
-- 修复序列起始值（解决 ORA-00001 主键冲突）
-- 背景：序列用 START WITH 1 创建，但表里已有 id=1,2,3... 的数据（手动初始化的基础数据），
--       导致新增时序列返回已占用的 id，触发主键冲突 bc_sports_pk_sys_xxx。
-- 本脚本：自动查每张表当前最大数字 id，把序列重置为 max(id)+1。
-- 原理：Oracle 序列不能直接改 START WITH，需 DROP 再 CREATE，或用 ALTER 的 INCREMENT  trick。
--       这里用安全的方式：DROP 后按 max(id)+1 重新 CREATE。
-- 创建时间: 2026-07-28
-- ========================================================

-- ============================================================
-- 先诊断：查每张表当前最大的数字 id（只读，先看清楚现状）
-- 把下面的查询结果记下来，确认 4 张表各自的最大值
-- ============================================================
-- 渠道性质
SELECT 'channel_nature' AS tbl, NVL(MAX(TO_NUMBER(REGEXP_SUBSTR(id, '^\d+'))), 0) AS max_num_id
FROM bc_sports_sys_channel_nature WHERE REGEXP_LIKE(id, '^\d+$');

-- 渠道类型
SELECT 'channel_type' AS tbl, NVL(MAX(TO_NUMBER(REGEXP_SUBSTR(id, '^\d+'))), 0) AS max_num_id
FROM bc_sports_sys_channel_type WHERE REGEXP_LIKE(id, '^\d+$');

-- 品牌
SELECT 'brand' AS tbl, NVL(MAX(TO_NUMBER(REGEXP_SUBSTR(id, '^\d+'))), 0) AS max_num_id
FROM bc_sports_sys_brand WHERE REGEXP_LIKE(id, '^\d+$');

-- 地区
SELECT 'region' AS tbl, NVL(MAX(TO_NUMBER(REGEXP_SUBSTR(id, '^\d+'))), 0) AS max_num_id
FROM bc_sports_sys_region WHERE REGEXP_LIKE(id, '^\d+$');


-- ============================================================
-- 修复：重置序列起始值为 max(id)+1
-- 用动态 SQL 读取每张表最大数字 id，DROP 旧序列后按 max+1 重建。
-- 用过程封装，一次执行全部 4 个序列。
-- ============================================================
DECLARE
    v_max_id NUMBER;
    v_seq_name VARCHAR2(50);
BEGIN
    -- ---------- 渠道性质 channel_nature ----------
    SELECT NVL(MAX(TO_NUMBER(id)), 0) INTO v_max_id
    FROM bc_sports_sys_channel_nature WHERE REGEXP_LIKE(id, '^\d+$');

    BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE bc_sports_seq_channel_nature';
    EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN RAISE; END IF; END;

    EXECUTE IMMEDIATE 'CREATE SEQUENCE bc_sports_seq_channel_nature START WITH ' || (v_max_id + 1) || ' INCREMENT BY 1 NOCACHE NOCYCLE';
    DBMS_OUTPUT.PUT_LINE('channel_nature: max_id=' || v_max_id || ', 序列重置为 ' || (v_max_id + 1));

    -- ---------- 渠道类型 channel_type ----------
    SELECT NVL(MAX(TO_NUMBER(id)), 0) INTO v_max_id
    FROM bc_sports_sys_channel_type WHERE REGEXP_LIKE(id, '^\d+$');

    BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE bc_sports_seq_channel_type';
    EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN RAISE; END IF; END;

    EXECUTE IMMEDIATE 'CREATE SEQUENCE bc_sports_seq_channel_type START WITH ' || (v_max_id + 1) || ' INCREMENT BY 1 NOCACHE NOCYCLE';
    DBMS_OUTPUT.PUT_LINE('channel_type: max_id=' || v_max_id || ', 序列重置为 ' || (v_max_id + 1));

    -- ---------- 品牌 brand ----------
    SELECT NVL(MAX(TO_NUMBER(id)), 0) INTO v_max_id
    FROM bc_sports_sys_brand WHERE REGEXP_LIKE(id, '^\d+$');

    BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE bc_sports_seq_brand';
    EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN RAISE; END IF; END;

    EXECUTE IMMEDIATE 'CREATE SEQUENCE bc_sports_seq_brand START WITH ' || (v_max_id + 1) || ' INCREMENT BY 1 NOCACHE NOCYCLE';
    DBMS_OUTPUT.PUT_LINE('brand: max_id=' || v_max_id || ', 序列重置为 ' || (v_max_id + 1));

    -- ---------- 地区 region ----------
    SELECT NVL(MAX(TO_NUMBER(id)), 0) INTO v_max_id
    FROM bc_sports_sys_region WHERE REGEXP_LIKE(id, '^\d+$');

    BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE bc_sports_seq_region';
    EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN RAISE; END IF; END;

    EXECUTE IMMEDIATE 'CREATE SEQUENCE bc_sports_seq_region START WITH ' || (v_max_id + 1) || ' INCREMENT BY 1 NOCACHE NOCYCLE';
    DBMS_OUTPUT.PUT_LINE('region: max_id=' || v_max_id || ', 序列重置为 ' || (v_max_id + 1));
END;
/
COMMIT;

-- 验证：确认 4 个序列的当前值
-- SELECT sequence_name, last_number FROM user_sequences
-- WHERE sequence_name IN ('BC_SPORTS_SEQ_CHANNEL_NATURE','BC_SPORTS_SEQ_CHANNEL_TYPE','BC_SPORTS_SEQ_BRAND','BC_SPORTS_SEQ_REGION');
