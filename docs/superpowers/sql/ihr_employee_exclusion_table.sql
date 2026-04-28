
-- 在 SQL Server 的 BC_SPORTS_QYWX 数据库中执行
-- 创建 IHR 员工排除表

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[ihr_employee_exclusion]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[ihr_employee_exclusion] (
        [id] VARCHAR(64) NOT NULL,
        [staff_name] VARCHAR(100) NOT NULL,
        [staff_no] VARCHAR(50) NOT NULL,
        [exclusion_type] INT NOT NULL,
        [status] INT DEFAULT 1,
        [reason] VARCHAR(500) NULL,
        [create_time] DATETIME NULL,
        [update_time] DATETIME NULL,
        [create_by] VARCHAR(64) NULL,
        [update_by] VARCHAR(64) NULL,
        [deleted] INT DEFAULT 0,
        CONSTRAINT [PK_ihr_employee_exclusion] PRIMARY KEY CLUSTERED ([id] ASC)
    );

    CREATE INDEX [idx_exclusion_type] ON [dbo].[ihr_employee_exclusion] ([exclusion_type]);
    CREATE INDEX [idx_staff_no] ON [dbo].[ihr_employee_exclusion] ([staff_no]);
    CREATE INDEX [idx_status] ON [dbo].[ihr_employee_exclusion] ([status]);

    PRINT '表 ihr_employee_exclusion 创建成功';
END
ELSE
BEGIN
    PRINT '表 ihr_employee_exclusion 已存在，跳过创建';
END
