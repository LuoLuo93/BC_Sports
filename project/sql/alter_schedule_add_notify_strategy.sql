-- ==========================================================
-- 定时任务表 - 新增推送策略和参数字段
-- ==========================================================

-- Oracle 版本
ALTER TABLE bc_sports_sys_schedule_job ADD (
    notify_strategy VARCHAR2(20),
    PARAMS VARCHAR2(2000)
);

COMMENT ON COLUMN bc_sports_sys_schedule_job.notify_strategy IS '推送策略(ALWAYS/FAIL_ONLY/DISABLED)';
COMMENT ON COLUMN bc_sports_sys_schedule_job.PARAMS IS '任务参数(JSON)';

-- SQL Server 版本 (如果需要):
-- ALTER TABLE bc_sports_sys_schedule_job ADD notify_strategy NVARCHAR(20);
-- ALTER TABLE bc_sports_sys_schedule_job ADD PARAMS NVARCHAR(2000);
-- EXEC sp_addextendedproperty 'MS_Description', '推送策略(ALWAYS/FAIL_ONLY/DISABLED)', 'SCHEMA', 'dbo', 'TABLE', 'bc_sports_sys_schedule_job', 'COLUMN', 'notify_strategy';
-- EXEC sp_addextendedproperty 'MS_Description', '任务参数(JSON)', 'SCHEMA', 'dbo', 'TABLE', 'bc_sports_sys_schedule_job', 'COLUMN', 'PARAMS';

COMMIT;
