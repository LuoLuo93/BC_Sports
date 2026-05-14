-- ==========================================================
-- ERP人员同步状态表
-- 目标数据库: SQL Server > BC_SPORTS_IHR
-- 说明: 记录每个员工同步到ERP的状态
--       通过 sync_type 区分入职/变更/离职
-- ==========================================================

USE [BC_SPORTS_IHR];
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[erp_employee_sync_status]') AND type IN (N'U'))
BEGIN
    CREATE TABLE [dbo].[erp_employee_sync_status] (
        [id]                BIGINT          IDENTITY(1,1)   PRIMARY KEY,
        [sync_type]         NVARCHAR(20)    NOT NULL,       -- ONBOARDING / UPDATE / LEAVING
        [employee_id]       NVARCHAR(50)    NOT NULL,
        [staff_name]        NVARCHAR(100)   NULL,
        [staff_no]          NVARCHAR(50)    NULL,
        [sync_status]       INT             NOT NULL DEFAULT 0, -- 0=未同步, 1=成功, 2=失败, 3=已跳过
        [sync_time]         DATETIME        NULL,
        [error_message]     NVARCHAR(500)   NULL,
        [create_time]       DATETIME        DEFAULT GETDATE(),
        [update_time]       DATETIME        DEFAULT GETDATE()
    );
    PRINT 'Table erp_employee_sync_status created';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'[dbo].[erp_employee_sync_status]') AND name = N'IX_ErpSync_EmployeeId_Type')
    CREATE UNIQUE NONCLUSTERED INDEX [IX_ErpSync_EmployeeId_Type]
        ON [dbo].[erp_employee_sync_status] ([employee_id], [sync_type]);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'[dbo].[erp_employee_sync_status]') AND name = N'IX_ErpSync_SyncType_Status')
    CREATE NONCLUSTERED INDEX [IX_ErpSync_SyncType_Status]
        ON [dbo].[erp_employee_sync_status] ([sync_type], [sync_status]);
GO

PRINT 'erp_employee_sync_status table deployed';
GO
