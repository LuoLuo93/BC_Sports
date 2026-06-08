-- ==========================================================
-- 用户表 - 新增 BCrypt 密码字段
-- ==========================================================

-- Oracle 版本
ALTER TABLE bc_sports_sys_user ADD (
    password_new VARCHAR2(100)
);

COMMENT ON COLUMN bc_sports_sys_user.password_new IS 'BCrypt加密密码（渐进式迁移）';

-- SQL Server 版本 (如果需要):
-- ALTER TABLE bc_sports_sys_user ADD password_new NVARCHAR(100);
-- EXEC sp_addextendedproperty 'MS_Description', 'BCrypt加密密码（渐进式迁移）', 'SCHEMA', 'dbo', 'TABLE', 'bc_sports_sys_user', 'COLUMN', 'password_new';

COMMIT;
