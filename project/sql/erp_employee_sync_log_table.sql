-- ==========================================================
-- ERP人员同步日志表
-- 目标数据库: SQL Server > BC_SPORTS_IHR
-- 说明: 记录每次同步伯俊ERP的完整请求体和返回参数
--       与 erp_employee_sync_status(最新态) 不同，本表每次同步 INSERT 一条，保留完整历史
-- ==========================================================

USE [BC_SPORTS_IHR];
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[erp_employee_sync_log]') AND type IN (N'U'))
BEGIN
    CREATE TABLE [dbo].[erp_employee_sync_log] (
        [id]              BIGINT         IDENTITY(1,1)   PRIMARY KEY,
        [sync_type]       NVARCHAR(20)   NOT NULL,       -- ONBOARDING / UPDATE / LEAVING
        [employee_id]     NVARCHAR(50)   NULL,
        [staff_name]      NVARCHAR(100)  NULL,
        [staff_no]        NVARCHAR(50)   NULL,
        [sync_status]     INT            NOT NULL,        -- 1=成功, 2=失败, 3=已跳过
        [request_body]    NVARCHAR(MAX)  NULL,            -- 完整请求体(含sip_appkey/sip_sign/transactions)
        [response_body]   NVARCHAR(MAX)  NULL,            -- 伯俊返回的完整响应JSON
        [error_message]   NVARCHAR(500)  NULL,
        [sync_time]       DATETIME       NOT NULL DEFAULT GETDATE()
    );
    PRINT 'Table erp_employee_sync_log created';
END
GO

-- 按同步类型+时间倒序的分页查询索引
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'[dbo].[erp_employee_sync_log]') AND name = N'IX_ErpSyncLog_Type_Time')
    CREATE NONCLUSTERED INDEX [IX_ErpSyncLog_Type_Time]
        ON [dbo].[erp_employee_sync_log] ([sync_type], [sync_time] DESC);
GO

-- 按工号查询索引
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'[dbo].[erp_employee_sync_log]') AND name = N'IX_ErpSyncLog_StaffNo')
    CREATE NONCLUSTERED INDEX [IX_ErpSyncLog_StaffNo]
        ON [dbo].[erp_employee_sync_log] ([staff_no]);
GO

PRINT 'erp_employee_sync_log table deployed';
GO
