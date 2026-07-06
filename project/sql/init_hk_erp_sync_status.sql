-- ======================================================
-- BC体育 - HK ERP直写同步状态表 (BC_SPORTS_IHR 库)
-- ======================================================
-- 用途：记录 HK ERP 直写链路（入职/变更/离职）的同步状态
--       与伯俊链路的 erp_employee_sync_status 隔离，sync_type 用 HK_ 前缀
-- 前置条件：数据库为 BC_SPORTS_IHR (SQL Server)
-- 幂等：可重复执行

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'hk_erp_sync_status')
BEGIN
    CREATE TABLE dbo.hk_erp_sync_status (
        id              BIGINT          IDENTITY(1,1) PRIMARY KEY,
        sync_type       NVARCHAR(20)    NOT NULL,       -- HK_ONBOARDING / HK_UPDATE / HK_LEAVING
        employee_id     NVARCHAR(50)    NOT NULL,       -- 关联 employee_information.id
        staff_name      NVARCHAR(100),
        staff_no        NVARCHAR(50),
        sync_status     INT             NOT NULL DEFAULT 0,  -- 0待同步 1成功 2失败 3跳过
        sync_time       DATETIME,
        error_message   NVARCHAR(500),
        create_time     DATETIME        DEFAULT GETDATE(),
        update_time     DATETIME        DEFAULT GETDATE()
    );

    -- 唯一约束：(employee_id, sync_type) —— MERGE upsert 使用
    CREATE UNIQUE INDEX uk_hk_erp_sync_emp_type
        ON dbo.hk_erp_sync_status (employee_id, sync_type);

    PRINT 'hk_erp_sync_status 表创建成功';
END
ELSE
BEGIN
    PRINT 'hk_erp_sync_status 表已存在，跳过创建';
END
GO
