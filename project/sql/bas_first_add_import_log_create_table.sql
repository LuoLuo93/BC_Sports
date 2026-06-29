-- ==========================================================
-- Bas_FirstAdd_ImportLog 表：客户首次添加导入日志（SQL Server）
-- 库: BC_SPORTS_QYWX
-- ==========================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.Bas_FirstAdd_ImportLog') AND type in (N'U'))
    DROP TABLE dbo.Bas_FirstAdd_ImportLog;
GO

CREATE TABLE dbo.Bas_FirstAdd_ImportLog
(
    id            INT IDENTITY(1,1) PRIMARY KEY,
    file_name     NVARCHAR(255),   -- 原始文件名
    file_size     BIGINT,          -- 文件大小(字节)
    total_count   INT,             -- 总行数
    success_count INT,             -- 成功数
    fail_count    INT,             -- 失败数
    status        NVARCHAR(20),    -- SUCCESS / PARTIAL / FAILED
    error_msg     NVARCHAR(MAX),   -- 错误信息(截断)
    create_time   DATETIME DEFAULT GETDATE(),
    create_by     NVARCHAR(64)     -- 操作人
);
GO

EXEC sp_addextendedproperty 'MS_Description', '客户首次添加-导入日志', 'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd_ImportLog';
EXEC sp_addextendedproperty 'MS_Description', '原始文件名',   'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd_ImportLog', 'COLUMN', 'file_name';
EXEC sp_addextendedproperty 'MS_Description', '文件大小(字节)','SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd_ImportLog', 'COLUMN', 'file_size';
EXEC sp_addextendedproperty 'MS_Description', '总行数',       'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd_ImportLog', 'COLUMN', 'total_count';
EXEC sp_addextendedproperty 'MS_Description', '成功数',       'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd_ImportLog', 'COLUMN', 'success_count';
EXEC sp_addextendedproperty 'MS_Description', '失败数',       'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd_ImportLog', 'COLUMN', 'fail_count';
EXEC sp_addextendedproperty 'MS_Description', '状态',         'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd_ImportLog', 'COLUMN', 'status';
EXEC sp_addextendedproperty 'MS_Description', '错误信息',     'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd_ImportLog', 'COLUMN', 'error_msg';
EXEC sp_addextendedproperty 'MS_Description', '创建时间',     'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd_ImportLog', 'COLUMN', 'create_time';
EXEC sp_addextendedproperty 'MS_Description', '操作人',       'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd_ImportLog', 'COLUMN', 'create_by';
GO

CREATE INDEX idx_first_add_import_log_time ON dbo.Bas_FirstAdd_ImportLog(create_time DESC);
GO
