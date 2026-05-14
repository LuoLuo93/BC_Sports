-- ==========================================================
-- 人员离职管理 - 同步状态表
-- 目标数据库: SQL Server > BC_SPORTS_IHR
-- 说明: 记录每个离职员工同步到企微的状态
--       只在企微离职同步任务时写入，IHR增量同步不触碰此表
-- ==========================================================

USE [BC_SPORTS_IHR];
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[ihr_employee_leaving_status]') AND type IN (N'U'))
BEGIN
    CREATE TABLE [dbo].[ihr_employee_leaving_status] (
        [id]                BIGINT          IDENTITY(1,1)   PRIMARY KEY,
        [employee_id]       NVARCHAR(50)    NOT NULL,
        [staff_name]        NVARCHAR(100)   NULL,
        [staff_no]          NVARCHAR(50)    NULL,
        [sync_status]       INT             NOT NULL DEFAULT 0,
        [sync_time]         DATETIME        NULL,
        [error_message]     NVARCHAR(500)   NULL,
        [create_time]       DATETIME        DEFAULT GETDATE(),
        [update_time]       DATETIME        DEFAULT GETDATE()
    );
    PRINT 'Table ihr_employee_leaving_status created';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'[dbo].[ihr_employee_leaving_status]') AND name = N'idx_leaving_employee_id')
    CREATE UNIQUE INDEX [idx_leaving_employee_id] ON [dbo].[ihr_employee_leaving_status] ([employee_id]);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'[dbo].[ihr_employee_leaving_status]') AND name = N'idx_leaving_sync_status')
    CREATE INDEX [idx_leaving_sync_status] ON [dbo].[ihr_employee_leaving_status] ([sync_status]);
GO

PRINT 'ihr_employee_leaving_status table deployed';
GO
