-- ============================================================
-- sticker_sizes 尺码编码组内唯一约束
-- 业务规则：同一尺码组(group_id)内 size_code 唯一
-- 部署顺序：先执行本脚本 → 再部署后端(已加 Service 层校验)
-- ============================================================

-- 1. 清理历史脏数据：同一 group_id + size_code 重复的，保留 create_time 最早的一条，其余软删
--    （逻辑删除 deleted=1，不物理删除，可追溯）
UPDATE sticker_sizes s
SET s.deleted = 1
WHERE s.deleted = 0
  AND s.rowid <> (
      SELECT MIN(t.rowid)
      FROM sticker_sizes t
      WHERE t.group_id = s.group_id
        AND t.size_code = s.size_code
        AND t.deleted = 0
  );

COMMIT;

-- 2. 建组内 size_code 唯一索引（含 deleted 维度，避免软删记录占用唯一槽位）
CREATE UNIQUE INDEX uk_sticker_sizes_group_code
    ON sticker_sizes (group_id, size_code, deleted);

COMMIT;
