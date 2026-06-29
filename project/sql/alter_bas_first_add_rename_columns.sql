-- ==========================================================
-- Bas_FirstAdd 表：中文列名改英文 snake_case + 增加自增主键
-- 库: BC_SPORTS_QYWX (SQL Server)
-- ==========================================================

-- 1. 增加自增主键（与现有 VX_ 表风格一致）
ALTER TABLE dbo.Bas_FirstAdd ADD id INT IDENTITY(1,1) PRIMARY KEY;

-- 2. 重命名中文列
EXEC sp_rename 'dbo.Bas_FirstAdd.客户id',         'customer_id',      'COLUMN';
EXEC sp_rename 'dbo.Bas_FirstAdd.首次添加时间',    'first_add_time',   'COLUMN';
EXEC sp_rename 'dbo.Bas_FirstAdd.首次添加人部门',  'first_adder_dept', 'COLUMN';
EXEC sp_rename 'dbo.Bas_FirstAdd.首次添加人店铺',  'first_adder_store','COLUMN';
EXEC sp_rename 'dbo.Bas_FirstAdd.首次添加人',      'first_adder',      'COLUMN';

-- 3. 校验
-- SELECT TOP 5 * FROM dbo.Bas_FirstAdd;
