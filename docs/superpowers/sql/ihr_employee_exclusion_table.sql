-- ======================================================
-- IHR 员工排除管理 - 数据表脚本
-- 执行目标：SQL Server > BC_SPORTS_QYWX 数据库
-- ======================================================

-- 1. 创建排除表
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[ihr_employee_exclusion]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[ihr_employee_exclusion] (
        [id]             VARCHAR(64)   NOT NULL,
        [staff_name]     VARCHAR(100)  NOT NULL,
        [staff_no]       VARCHAR(50)   NOT NULL,
        [exclusion_type] INT           NOT NULL,       -- 1=入职排除, 2=离职排除
        [status]         INT           DEFAULT 1,       -- 0=禁用, 1=启用
        [reason]         VARCHAR(500)  NULL,
        [create_time]    DATETIME      NULL,
        [update_time]    DATETIME      NULL,
        [create_by]      VARCHAR(64)   NULL,
        [update_by]      VARCHAR(64)   NULL,
        [deleted]        INT           DEFAULT 0,       -- 0=未删除, 1=已删除（逻辑删除）
        CONSTRAINT [PK_ihr_employee_exclusion] PRIMARY KEY CLUSTERED ([id] ASC)
    );
    PRINT '表 ihr_employee_exclusion 创建成功';
END
ELSE
BEGIN
    PRINT '表 ihr_employee_exclusion 已存在，跳过建表';
END
GO

-- 2. 单列索引（基础查询优化）
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'[dbo].[ihr_employee_exclusion]') AND name = N'idx_exclusion_type')
    CREATE INDEX [idx_exclusion_type] ON [dbo].[ihr_employee_exclusion] ([exclusion_type]);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'[dbo].[ihr_employee_exclusion]') AND name = N'idx_staff_no')
    CREATE INDEX [idx_staff_no] ON [dbo].[ihr_employee_exclusion] ([staff_no]);
GO

-- 3. 复合索引（优化 checkExcluded 高频查询：WHERE staff_name=? AND staff_no=? AND exclusion_type=? AND status=1 AND deleted=0）
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'[dbo].[ihr_employee_exclusion]') AND name = N'idx_exclusion_check')
    CREATE INDEX [idx_exclusion_check] ON [dbo].[ihr_employee_exclusion] ([staff_name], [staff_no], [exclusion_type], [status], [deleted]);
GO

PRINT '========================================';
PRINT 'IHR 员工排除表及索引部署完成';
PRINT '========================================';
