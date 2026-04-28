-- 企业微信成员扩展属性表
-- 对应原接口项目中的 VX_attrsBase

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[VX_attrsBase]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[VX_attrsBase] (
        [id] BIGINT NOT NULL PRIMARY KEY,
        [userid] NVARCHAR(100) NOT NULL,
        [name] NVARCHAR(200) NULL,
        [enrollInDate] NVARCHAR(100) NULL,
        [type] NVARCHAR(50) NULL,
        [createTime] DATETIME NULL
    );

    CREATE INDEX [idx_vx_attrs_userid] ON [dbo].[VX_attrsBase]([userid]);
END
GO
