-- ========================================================
-- 重置 4 个序列的起始值为 max(id)+1（解决序列与表数据不同步）
-- 背景：序列 START WITH 1，但表里已有 id 到 40/105/45/45 的数据，
--       导致 NEXTVAL 返回已占用的 id，新增触发 ORA-00001 主键冲突。
-- 本脚本用纯 SQL（DROP + CREATE），图形客户端可直接逐条执行，无需匿名块。
-- 已知 max(id)：BRAND=40, REGION=105, CHANNEL_TYPE=45, CHANNEL_NATURE=45
-- 执行前请确认上面 max 值仍有效（如有新增数据需重新核算）
-- 创建时间: 2026-07-28
-- ========================================================

-- 1. 品牌 brand（max id = 40 → 重置为 41）
DROP SEQUENCE bc_sports_seq_brand;
CREATE SEQUENCE bc_sports_seq_brand START WITH 41 INCREMENT BY 1 NOCACHE NOCYCLE;

-- 2. 地区 region（max id = 105 → 重置为 106）
DROP SEQUENCE bc_sports_seq_region;
CREATE SEQUENCE bc_sports_seq_region START WITH 106 INCREMENT BY 1 NOCACHE NOCYCLE;

-- 3. 渠道类型 channel_type（max id = 45 → 重置为 46）
DROP SEQUENCE bc_sports_seq_channel_type;
CREATE SEQUENCE bc_sports_seq_channel_type START WITH 46 INCREMENT BY 1 NOCACHE NOCYCLE;

-- 4. 渠道性质 channel_nature（max id = 45 → 重置为 46）
DROP SEQUENCE bc_sports_seq_channel_nature;
CREATE SEQUENCE bc_sports_seq_channel_nature START WITH 46 INCREMENT BY 1 NOCACHE NOCYCLE;

COMMIT;

-- 验证：确认 4 个序列的 LAST_NUMBER 都大于对应表的 max(id)
-- SELECT sequence_name, last_number FROM user_sequences
-- WHERE sequence_name IN ('BC_SPORTS_SEQ_BRAND','BC_SPORTS_SEQ_REGION',
--   'BC_SPORTS_SEQ_CHANNEL_TYPE','BC_SPORTS_SEQ_CHANNEL_NATURE');
-- 期望：BRAND=41, REGION=106, CHANNEL_TYPE=46, CHANNEL_NATURE=46
