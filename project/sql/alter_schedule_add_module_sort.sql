-- ==========================================================
-- 定时任务表 - 新增模块和排序字段
-- ==========================================================

-- Oracle 版本
ALTER TABLE bc_sports_sys_schedule_job ADD (
    module VARCHAR2(50),
    sort NUMBER(10) DEFAULT 0
);

COMMENT ON COLUMN bc_sports_sys_schedule_job.module IS '任务模块(IHR/QW/DEMO/OTHER)';
COMMENT ON COLUMN bc_sports_sys_schedule_job.sort IS '排序号';

-- SQL Server 版本 (如果需要):
-- ALTER TABLE bc_sports_sys_schedule_job ADD module NVARCHAR(50);
-- ALTER TABLE bc_sports_sys_schedule_job ADD sort INT DEFAULT 0;
-- EXEC sp_addextendedproperty 'MS_Description', '任务模块(IHR/QW/DEMO/OTHER)', 'SCHEMA', 'dbo', 'TABLE', 'bc_sports_sys_schedule_job', 'COLUMN', 'module';
-- EXEC sp_addextendedproperty 'MS_Description', '排序号', 'SCHEMA', 'dbo', 'TABLE', 'bc_sports_sys_schedule_job', 'COLUMN', 'sort';

COMMIT;
