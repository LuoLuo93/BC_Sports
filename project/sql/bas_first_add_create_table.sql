-- ==========================================================
-- Bas_FirstAdd 表：全新建表语句（SQL Server）
-- 用途：客户首次添加记录，供 Excel 批量导入
-- ==========================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.Bas_FirstAdd') AND type in (N'U'))
    DROP TABLE dbo.Bas_FirstAdd;
GO

CREATE TABLE dbo.Bas_FirstAdd
(
    id               INT IDENTITY(1,1) PRIMARY KEY,  -- 自增主键
    customer_id      NVARCHAR(255),                  -- 客户id
    first_add_time   NVARCHAR(255),                  -- 首次添加时间
    first_adder_dept NVARCHAR(255),                  -- 首次添加人部门
    first_adder_store NVARCHAR(255),                 -- 首次添加人店铺
    first_adder      NVARCHAR(255)                   -- 首次添加人
);
GO

-- 表注释（SQL Server 用扩展属性）
EXEC sp_addextendedproperty 'MS_Description', '客户首次添加记录表', 'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd';
EXEC sp_addextendedproperty 'MS_Description', '自增主键',          'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd', 'COLUMN', 'id';
EXEC sp_addextendedproperty 'MS_Description', '客户id',            'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd', 'COLUMN', 'customer_id';
EXEC sp_addextendedproperty 'MS_Description', '首次添加时间',       'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd', 'COLUMN', 'first_add_time';
EXEC sp_addextendedproperty 'MS_Description', '首次添加人部门',     'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd', 'COLUMN', 'first_adder_dept';
EXEC sp_addextendedproperty 'MS_Description', '首次添加人店铺',     'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd', 'COLUMN', 'first_adder_store';
EXEC sp_addextendedproperty 'MS_Description', '首次添加人',         'SCHEMA', 'dbo', 'TABLE', 'Bas_FirstAdd', 'COLUMN', 'first_adder';
GO

-- 导入去重优化索引（按 customer_id 去重查询）
CREATE INDEX idx_bas_first_add_customer_id ON dbo.Bas_FirstAdd(customer_id);
GO
