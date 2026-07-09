-- 打印任务批次：同一次下发(createTasksFromOrder)或一次补打(reprintTask)的任务共享同一 batch_id
-- 用于在任务记录中区分"同一批次"，便于排查集中报错。

ALTER TABLE print_task ADD (
    batch_id   VARCHAR2(64)    -- 打印批次ID，同批任务共享；历史数据为 NULL
);

COMMENT ON COLUMN print_task.batch_id IS '打印批次ID，同一次下发/补打的任务共享，NULL为历史数据';
