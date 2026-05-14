-- ==========================================================
-- 人员入职管理 - 同步状态表
-- 目标数据库: SQL Server > BC_SPORTS_IHR
-- 说明: 记录每个入职员工同步到企微的状态
--       只在企微同步任务时写入，IHR增量同步不触碰此表
-- ==========================================================

USE [BC_SPORTS_IHR];
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[ihr_employee_onboarding_status]') AND type IN (N'U'))
BEGIN
    CREATE TABLE [dbo].[ihr_employee_onboarding_status] (
        [id]                BIGINT          IDENTITY(1,1)   PRIMARY KEY,
        [employees_id]      NVARCHAR(50)    NOT NULL,
        [staff_name]        NVARCHAR(100)   NULL,
        [staff_no]          NVARCHAR(50)    NULL,
        [sync_status]       INT             NOT NULL DEFAULT 0,
        [sync_time]         DATETIME        NULL,
        [error_message]     NVARCHAR(500)   NULL,
        [create_time]       DATETIME        DEFAULT GETDATE(),
        [update_time]       DATETIME        DEFAULT GETDATE()
    );
    PRINT 'Table ihr_employee_onboarding_status created';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'[dbo].[ihr_employee_onboarding_status]') AND name = N'idx_onboarding_employees_id')
    CREATE UNIQUE INDEX [idx_onboarding_employees_id] ON [dbo].[ihr_employee_onboarding_status] ([employees_id]);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'[dbo].[ihr_employee_onboarding_status]') AND name = N'idx_onboarding_sync_status')
    CREATE INDEX [idx_onboarding_sync_status] ON [dbo].[ihr_employee_onboarding_status] ([sync_status]);
GO

PRINT 'ihr_employee_onboarding_status table deployed';
GO
