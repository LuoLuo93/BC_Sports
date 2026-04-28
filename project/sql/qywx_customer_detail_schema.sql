-- ==========================================================
-- 企业微信客户详情表
-- 对应原接口项目中的 VX_CustomerListDetails_external_contact 和 VX_CustomerListDetails_follow_info
-- ==========================================================

USE [BC_SPORTS_QYWX];
GO

-- 外部联系人基本信息表
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[VX_CustomerListDetails_external_contact]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[VX_CustomerListDetails_external_contact] (
        [id] INT IDENTITY(1,1) PRIMARY KEY,
        [externalUserid] NVARCHAR(100) NOT NULL,
        [name] NVARCHAR(200) NULL,
        [type] NVARCHAR(50) NULL,
        [gender] NVARCHAR(50) NULL,
        [unionid] NVARCHAR(100) NULL,
        [corp_name] NVARCHAR(200) NULL,
        [createTime] DATETIME NULL DEFAULT GETDATE()
    );

    CREATE INDEX [idx_customer_external_userid] ON [dbo].[VX_CustomerListDetails_external_contact]([externalUserid]);
    PRINT '===== 表 VX_CustomerListDetails_external_contact 创建完成 =====';
END
GO

-- 外部联系人跟进信息表
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[VX_CustomerListDetails_follow_info]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[VX_CustomerListDetails_follow_info] (
        [id] INT IDENTITY(1,1) PRIMARY KEY,
        [userid] NVARCHAR(100) NOT NULL,
        [remark] NVARCHAR(500) NULL,
        [description] NVARCHAR(500) NULL,
        [createtime] NVARCHAR(100) NULL,
        [addWay] NVARCHAR(100) NULL,
        [operUserid] NVARCHAR(100) NULL,
        [createTime2] DATETIME NULL DEFAULT GETDATE(),
        [externalUserid] NVARCHAR(100) NOT NULL
    );

    CREATE INDEX [idx_follow_userid] ON [dbo].[VX_CustomerListDetails_follow_info]([userid]);
    CREATE INDEX [idx_follow_external_userid] ON [dbo].[VX_CustomerListDetails_follow_info]([externalUserid]);
    PRINT '===== 表 VX_CustomerListDetails_follow_info 创建完成 =====';
END
GO

PRINT '===== 企业微信客户详情表初始化完成 =====';
GO
