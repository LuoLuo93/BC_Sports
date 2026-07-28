-- ========================================================
-- 实体渠道配置表 唯一约束迁移脚本
-- 表名: bc_sports_sys_entity_channel
-- 背景: 业务已无"客户"概念，只剩"店仓"。一个店铺可挂多个品牌，
--       唯一行定义为 (external_id, brand_id)。
-- 存量处理: entity_type='customer' 的历史记录保留展示，不做任何处理；
--          导入只匹配 store 记录。
-- 创建时间: 2026-07-28
--
-- ⚠️ 重要说明：
--   生产表当前【没有】任何业务唯一约束（init_entity_channel.sql 里声明的
--   uk_external_type 约束在生产库并不存在，建表时未创建）。
--   因此本脚本无需 DROP，直接 ADD 新约束即可。
--
-- 执行顺序（严格按 Step 1 → 2 → 3 → 4，任一步有数据都不可跳过）：
--   Step 1: 检查存量重复
--   Step 2: 清理重复（保留每组最新一条，其余软删）
--   Step 3: 复核无重复
--   Step 4: 添加唯一约束 + 更新注释
-- ========================================================


-- ╔══════════════════════════════════════════════════════════╗
-- ║  Step 1：检查存量数据是否违反新唯一性（只读，先跑看结果）   ║
-- ╚══════════════════════════════════════════════════════════╝
-- 期望结果：0 行。若有结果，说明同一 (external_id, brand_id) 存在多条未删除记录，
-- 必须执行 Step 2 清理后才能加约束，否则 ADD 会报 ORA-00001。
SELECT external_id, brand_id, COUNT(*) AS cnt
FROM bc_sports_sys_entity_channel
WHERE deleted = 0
GROUP BY external_id, brand_id
HAVING COUNT(*) > 1
ORDER BY cnt DESC;


-- ╔══════════════════════════════════════════════════════════╗
-- ║  Step 2：清理重复（保留每组最新一条，其余软删）            ║
-- ║  仅在 Step 1 有结果时执行。用 ROWID 精确定位，避免元组 IN。 ║
-- ╚══════════════════════════════════════════════════════════╝
-- 保留规则：同一 (external_id, brand_id) 组内，按 update_time 倒序取第 1 条，
--          其余（rn > 1）置 deleted = 1。
--          update_time 为空时按 id 兜底排序（id 来自序列，大体时序递增）。
UPDATE bc_sports_sys_entity_channel
SET deleted = 1, update_time = SYSDATE
WHERE rowid IN (
    SELECT rid FROM (
        SELECT rowid AS rid,
               ROW_NUMBER() OVER (
                   PARTITION BY external_id, brand_id
                   ORDER BY update_time DESC NULLS LAST, id DESC
               ) AS rn
        FROM bc_sports_sys_entity_channel
        WHERE deleted = 0
    )
    WHERE rn > 1
);
COMMIT;


-- ╔══════════════════════════════════════════════════════════╗
-- ║  Step 3：复核清理结果（只读，确认已无重复）                ║
-- ╚══════════════════════════════════════════════════════════╝
-- 期望结果：0 行。若仍有结果，说明 Step 2 未执行或失败，勿继续。
SELECT external_id, brand_id, COUNT(*) AS cnt
FROM bc_sports_sys_entity_channel
WHERE deleted = 0
GROUP BY external_id, brand_id
HAVING COUNT(*) > 1;


-- ╔══════════════════════════════════════════════════════════╗
-- ║  Step 4：添加唯一约束 + 更新注释                           ║
-- ║  仅在 Step 3 结果为空（0 行）后执行。                       ║
-- ╚══════════════════════════════════════════════════════════╝
ALTER TABLE bc_sports_sys_entity_channel ADD CONSTRAINT uk_external_brand UNIQUE (external_id, brand_id);

COMMENT ON TABLE bc_sports_sys_entity_channel IS '实体渠道配置表（店铺的渠道属性管理；一店铺+一品牌唯一一行，可多品牌）';
COMMENT ON COLUMN bc_sports_sys_entity_channel.entity_type IS '实体类型：store-店仓（customer-客户仅存量兼容，不再产生新数据）';

COMMIT;
