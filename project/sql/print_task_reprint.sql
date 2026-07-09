-- 打印任务补打功能：增加补打来源标记字段
-- 补打任务作为新记录插入（is_reprint=1），通过 source_task_id 关联原任务，原任务保持不动。

ALTER TABLE print_task ADD (
    source_task_id   VARCHAR2(64),               -- 原任务ID（补打来源；NULL 表示原始任务）
    is_reprint       NUMBER(1)      DEFAULT 0,    -- 是否补打任务：0 否 / 1 是
    reprint_reason   VARCHAR2(500)               -- 补打原因（审计）
);

COMMENT ON COLUMN print_task.source_task_id IS '补打来源任务ID，NULL为原始任务';
COMMENT ON COLUMN print_task.is_reprint     IS '是否补打任务：0否 1是';
COMMENT ON COLUMN print_task.reprint_reason IS '补打原因';
