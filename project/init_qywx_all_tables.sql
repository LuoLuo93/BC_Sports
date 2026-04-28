-- ==========================================================
-- BC体育数据管理系统 - 企业微信数据库完整初始化 (SQL Server)
-- 目标数据库: BC_SPORTS_QYWX
-- 包含所有企业微信相关表
-- ==========================================================

USE [BC_SPORTS_QYWX];
GO

PRINT '===== 开始初始化 BC_SPORTS_QYWX 数据库 =====';
GO

-- ==========================================================
-- 1. AccessToken 表
-- ==========================================================
IF OBJECT_ID('dbo.VX_AccessToken', 'U') IS NOT NULL DROP TABLE dbo.VX_AccessToken;
CREATE TABLE dbo.VX_AccessToken (
    id              BIGINT          PRIMARY KEY,
    access_token    NVARCHAR(500)   NOT NULL,
    createTime      DATETIME        DEFAULT GETDATE(),
    updateTime      DATETIME        DEFAULT GETDATE()
);
PRINT '===== 表 VX_AccessToken 创建完成 =====';
GO

-- ==========================================================
-- 2. 部门表
-- ==========================================================
IF OBJECT_ID('dbo.VX_DepartmentList', 'U') IS NOT NULL DROP TABLE dbo.VX_DepartmentList;
CREATE TABLE dbo.VX_DepartmentList (
    id              BIGINT          PRIMARY KEY,
    departId        NVARCHAR(100),
    name            NVARCHAR(200),
    name_en         NVARCHAR(200),
    parentId        NVARCHAR(100),
    parentOrder     NVARCHAR(100),
    createTime      DATETIME        DEFAULT GETDATE(),
    updateTime      DATETIME        DEFAULT GETDATE()
);
PRINT '===== 表 VX_DepartmentList 创建完成 =====';
GO

-- ==========================================================
-- 3. 部门成员表（简易版）
-- ==========================================================
IF OBJECT_ID('dbo.VX_DepartmentMember', 'U') IS NOT NULL DROP TABLE dbo.VX_DepartmentMember;
CREATE TABLE dbo.VX_DepartmentMember (
    id              BIGINT          PRIMARY KEY,
    userid          NVARCHAR(100),
    name            NVARCHAR(200),
    open_userid     NVARCHAR(200),
    department      INT,
    createTime      DATETIME        DEFAULT GETDATE(),
    updateTime      DATETIME        DEFAULT GETDATE()
);
PRINT '===== 表 VX_DepartmentMember 创建完成 =====';
GO

-- ==========================================================
-- 4. 配置了客户联系功能的成员表
-- ==========================================================
IF OBJECT_ID('dbo.VX_FollowUserList', 'U') IS NOT NULL DROP TABLE dbo.VX_FollowUserList;
CREATE TABLE dbo.VX_FollowUserList (
    id              BIGINT          PRIMARY KEY,
    follow_user     NVARCHAR(100),
    createTime      DATETIME        DEFAULT GETDATE()
);
PRINT '===== 表 VX_FollowUserList 创建完成 =====';
GO

-- ==========================================================
-- 5. 部门成员详情表（包含职位、手机等信息）
-- ==========================================================
IF OBJECT_ID('dbo.VX_DepartmentMembersList', 'U') IS NOT NULL DROP TABLE dbo.VX_DepartmentMembersList;
CREATE TABLE dbo.VX_DepartmentMembersList (
    id              BIGINT          PRIMARY KEY,
    userid          NVARCHAR(100),
    name            NVARCHAR(200),
    open_userid     NVARCHAR(200),
    createTime      DATETIME        DEFAULT GETDATE(),
    main_department NVARCHAR(200),
    position        NVARCHAR(200),
    mobile          NVARCHAR(100),
    status          NVARCHAR(50),
    updateTime      DATETIME        DEFAULT GETDATE()
);
PRINT '===== 表 VX_DepartmentMembersList 创建完成 =====';
GO

-- ==========================================================
-- 6. 客户联系成员部门关系表
-- ==========================================================
IF OBJECT_ID('dbo.VX_CustomerList_department', 'U') IS NOT NULL DROP TABLE dbo.VX_CustomerList_department;
CREATE TABLE dbo.VX_CustomerList_department (
    id              BIGINT          PRIMARY KEY,
    userid          NVARCHAR(100),
    department      INT,
    createTime      DATETIME        DEFAULT GETDATE()
);
PRINT '===== 表 VX_CustomerList_department 创建完成 =====';
GO

-- ==========================================================
-- 7. 成员扩展属性表 (VX_attrsBase)
-- ==========================================================
IF OBJECT_ID('dbo.VX_attrsBase', 'U') IS NOT NULL DROP TABLE dbo.VX_attrsBase;
CREATE TABLE dbo.VX_attrsBase (
    id              BIGINT          PRIMARY KEY,
    userid          NVARCHAR(100)   NOT NULL,
    name            NVARCHAR(200),
    type            NVARCHAR(50),
    enrollInDate    NVARCHAR(500),
    createTime      DATETIME        DEFAULT GETDATE()
);
CREATE INDEX [idx_attrsbase_userid] ON [dbo].[VX_attrsBase]([userid]);
PRINT '===== 表 VX_attrsBase 创建完成 =====';
GO

-- ==========================================================
-- 8. 企业微信外部联系人基本信息表
-- ==========================================================
IF OBJECT_ID('dbo.VX_CustomerListDetails_external_contact', 'U') IS NOT NULL DROP TABLE dbo.VX_CustomerListDetails_external_contact;
CREATE TABLE dbo.VX_CustomerListDetails_external_contact (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    externalUserid  NVARCHAR(100)   NOT NULL,
    name            NVARCHAR(200),
    type            NVARCHAR(50),
    gender          NVARCHAR(50),
    unionid         NVARCHAR(100),
    corp_name       NVARCHAR(200),
    createTime      DATETIME        DEFAULT GETDATE()
);
CREATE INDEX [idx_customer_external_userid] ON [dbo].[VX_CustomerListDetails_external_contact]([externalUserid]);
PRINT '===== 表 VX_CustomerListDetails_external_contact 创建完成 =====';
GO

-- ==========================================================
-- 9. 企业微信外部联系人跟进信息表
-- ==========================================================
IF OBJECT_ID('dbo.VX_CustomerListDetails_follow_info', 'U') IS NOT NULL DROP TABLE dbo.VX_CustomerListDetails_follow_info;
CREATE TABLE dbo.VX_CustomerListDetails_follow_info (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    userid          NVARCHAR(100)   NOT NULL,
    remark          NVARCHAR(500),
    description     NVARCHAR(500),
    createtime      NVARCHAR(100),
    addWay          NVARCHAR(100),
    operUserid      NVARCHAR(100),
    createTime2     DATETIME        DEFAULT GETDATE(),
    externalUserid  NVARCHAR(100)   NOT NULL
);
CREATE INDEX [idx_follow_userid] ON [dbo].[VX_CustomerListDetails_follow_info]([userid]);
CREATE INDEX [idx_follow_external_userid] ON [dbo].[VX_CustomerListDetails_follow_info]([externalUserid]);
PRINT '===== 表 VX_CustomerListDetails_follow_info 创建完成 =====';
GO

-- ==========================================================
-- 10. 企业微信群列表（临时表）
-- ==========================================================
IF OBJECT_ID('dbo.VX_CustomerBase', 'U') IS NOT NULL DROP TABLE dbo.VX_CustomerBase;
CREATE TABLE dbo.VX_CustomerBase (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    chatId          NVARCHAR(100)   NOT NULL,
    status          NVARCHAR(50),
    createTime      DATETIME        DEFAULT GETDATE()
);
CREATE INDEX [idx_vx_customerbase_chatid] ON [dbo].[VX_CustomerBase]([chatId]);
PRINT '===== 表 VX_CustomerBase 创建完成 =====';
GO

-- ==========================================================
-- 11. 企业微信群详情表
-- ==========================================================
IF OBJECT_ID('dbo.VX_CustomerBaseDetails', 'U') IS NOT NULL DROP TABLE dbo.VX_CustomerBaseDetails;
CREATE TABLE dbo.VX_CustomerBaseDetails (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    chatId          NVARCHAR(100)   NOT NULL,
    name            NVARCHAR(500),
    owner           NVARCHAR(100),
    createTime      NVARCHAR(100),
    member_list     INT,
    createTime2     DATETIME        DEFAULT GETDATE()
);
CREATE INDEX [idx_vx_customerbasedetails_chatid] ON [dbo].[VX_CustomerBaseDetails]([chatId]);
CREATE INDEX [idx_vx_customerbasedetails_owner] ON [dbo].[VX_CustomerBaseDetails]([owner]);
PRINT '===== 表 VX_CustomerBaseDetails 创建完成 =====';
GO

-- ==========================================================
-- 12. 企业微信群成员表
-- ==========================================================
IF OBJECT_ID('dbo.VX_CustomerBaseDetails_GroupMembers', 'U') IS NOT NULL DROP TABLE dbo.VX_CustomerBaseDetails_GroupMembers;
CREATE TABLE dbo.VX_CustomerBaseDetails_GroupMembers (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    chatId          NVARCHAR(100)   NOT NULL,
    userid          NVARCHAR(100)   NOT NULL,
    type            NVARCHAR(50),
    join_time       NVARCHAR(100),
    join_scene      NVARCHAR(100),
    invitor         NVARCHAR(100),
    name            NVARCHAR(200),
    unionId         NVARCHAR(100),
    createTime      DATETIME        DEFAULT GETDATE()
);
CREATE INDEX [idx_vx_customerbasegroupmembers_chatid] ON [dbo].[VX_CustomerBaseDetails_GroupMembers]([chatId]);
CREATE INDEX [idx_vx_customerbasegroupmembers_userid] ON [dbo].[VX_CustomerBaseDetails_GroupMembers]([userid]);
PRINT '===== 表 VX_CustomerBaseDetails_GroupMembers 创建完成 =====';
GO

-- ==========================================================
-- 初始化完成
-- ==========================================================
PRINT ' ';
PRINT '============================================================';
PRINT '===== BC_SPORTS_QYWX 数据库所有表初始化完成！！！ =====';
PRINT '============================================================';
PRINT ' ';
PRINT '已创建的表列表：';
PRINT '  1. VX_AccessToken                           - Token表';
PRINT '  2. VX_DepartmentList                        - 部门表';
PRINT '  3. VX_DepartmentMember                      - 部门成员表';
PRINT '  4. VX_FollowUserList                        - 客户联系成员表';
PRINT '  5. VX_DepartmentMembersList                 - 成员详情表';
PRINT '  6. VX_CustomerList_department               - 成员部门关系表';
PRINT '  7. VX_attrsBase                             - 成员扩展属性表';
PRINT '  8. VX_CustomerListDetails_external_contact  - 外部联系人表';
PRINT '  9. VX_CustomerListDetails_follow_info       - 跟进信息表';
PRINT ' 10. VX_CustomerBase                          - 群列表表';
PRINT ' 11. VX_CustomerBaseDetails                   - 群详情表';
PRINT ' 12. VX_CustomerBaseDetails_GroupMembers      - 群成员表';
PRINT ' ';
PRINT '现在可以运行企业微信同步任务了！';
GO
