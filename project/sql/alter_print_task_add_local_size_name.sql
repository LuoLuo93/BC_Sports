-- ============================================================
-- print_task 表新增 local_size_name 列（修正尺码/矫正尺码）
-- 用于任务记录列表/详情展示修正尺码，空则回退展示 size_name(ERP原始尺码)
-- ============================================================

ALTER TABLE print_task ADD (local_size_name VARCHAR2(50));

COMMENT ON COLUMN print_task.local_size_name IS '修正尺码(矫正尺码，空则展示size_name)';

COMMIT;
