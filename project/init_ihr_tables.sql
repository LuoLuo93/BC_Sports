-- ==========================================================
-- BC体育数据管理系统 - IHR模块建表脚本 (SQL Server)
-- 目标数据库: BC_SPORTS_IHR
-- 说明: 从iHR360 API同步的人力数据，新增审计字段
-- ==========================================================

USE [BC_SPORTS_IHR];
GO

-- 1. OAuth Token 表
IF OBJECT_ID('dbo.access_token', 'U') IS NOT NULL DROP TABLE dbo.access_token;
CREATE TABLE dbo.access_token (
    id              BIGINT          PRIMARY KEY,
    access_token    NVARCHAR(500)   NOT NULL,
    expires_in      INT             DEFAULT 7200,
    create_time     DATETIME        DEFAULT GETDATE(),
    update_time     DATETIME        DEFAULT GETDATE()
);
GO

-- 2. 员工ID辅助表
IF OBJECT_ID('dbo.employees_auxiliary', 'U') IS NOT NULL DROP TABLE dbo.employees_auxiliary;
CREATE TABLE dbo.employees_auxiliary (
    id              BIGINT          IDENTITY(1,1) PRIMARY KEY,
    employees_id    NVARCHAR(50)    NOT NULL,
    create_time     DATETIME        DEFAULT GETDATE()
);
GO

-- 3. 员工基本信息表
IF OBJECT_ID('dbo.employees', 'U') IS NOT NULL DROP TABLE dbo.employees;
CREATE TABLE dbo.employees (
    id                  BIGINT          PRIMARY KEY,
    staff_id            NVARCHAR(50),
    staff_no            NVARCHAR(50),
    staff_name          NVARCHAR(100),
    nick_name           NVARCHAR(100),
    mobile_no           NVARCHAR(20),
    email               NVARCHAR(100),
    staff_status        NVARCHAR(20),
    id_card_type        NVARCHAR(20),
    id_card_no          NVARCHAR(50),
    department_name     NVARCHAR(200),
    position_name       NVARCHAR(100),
    sex                 NVARCHAR(10),
    staff_type          NVARCHAR(20),
    marry_status        NVARCHAR(20),
    highest_education   NVARCHAR(50),
    work_place          NVARCHAR(200),
    birthday            NVARCHAR(20),
    contract_begin_date NVARCHAR(20),
    contract_end_date   NVARCHAR(20),
    enroll_in_date      NVARCHAR(20),
    probation_end_date  NVARCHAR(20),
    created_date        NVARCHAR(20),
    last_update_date    NVARCHAR(20),
    create_time         DATETIME        DEFAULT GETDATE(),
    update_time         DATETIME        DEFAULT GETDATE()
);
GO

-- 4. 部门表
IF OBJECT_ID('dbo.department', 'U') IS NOT NULL DROP TABLE dbo.department;
CREATE TABLE dbo.department (
    id              BIGINT          PRIMARY KEY,
    department_id   BIGINT,
    name            NVARCHAR(200),
    parent_id       BIGINT,
    type            NVARCHAR(50),
    create_time     DATETIME        DEFAULT GETDATE(),
    update_time     DATETIME        DEFAULT GETDATE()
);
GO

-- 5. 员工详细信息表
IF OBJECT_ID('dbo.employee_information', 'U') IS NOT NULL DROP TABLE dbo.employee_information;
CREATE TABLE dbo.employee_information (
    id                          NVARCHAR(50)    PRIMARY KEY,
    age                         NVARCHAR(10),
    created_date                NVARCHAR(20),
    company_id                  NVARCHAR(50),
    qq_no                       NVARCHAR(50),
    wechat_no                   NVARCHAR(50),
    staff_no                    NVARCHAR(50),
    staff_name                  NVARCHAR(100),
    id_card_no                  NVARCHAR(50),
    id_card_type                NVARCHAR(20),
    birthday                    NVARCHAR(20),
    sex                         NVARCHAR(10),
    mobile_no                   NVARCHAR(20),
    email                       NVARCHAR(100),
    work_phone                  NVARCHAR(20),
    work_email                  NVARCHAR(100),
    marry_status                NVARCHAR(20),
    highest_education           NVARCHAR(50),
    nationality                 NVARCHAR(50),
    native_place                NVARCHAR(100),
    native_place_province_code  NVARCHAR(20),
    native_place_city_code      NVARCHAR(20),
    political_status            NVARCHAR(50),
    nick_name                   NVARCHAR(100),
    blood_type                  NVARCHAR(10),
    spouse_name                 NVARCHAR(50),
    child_name                  NVARCHAR(50),
    emergency_contact_name      NVARCHAR(50),
    emergency_contact_mobile    NVARCHAR(20),
    staff_status                NVARCHAR(20),
    staff_type                  NVARCHAR(20),
    leave_date                  NVARCHAR(20),
    probation_end_date          NVARCHAR(20),
    enroll_in_date              NVARCHAR(20),
    probation_length            NVARCHAR(20),
    contract_begin_date         NVARCHAR(20),
    contract_end_date           NVARCHAR(20),
    positive_date               NVARCHAR(20),
    hukou_type                  NVARCHAR(20),
    hukou_address               NVARCHAR(200),
    hukou_province_code         NVARCHAR(20),
    hukou_city_code             NVARCHAR(20),
    living_address              NVARCHAR(200),
    living_province_code        NVARCHAR(20),
    living_city_code            NVARCHAR(20),
    department_id               NVARCHAR(50),
    department_name             NVARCHAR(200),
    corporation_name            NVARCHAR(200),
    corporation_id              NVARCHAR(50),
    position_name               NVARCHAR(100),
    position_id                 NVARCHAR(50),
    job_title_name              NVARCHAR(100),
    job_title_id                NVARCHAR(50),
    supervisor_name             NVARCHAR(100),
    supervisor_id               NVARCHAR(50),
    position_level_id           NVARCHAR(50),
    position_level_name         NVARCHAR(100),
    last_name                   NVARCHAR(50),
    first_name                  NVARCHAR(50),
    legal_name                  NVARCHAR(100),
    work_place                  NVARCHAR(200),
    contract_type               NVARCHAR(50),
    is_deleted                  NVARCHAR(10),
    delete_date                 NVARCHAR(20),
    quit_type                   NVARCHAR(50),
    last_work_day               NVARCHAR(20),
    staff_origin                NVARCHAR(50),
    initial_work_years          NVARCHAR(10),
    enroll_work_years           NVARCHAR(10),
    first_level_department_id   NVARCHAR(50),
    first_level_department_name NVARCHAR(200),
    staff_remark                NVARCHAR(500),
    quit_reason                 NVARCHAR(500),
    quit_reason_type            NVARCHAR(50),
    quit_remind_staff           NVARCHAR(10),
    lunar_birthday_year         NVARCHAR(10),
    lunar_birthday_month        NVARCHAR(10),
    lunar_birthday_day          NVARCHAR(10),
    next_solar_birthday         NVARCHAR(20),
    reinstate_number            NVARCHAR(10),
    company_site_id             NVARCHAR(50),
    company_site_name           NVARCHAR(200),
    country                     NVARCHAR(50),
    is_probation                NVARCHAR(10),
    probation_status            NVARCHAR(20),
    create_time                 DATETIME        DEFAULT GETDATE(),
    update_time                 DATETIME        DEFAULT GETDATE()
);
GO

-- 6. 员工自定义属性表 (flexAttributes)
IF OBJECT_ID('dbo.employee_flex_attributes', 'U') IS NOT NULL DROP TABLE dbo.employee_flex_attributes;
CREATE TABLE dbo.employee_flex_attributes (
    id              NVARCHAR(50)    PRIMARY KEY,
    d_code_type_1   NVARCHAR(200),
    d_code_type_2   NVARCHAR(200),
    d_code_type_3   NVARCHAR(200),
    d_code_type_4   NVARCHAR(200),
    d_boolean_0     NVARCHAR(200),
    d_code_type_5   NVARCHAR(200),
    d_code_type_6   NVARCHAR(200),
    d_date_2        NVARCHAR(200),
    d_code_type_7   NVARCHAR(200),
    d_code_type_8   NVARCHAR(200),
    d_code_type_9   NVARCHAR(200),
    create_time     DATETIME        DEFAULT GETDATE(),
    update_time     DATETIME        DEFAULT GETDATE()
);
GO

-- 7. 员工自定义子集表 (tab_staff_subset04)
IF OBJECT_ID('dbo.employee_tab_staff_subset04', 'U') IS NOT NULL DROP TABLE dbo.employee_tab_staff_subset04;
CREATE TABLE dbo.employee_tab_staff_subset04 (
    staff_id        NVARCHAR(50)    PRIMARY KEY,
    d_code_type_0   NVARCHAR(200),
    d_code_type_1   NVARCHAR(200),
    d_code_type_2   NVARCHAR(200),
    d_string_1      NVARCHAR(200),
    d_code_type_3   NVARCHAR(200),
    d_code_type_4   NVARCHAR(200),
    d_code_type_5   NVARCHAR(200),
    d_string_0      NVARCHAR(200),
    d_code_type_6   NVARCHAR(200),
    create_time     DATETIME        DEFAULT GETDATE(),
    update_time     DATETIME        DEFAULT GETDATE()
);
GO

-- 8. 考勤周期设置表
IF OBJECT_ID('dbo.attendance_settings', 'U') IS NOT NULL DROP TABLE dbo.attendance_settings;
CREATE TABLE dbo.attendance_settings (
    id                  NVARCHAR(50)    PRIMARY KEY,
    period_name         NVARCHAR(100),
    period_start_month  NVARCHAR(20),
    period_start_day    NVARCHAR(20),
    period_end_month    NVARCHAR(20),
    period_end_day      NVARCHAR(20),
    default_period      NVARCHAR(10),
    enabled             NVARCHAR(10),
    create_time         DATETIME        DEFAULT GETDATE(),
    update_time         DATETIME        DEFAULT GETDATE()
);
GO

-- 9. 考勤数据表
IF OBJECT_ID('dbo.attendance', 'U') IS NOT NULL DROP TABLE dbo.attendance;
CREATE TABLE dbo.attendance (
    id                          NVARCHAR(50)    PRIMARY KEY,
    staff_id                    NVARCHAR(50),
    supposed_attendance_days    NVARCHAR(10),
    supposed_attendance_hours   NVARCHAR(10),
    actual_attendance_days      NVARCHAR(10),
    actual_attendance_hours     NVARCHAR(10),
    rest_days                   NVARCHAR(10),
    absence_hours               NVARCHAR(10),
    absence_number              NVARCHAR(10),
    absence_times               NVARCHAR(10),
    appeal_times                NVARCHAR(10),
    late_minutes                NVARCHAR(10),
    late_times                  NVARCHAR(10),
    early_minutes               NVARCHAR(10),
    early_times                 NVARCHAR(10),
    sign_in_missing_times       NVARCHAR(10),
    sign_out_missing_times      NVARCHAR(10),
    business_travel_days        NVARCHAR(10),
    field_work_hours            NVARCHAR(10),
    normal_hour                 NVARCHAR(10),
    weekend_hour                NVARCHAR(10),
    statutory_hour              NVARCHAR(10),
    normal_to_rest_hour         NVARCHAR(10),
    weekend_to_rest_hour        NVARCHAR(10),
    statutory_to_rest_hour      NVARCHAR(10),
    normal_to_salary_hour       NVARCHAR(10),
    weekend_to_salary_hour      NVARCHAR(10),
    statutory_to_salary_hour    NVARCHAR(10),
    workday_delay_hours         NVARCHAR(10),
    day_off_delay_hours         NVARCHAR(10),
    holiday_delay_hours         NVARCHAR(10),
    deep_night_duration         NVARCHAR(10),
    annual_leave                NVARCHAR(10),
    personal_leave              NVARCHAR(10),
    breastfeeding_leave         NVARCHAR(10),
    compensated_leave           NVARCHAR(10),
    mourning_leave              NVARCHAR(10),
    full_pay_sick_leave         NVARCHAR(10),
    paid_sick_leave             NVARCHAR(10),
    paternity_leave             NVARCHAR(10),
    home_leave                  NVARCHAR(10),
    marriage_leave              NVARCHAR(10),
    maternity_leave             NVARCHAR(10),
    other_vacation              NVARCHAR(10),
    prenatal_checkup            NVARCHAR(10),
    remark                      NVARCHAR(500),
    update_date                 NVARCHAR(20),
    create_time                 DATETIME        DEFAULT GETDATE(),
    update_time                 DATETIME        DEFAULT GETDATE()
);
GO

-- 10. 每日新增员工ID表
IF OBJECT_ID('dbo.employee_additions', 'U') IS NOT NULL DROP TABLE dbo.employee_additions;
CREATE TABLE dbo.employee_additions (
    id              BIGINT          IDENTITY(1,1) PRIMARY KEY,
    employees_id    NVARCHAR(50)    NOT NULL,
    sync_date       DATETIME        NOT NULL,
    create_time     DATETIME        DEFAULT GETDATE()
);
GO

-- 11. 每日调整员工ID表
IF OBJECT_ID('dbo.employee_modifications', 'U') IS NOT NULL DROP TABLE dbo.employee_modifications;
CREATE TABLE dbo.employee_modifications (
    id              BIGINT          IDENTITY(1,1) PRIMARY KEY,
    staff_id        NVARCHAR(50)    NOT NULL,
    last_update     DATETIME,
    sync_date       DATETIME        NOT NULL,
    create_time     DATETIME        DEFAULT GETDATE()
);
GO

PRINT '===== BC_SPORTS_IHR 数据库表创建完成 =====';
GO
