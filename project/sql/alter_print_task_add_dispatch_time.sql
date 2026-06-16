-- ============================================================
-- print_task 增加 dispatch_time（真实派发时间）
-- 背景：原来用 create_time 近似判断"卡住"，排队等待时间会被错误计入阈值，
--      可能误重派"正在打印"的任务，造成重复打印。改用 dispatch_time 从派发时刻开始计时。
-- ============================================================

ALTER TABLE print_task ADD (dispatch_time TIMESTAMP);

-- 回填：已有任务用 create_time 近似，避免 NULL 导致重派逻辑漏判（NULL < 阈值 在 Oracle 中为 NULL，不命中）
UPDATE print_task SET dispatch_time = create_time WHERE dispatch_time IS NULL;

COMMENT ON COLUMN print_task.dispatch_time IS '派发时间（Agent 拉取并置为打印中的时刻）';

-- 辅助巡检查询（按 status + dispatch_time 过滤长时间未回报的任务）
CREATE INDEX idx_print_task_dispatch ON print_task (status, dispatch_time);

COMMIT;

-- ============================================================
-- 可选：种子化"巡检卡住任务"定时任务（每 5 分钟执行）
-- 也可不执行此段，改为在后台「定时任务」界面手动新建（与 checkOffline 保持一致的配置方式）：
--   预设任务 = 贴纸打印-巡检卡住任务，cron = */5 * * * *，启用
-- ============================================================
INSERT INTO bc_sports_sys_schedule_job
  (id, job_name, task_key, cron_expression, status, module, sort, remark, create_time, create_by, deleted)
SELECT 'SCH_TICKET_TASK_CHECK_STUCK', '贴纸打印-巡检卡住任务', 'ticket.task.checkStuck',
       '*/5 * * * *', 1, 'TICKET', 2,
       '每5分钟巡检长时间未拉取/未回报的打印任务并告警', SYSTIMESTAMP, 'admin', 0
  FROM DUAL
  WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_schedule_job WHERE task_key = 'ticket.task.checkStuck');

COMMIT;
