-- ==========================================================
-- 企业微信群相关表
-- ==========================================================

USE [BC_SPORTS_QYWX];
GO

-- 企业微信群列表（临时表）
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[VX_CustomerBase]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[VX_CustomerBase] (
        [id] INT IDENTITY(1,1) PRIMARY KEY,
        [chatId] NVARCHAR(100) NOT NULL,
        [status] NVARCHAR(50) NULL,
        [createTime] DATETIME NULL DEFAULT GETDATE()
    );
    CREATE INDEX [idx_vx_customerbase_chatid] ON [dbo].[VX_CustomerBase]([chatId]);
    PRINT '===== 表 VX_CustomerBase 创建完成 =====';
END
GO

-- 企业微信群详情表
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[VX_CustomerBaseDetails]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[VX_CustomerBaseDetails] (
        [id] INT IDENTITY(1,1) PRIMARY KEY,
        [chatId] NVARCHAR(100) NOT NULL,
        [name] NVARCHAR(500) NULL,
        [owner] NVARCHAR(100) NULL,
        [createTime] NVARCHAR(100) NULL,
        [member_list] INT NULL,
        [createTime2] DATETIME NULL DEFAULT GETDATE()
    );
    CREATE INDEX [idx_vx_customerbasedetails_chatid] ON [dbo].[VX_CustomerBaseDetails]([chatId]);
    CREATE INDEX [idx_vx_customerbasedetails_owner] ON [dbo].[VX_CustomerBaseDetails]([owner]);
    PRINT '===== 表 VX_CustomerBaseDetails 创建完成 =====';
END
GO

-- 企业微信群成员表
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[VX_CustomerBaseDetails_GroupMembers]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[VX_CustomerBaseDetails_GroupMembers] (
        [id] INT IDENTITY(1,1) PRIMARY KEY,
        [chatId] NVARCHAR(100) NOT NULL,
        [userid] NVARCHAR(100) NOT NULL,
        [type] NVARCHAR(50) NULL,
        [join_time] NVARCHAR(100) NULL,
        [join_scene] NVARCHAR(100) NULL,
        [invitor] NVARCHAR(100) NULL,
        [name] NVARCHAR(200) NULL,
        [unionId] NVARCHAR(100) NULL,
        [createTime] DATETIME NULL DEFAULT GETDATE()
    );
    CREATE INDEX [idx_vx_customerbasegroupmembers_chatid] ON [dbo].[VX_CustomerBaseDetails_GroupMembers]([chatId]);
    CREATE INDEX [idx_vx_customerbasegroupmembers_userid] ON [dbo].[VX_CustomerBaseDetails_GroupMembers]([userid]);
    PRINT '===== 表 VX_CustomerBaseDetails_GroupMembers 创建完成 =====';
END
GO

PRINT '===== 企业微信群相关表初始化完成 =====';
GO
