-- ==========================================================
-- BC体育数据管理系统 - 企业微信数据库初始化 (SQL Server)
-- 目标数据库: BC_SPORTS_QYWX
-- ==========================================================

-- 创建数据库（如果不存在）
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'BC_SPORTS_QYWX')
BEGIN
    CREATE DATABASE [BC_SPORTS_QYWX];
    PRINT '===== 数据库 BC_SPORTS_QYWX 创建完成 =====';
END
GO

USE [BC_SPORTS_QYWX];
GO


-- AccessToken 表
IF OBJECT_ID('dbo.VX_AccessToken', 'U') IS NOT NULL DROP TABLE dbo.VX_AccessToken;
CREATE TABLE dbo.VX_AccessToken (
    id              BIGINT          PRIMARY KEY,
    access_token    NVARCHAR(500)   NOT NULL,
    createTime      DATETIME        DEFAULT GETDATE(),
    updateTime      DATETIME        DEFAULT GETDATE()
);
GO

-- 部门表
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
GO

-- 部门成员表（简易版）
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
GO

-- 配置了客户联系功能的成员表
IF OBJECT_ID('dbo.VX_FollowUserList', 'U') IS NOT NULL DROP TABLE dbo.VX_FollowUserList;
CREATE TABLE dbo.VX_FollowUserList (
    id              BIGINT          PRIMARY KEY,
    follow_user     NVARCHAR(100),
    createTime      DATETIME        DEFAULT GETDATE()
);
GO

-- 部门成员详情表（包含职位、手机等信息）
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
GO

-- 客户联系成员部门关系表
IF OBJECT_ID('dbo.VX_CustomerList_department', 'U') IS NOT NULL DROP TABLE dbo.VX_CustomerList_department;
CREATE TABLE dbo.VX_CustomerList_department (
    id              BIGINT          PRIMARY KEY,
    userid          NVARCHAR(100),
    department      INT,
    createTime      DATETIME        DEFAULT GETDATE()
);
GO

PRINT '===== BC_SPORTS_QYWX 数据库表创建完成 =====';
GO

-- ==========================================================
-- 预设定时任务（需要在主库中执行）
-- ==========================================================
-- 注意：以下INSERT语句需要在BC_SPORTS数据库的schedule_task表中执行
-- 请根据实际情况修改数据库名称
--
-- USE [BC_SPORTS];
-- GO
--
-- -- 企业微信-刷新Token（每小时执行）
-- IF NOT EXISTS (SELECT 1 FROM schedule_task WHERE task_key = 'qywx.token.refresh')
-- BEGIN
--     INSERT INTO schedule_task (task_key, task_name, bean_name, method_name, cron_expression, task_status, create_time, update_time, description)
--     VALUES ('qywx.token.refresh', 'QW-刷新Token', 'qywxTokenTask', 'refresh', '0 0 * * * ?', 0, GETDATE(), GETDATE(), '刷新企业微信AccessToken');
-- END
--
-- -- 企业微信-同步部门（每天凌晨2点执行）
-- IF NOT EXISTS (SELECT 1 FROM schedule_task WHERE task_key = 'qywx.department.sync')
-- BEGIN
--     INSERT INTO schedule_task (task_key, task_name, bean_name, method_name, cron_expression, task_status, create_time, update_time, description)
--     VALUES ('qywx.department.sync', 'QW-同步部门', 'qywxDepartmentTask', 'sync', '0 0 2 * * ?', 0, GETDATE(), GETDATE(), '同步企业微信部门列表');
-- END
--
-- -- 企业微信-同步部门成员（每天凌晨2点15分执行）
-- IF NOT EXISTS (SELECT 1 FROM schedule_task WHERE task_key = 'qywx.department.member.sync')
-- BEGIN
--     INSERT INTO schedule_task (task_key, task_name, bean_name, method_name, cron_expression, task_status, create_time, update_time, description)
--     VALUES ('qywx.department.member.sync', 'QW-同步部门成员', 'qywxDepartmentMemberTask', 'sync', '0 15 2 * * ?', 0, GETDATE(), GETDATE(), '同步企业微信部门成员列表');
-- END
--
-- -- 企业微信-同步客户联系成员（每天3:15和15:15执行）
-- IF NOT EXISTS (SELECT 1 FROM schedule_task WHERE task_key = 'qywx.follow.user.sync')
-- BEGIN
--     INSERT INTO schedule_task (task_key, task_name, bean_name, method_name, cron_expression, task_status, create_time, update_time, description)
--     VALUES ('qywx.follow.user.sync', 'QW-同步客户联系成员', 'qywxFollowUserTask', 'sync', '0 15 3,15 * * ?', 0, GETDATE(), GETDATE(), '同步配置了客户联系功能的成员列表');
-- END
--
-- -- 企业微信-同步成员详情（每天1:05和15:05执行）
-- IF NOT EXISTS (SELECT 1 FROM schedule_task WHERE task_key = 'qywx.department.member.detail.sync')
-- BEGIN
--     INSERT INTO schedule_task (task_key, task_name, bean_name, method_name, cron_expression, task_status, create_time, update_time, description)
--     VALUES ('qywx.department.member.detail.sync', 'QW-同步成员详情[自动]', 'qywxDepartmentMemberDetailTask', 'sync', '0 5 1,15 * * ?', 0, GETDATE(), GETDATE(), '【默认】自动选择数据源同步成员详情');
-- END
--
-- -- 企业微信-同步成员详情[API直连]
-- IF NOT EXISTS (SELECT 1 FROM schedule_task WHERE task_key = 'qywx.department.member.detail.sync.api')
-- BEGIN
--     INSERT INTO schedule_task (task_key, task_name, bean_name, method_name, cron_expression, task_status, create_time, update_time, description)
--     VALUES ('qywx.department.member.detail.sync.api', 'QW-同步成员详情[API直连]', 'qywxDepartmentMemberDetailTask', 'syncFromApi', '0 0 3 * * ?', 0, GETDATE(), GETDATE(), '【推荐】直接从API获取所有成员并同步详情');
-- END
--
-- -- 企业微信-同步成员详情[仅客户联系]
-- IF NOT EXISTS (SELECT 1 FROM schedule_task WHERE task_key = 'qywx.department.member.detail.sync.follow')
-- BEGIN
--     INSERT INTO schedule_task (task_key, task_name, bean_name, method_name, cron_expression, task_status, create_time, update_time, description)
--     VALUES ('qywx.department.member.detail.sync.follow', 'QW-同步成员详情[仅客户联系]', 'qywxDepartmentMemberDetailTask', 'syncFromFollowUser', '0 20 3,15 * * ?', 0, GETDATE(), GETDATE(), '仅同步配置了客户联系功能的成员详情');
-- END
--
-- -- 企业微信-同步成员详情[仅部门成员]
-- IF NOT EXISTS (SELECT 1 FROM schedule_task WHERE task_key = 'qywx.department.member.detail.sync.dept')
-- BEGIN
--     INSERT INTO schedule_task (task_key, task_name, bean_name, method_name, cron_expression, task_status, create_time, update_time, description)
--     VALUES ('qywx.department.member.detail.sync.dept', 'QW-同步成员详情[仅部门成员]', 'qywxDepartmentMemberDetailTask', 'syncFromDepartment', '0 25 3,15 * * ?', 0, GETDATE(), GETDATE(), '仅同步已在本地的部门成员详情');
-- END
-- GO

PRINT '===== BC_SPORTS_QYWX 数据库初始化完成 =====';
GO
