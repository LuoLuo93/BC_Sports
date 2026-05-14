-- ==========================================================
-- 人员调整管理 - 同步状态表
-- 目标数据库: SQL Server > BC_SPORTS_IHR
-- ==========================================================

USE [BC_SPORTS_IHR];
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[ihr_employee_update_status]') AND type IN (N'U'))
BEGIN
    CREATE TABLE [dbo].[ihr_employee_update_status] (
        [id]                BIGINT          IDENTITY(1,1)   PRIMARY KEY,
        [staff_id]          NVARCHAR(50)    NOT NULL,
        [staff_name]        NVARCHAR(100)   NULL,
        [staff_no]          NVARCHAR(50)    NULL,
        [sync_status]       INT             NOT NULL DEFAULT 0,
        [sync_time]         DATETIME        NULL,
        [error_message]     NVARCHAR(500)   NULL,
        [create_time]       DATETIME        DEFAULT GETDATE(),
        [update_time]       DATETIME        DEFAULT GETDATE()
    );
    PRINT 'Table ihr_employee_update_status created';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'[dbo].[ihr_employee_update_status]') AND name = N'idx_update_staff_id')
    CREATE UNIQUE INDEX [idx_update_staff_id] ON [dbo].[ihr_employee_update_status] ([staff_id]);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'[dbo].[ihr_employee_update_status]') AND name = N'idx_update_sync_status')
    CREATE INDEX [idx_update_sync_status] ON [dbo].[ihr_employee_update_status] ([sync_status]);
GO
