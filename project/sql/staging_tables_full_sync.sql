-- ============================================================
-- 全量同步任务影子表(STG) — 配合"先写影子表、成功后原子切换"模式
-- 消除"先删后拉"风险: 同步中途失败时主表保留上一轮完整数据。
--
-- 涉及任务:
--   企微库(BC_SPORTS_QYWX): QywxCustomerDetailTask(客户详情2表)
--                           QywxDepartmentMemberDetailTask(成员详情2表)
--                           QywxGroupChatTask(群详情2表)
--   IHR库(BC_SPORTS_IHR):   IhrEmployeeTask.syncDetail(员工详情+弹性属性2表)
--
-- 说明:
--   1. 幂等: 已存在则跳过。
--   2. 影子表用 SELECT TOP 0 * INTO 建立结构与主表完全一致;
--      不建索引(纯堆表, 批量插入最快)。
--   3. 主表结构变更(ALTER)后, 影子表不会自动跟随, 需手动
--      DROP TABLE xxx_STG 后重跑本脚本重建。
-- ============================================================

-- ============================================================
-- 第 1 部分: 企微库执行 (BC_SPORTS_QYWX)
-- ============================================================

IF OBJECT_ID('dbo.VX_CustomerListDetails_external_contact_STG', 'U') IS NULL
    SELECT TOP 0 * INTO dbo.VX_CustomerListDetails_external_contact_STG FROM dbo.VX_CustomerListDetails_external_contact;
ELSE PRINT 'VX_CustomerListDetails_external_contact_STG 已存在, 跳过';

IF OBJECT_ID('dbo.VX_CustomerListDetails_follow_info_STG', 'U') IS NULL
    SELECT TOP 0 * INTO dbo.VX_CustomerListDetails_follow_info_STG FROM dbo.VX_CustomerListDetails_follow_info;
ELSE PRINT 'VX_CustomerListDetails_follow_info_STG 已存在, 跳过';

IF OBJECT_ID('dbo.VX_DepartmentMembersList_STG', 'U') IS NULL
    SELECT TOP 0 * INTO dbo.VX_DepartmentMembersList_STG FROM dbo.VX_DepartmentMembersList;
ELSE PRINT 'VX_DepartmentMembersList_STG 已存在, 跳过';

IF OBJECT_ID('dbo.VX_CustomerList_department_STG', 'U') IS NULL
    SELECT TOP 0 * INTO dbo.VX_CustomerList_department_STG FROM dbo.VX_CustomerList_department;
ELSE PRINT 'VX_CustomerList_department_STG 已存在, 跳过';

IF OBJECT_ID('dbo.VX_CustomerBaseDetails_STG', 'U') IS NULL
    SELECT TOP 0 * INTO dbo.VX_CustomerBaseDetails_STG FROM dbo.VX_CustomerBaseDetails;
ELSE PRINT 'VX_CustomerBaseDetails_STG 已存在, 跳过';

IF OBJECT_ID('dbo.VX_CustomerBaseDetails_GroupMembers_STG', 'U') IS NULL
    SELECT TOP 0 * INTO dbo.VX_CustomerBaseDetails_GroupMembers_STG FROM dbo.VX_CustomerBaseDetails_GroupMembers;
ELSE PRINT 'VX_CustomerBaseDetails_GroupMembers_STG 已存在, 跳过';

-- ============================================================
-- 第 2 部分: IHR库执行 (BC_SPORTS_IHR)
-- ============================================================

IF OBJECT_ID('dbo.employee_information_STG', 'U') IS NULL
    SELECT TOP 0 * INTO dbo.employee_information_STG FROM dbo.employee_information;
ELSE PRINT 'employee_information_STG 已存在, 跳过';

IF OBJECT_ID('dbo.employee_flex_attributes_STG', 'U') IS NULL
    SELECT TOP 0 * INTO dbo.employee_flex_attributes_STG FROM dbo.employee_flex_attributes;
ELSE PRINT 'employee_flex_attributes_STG 已存在, 跳过';
