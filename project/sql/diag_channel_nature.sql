-- ========================================================
-- 诊断脚本：定位"第一次新增成功、第二次失败"的根因
-- 跑完把 3 个查询的结果贴出来
-- ========================================================

-- 1. 表里现有的所有 id（看清楚是数字还是字符串，最大值是多少）
SELECT id, nature_name, parent_id, deleted, create_time
FROM bc_sports_sys_channel_nature
ORDER BY create_time DESC;

-- 2. 表里最大数字 id
SELECT NVL(MAX(TO_NUMBER(id)), 0) AS max_num_id, COUNT(*) AS total_rows
FROM bc_sports_sys_channel_nature
WHERE REGEXP_LIKE(id, '^\d+$');

-- 3. 序列当前值（LAST_NUMBER 是下次 NEXTVAL 会返回的值）
SELECT sequence_name, last_number, min_value, max_value, increment_by, cache_size, cycle_flag
FROM user_sequences
WHERE sequence_name = 'BC_SPORTS_SEQ_CHANNEL_NATURE';
