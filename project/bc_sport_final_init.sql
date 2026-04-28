-- ==========================================================
-- BC体育巅峰后台管理系统 - 终极全量初始化脚本 (Oracle 11g/12c/19c)
-- ==========================================================

-- 1. 清理已存在的表
BEGIN
   FOR r IN (SELECT table_name FROM user_tables WHERE table_name LIKE 'BC_SPORTS_SYS_%') LOOP
      EXECUTE IMMEDIATE 'DROP TABLE ' || r.table_name || ' CASCADE CONSTRAINTS';
   END LOOP;
END;
/

-- 2. 创建核心表结构

-- 部门表
CREATE TABLE bc_sports_sys_dept (
    id VARCHAR2(64) PRIMARY KEY,
    parent_id VARCHAR2(64) DEFAULT '0',
    dept_name VARCHAR2(50) NOT NULL,
    sort NUMBER(10) DEFAULT 0,
    leader VARCHAR2(50),
    phone VARCHAR2(20),
    email VARCHAR2(100),
    status NUMBER(1) DEFAULT 1,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0
);

-- 用户表
CREATE TABLE bc_sports_sys_user (
    id VARCHAR2(64) PRIMARY KEY,
    username VARCHAR2(50) NOT NULL UNIQUE,
    password VARCHAR2(100) NOT NULL,
    salt VARCHAR2(64),
    nickname VARCHAR2(50),
    email VARCHAR2(100),
    phone VARCHAR2(20),
    avatar VARCHAR2(200),
    status NUMBER(1) DEFAULT 1,
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR2(50),
    remark VARCHAR2(500),
    dept_id VARCHAR2(64),
    sort NUMBER(10) DEFAULT 0,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0
);

-- 角色表
CREATE TABLE bc_sports_sys_role (
    id VARCHAR2(64) PRIMARY KEY,
    role_name VARCHAR2(50) NOT NULL,
    role_code VARCHAR2(50) NOT NULL UNIQUE,
    description VARCHAR2(200),
    status NUMBER(1) DEFAULT 1,
    sort NUMBER(10) DEFAULT 0,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0
);

-- 菜单/按钮权限表
CREATE TABLE bc_sports_sys_menu (
    id VARCHAR2(64) PRIMARY KEY,
    parent_id VARCHAR2(64) DEFAULT '0',
    menu_name VARCHAR2(50) NOT NULL,
    icon VARCHAR2(50),
    menu_type NUMBER(1), -- 0:目录, 1:菜单, 2:按钮
    path VARCHAR2(200),
    permission VARCHAR2(100),
    sort NUMBER(10) DEFAULT 0,
    status NUMBER(1) DEFAULT 1,
    visible NUMBER(1) DEFAULT 1,
    description VARCHAR2(200),
    icon_color VARCHAR2(20),
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0
);

-- 字典类型表
CREATE TABLE bc_sports_sys_dict_type (
    id VARCHAR2(64) PRIMARY KEY,
    dict_name VARCHAR2(100) NOT NULL,
    dict_type VARCHAR2(100) NOT NULL UNIQUE,
    status NUMBER(1) DEFAULT 1,
    remark VARCHAR2(500),
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0
);

-- 字典数据表
CREATE TABLE bc_sports_sys_dict_data (
    id VARCHAR2(64) PRIMARY KEY,
    dict_type VARCHAR2(100) NOT NULL,
    dict_label VARCHAR2(100) NOT NULL,
    dict_value VARCHAR2(100) NOT NULL,
    sort NUMBER(10) DEFAULT 0,
    status NUMBER(1) DEFAULT 1,
    remark VARCHAR2(500),
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0
);

-- 操作日志表
CREATE TABLE bc_sports_sys_log (
    id VARCHAR2(64) PRIMARY KEY,
    module VARCHAR2(50),
    operation VARCHAR2(50),
    method VARCHAR2(200),
    params VARCHAR(4000), -- 适配Oracle大文本
    ip VARCHAR2(50),
    user_id VARCHAR2(64),
    username VARCHAR2(50),
    operation_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    status NUMBER(1) DEFAULT 1,
    error_msg VARCHAR(4000)
);

-- 业务表：品牌管理
CREATE TABLE bc_sports_sys_brand (
    id VARCHAR2(64) PRIMARY KEY,
    brand_name VARCHAR2(100) NOT NULL,
    brand_logo VARCHAR2(200),
    description VARCHAR2(500),
    status NUMBER(1) DEFAULT 1,
    sort NUMBER(10) DEFAULT 0,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0
);

-- 业务表：客户管理
CREATE TABLE bc_sports_sys_customer (
    id VARCHAR2(64) PRIMARY KEY,
    customer_name VARCHAR2(100) NOT NULL,
    customer_code VARCHAR2(50),
    contact_person VARCHAR2(50),
    contact_phone VARCHAR2(20),
    contact_email VARCHAR2(100),
    address VARCHAR2(200),
    description VARCHAR2(500),
    status NUMBER(1) DEFAULT 1,
    sort NUMBER(10) DEFAULT 0,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0
);

-- 业务表：仓库管理
CREATE TABLE bc_sports_sys_warehouse (
    id VARCHAR2(64) PRIMARY KEY,
    warehouse_name VARCHAR2(100) NOT NULL,
    warehouse_code VARCHAR2(50),
    contact_person VARCHAR2(50),
    contact_phone VARCHAR2(20),
    address VARCHAR2(200),
    description VARCHAR2(500),
    status NUMBER(1) DEFAULT 1,
    sort NUMBER(10) DEFAULT 0,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0
);

-- 创建序列
CREATE SEQUENCE bc_sports_seq_store START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE bc_sports_seq_customer START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE bc_sports_seq_warehouse START WITH 1 INCREMENT BY 1;

-- 业务表：店铺管理
CREATE TABLE bc_sports_sys_store (
    id VARCHAR2(64) PRIMARY KEY,
    store_name VARCHAR2(100) NOT NULL,
    store_code VARCHAR2(50),
    customer_id VARCHAR2(64),
    warehouse_id VARCHAR2(64),
    brand_id VARCHAR2(64),
    region_level1_id VARCHAR2(64),
    region_level2_id VARCHAR2(64),
    channel_type_id VARCHAR2(64),
    channel_definition_id VARCHAR2(64),
    channel_nature_id VARCHAR2(64),
    business_type NUMBER(1),
    description VARCHAR2(500),
    status NUMBER(1) DEFAULT 1,
    sort NUMBER(10) DEFAULT 0,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0
);

-- 业务表：地区管理
CREATE TABLE bc_sports_sys_region (
    id VARCHAR2(64) PRIMARY KEY,
    parent_id VARCHAR2(64) DEFAULT '0',
    region_name VARCHAR2(100) NOT NULL,
    region_code VARCHAR2(50),
    region_type NUMBER(1) DEFAULT 1,
    sort NUMBER(10) DEFAULT 0,
    status NUMBER(1) DEFAULT 1,
    remark VARCHAR2(500),
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0
);

-- 关联纽带表
CREATE TABLE bc_sports_sys_user_role (
    id VARCHAR2(64) PRIMARY KEY,
    user_id VARCHAR2(64) NOT NULL,
    role_id VARCHAR2(64) NOT NULL,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64)
);

CREATE TABLE bc_sports_sys_role_menu (
    id VARCHAR2(64) PRIMARY KEY,
    role_id VARCHAR2(64) NOT NULL,
    menu_id VARCHAR2(64) NOT NULL,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64)
);

-- 3. 注入系统核心数据

-- 3.1 预置超级管理员 (密码: admin123456, 盐值: bcsport)
INSERT INTO bc_sports_sys_user (id, username, password, salt, nickname, status)
VALUES ('1', 'admin', '80c818664c400530156aa5c803d85553', 'bcsport', '超级管理员', 1);

-- 3.2 预置超级角色
INSERT INTO bc_sports_sys_role (id, role_name, role_code, description, sort)
VALUES ('1', '超级管理员', 'admin', '掌握系统最高生杀大权', 1);

-- 3.3 建立关联
INSERT INTO bc_sports_sys_user_role (id, user_id, role_id, create_time, create_by) VALUES ('1', '1', '1', SYSTIMESTAMP, 'admin');

-- 4. 注入全量权限树

-- [系统管理]
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, visible) VALUES ('SYS_DIR', '0', '系统管理', 'bi-gear', 0, '/system', NULL, 10, 1);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, visible) VALUES ('SYS_USER', 'SYS_DIR', '用户管理', 'bi-people', 1, '/system/user', 'user:query', 1, 1);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('SYS_USER_ADD', 'SYS_USER', '用户新增', 2, 'user:add', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('SYS_USER_EDIT', 'SYS_USER', '用户修改', 2, 'user:edit', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('SYS_USER_DEL', 'SYS_USER', '用户删除', 2, 'user:delete', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('SYS_USER_RESET', 'SYS_USER', '密码重置', 2, 'user:resetPassword', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, visible) VALUES ('SYS_ROLE', 'SYS_DIR', '角色管理', 'bi-person-badge', 1, '/system/role', 'role:query', 2, 1);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('SYS_ROLE_ADD', 'SYS_ROLE', '角色新增', 2, 'role:add', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('SYS_ROLE_EDIT', 'SYS_ROLE', '角色修改', 2, 'role:edit', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('SYS_ROLE_DEL', 'SYS_ROLE', '角色删除', 2, 'role:delete', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, visible) VALUES ('SYS_MENU', 'SYS_DIR', '菜单管理', 'bi-menu-button-wide', 1, '/system/menu', 'menu:query', 3, 1);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('SYS_MENU_ADD', 'SYS_MENU', '菜单新增', 2, 'menu:add', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('SYS_MENU_EDIT', 'SYS_MENU', '菜单修改', 2, 'menu:edit', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('SYS_MENU_DEL', 'SYS_MENU', '菜单删除', 2, 'menu:delete', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, visible) VALUES ('SYS_DEPT', 'SYS_DIR', '部门管理', 'bi-diagram-3', 1, '/system/dept', 'dept:query', 4, 1);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('SYS_DEPT_ADD', 'SYS_DEPT', '部门新增', 2, 'dept:add', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('SYS_DEPT_EDIT', 'SYS_DEPT', '部门修改', 2, 'dept:edit', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('SYS_DEPT_DEL', 'SYS_DEPT', '部门删除', 2, 'dept:delete', 0);

-- [BI管理]
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, visible) VALUES ('BI_DIR', '0', 'BI管理', 'bi-pie-chart', 0, '/bi', NULL, 20, 1);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, visible) VALUES ('BI_BRAND', 'BI_DIR', '品牌管理', 'bi-tags', 1, '/bi/brand', 'bi:brand:query', 1, 1);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('BI_BRAND_ADD', 'BI_BRAND', '品牌新增', 2, 'bi:brand:add', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('BI_BRAND_EDIT', 'BI_BRAND', '品牌修改', 2, 'bi:brand:edit', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('BI_BRAND_DEL', 'BI_BRAND', '品牌删除', 2, 'bi:brand:delete', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, visible) VALUES ('BI_STORE', 'BI_DIR', '店铺管理', 'bi-shop', 1, '/bi/store', 'bi:store:query', 2, 1);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('BI_STORE_ADD', 'BI_STORE', '店铺新增', 2, 'bi:store:add', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('BI_STORE_EDIT', 'BI_STORE', '店铺修改', 2, 'bi:store:edit', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('BI_STORE_DEL', 'BI_STORE', '店铺删除', 2, 'bi:store:delete', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, visible) VALUES ('BI_REGION', 'BI_DIR', '地区管理', 'bi-geo-alt', 1, '/bi/region', 'bi:region:query', 3, 1);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('BI_REGION_ADD', 'BI_REGION', '地区新增', 2, 'bi:region:add', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('BI_REGION_EDIT', 'BI_REGION', '地区修改', 2, 'bi:region:edit', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('BI_REGION_DEL', 'BI_REGION', '地区删除', 2, 'bi:region:delete', 0);

-- 运维监控模块菜单
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, visible) VALUES ('MONITOR_DIR', '0', '运维监控', 'bi-activity', 0, '/monitor', NULL, 15, 1);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, visible) VALUES ('MONITOR_SCHEDULE', 'MONITOR_DIR', '定时任务', 'bi-clock-history', 1, '/monitor/schedule', 'monitor:schedule:query', 1, 1);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('MONITOR_SCHEDULE_ADD', 'MONITOR_SCHEDULE', '任务新增', 2, 'monitor:schedule:add', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('MONITOR_SCHEDULE_EDIT', 'MONITOR_SCHEDULE', '任务修改', 2, 'monitor:schedule:edit', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible) VALUES ('MONITOR_SCHEDULE_DEL', 'MONITOR_SCHEDULE', '任务删除', 2, 'monitor:schedule:delete', 0);

-- 定时任务相关表
CREATE TABLE bc_sports_sys_schedule_job (
    id               VARCHAR2(64)   PRIMARY KEY,
    job_name         VARCHAR2(100)  NOT NULL,
    task_key         VARCHAR2(100)  NOT NULL,
    cron_expression  VARCHAR2(100)  NOT NULL,
    status           NUMBER(1)      DEFAULT 0,
    remark           VARCHAR2(500),
    create_time      TIMESTAMP      DEFAULT SYSTIMESTAMP,
    update_time      TIMESTAMP      DEFAULT SYSTIMESTAMP,
    create_by        VARCHAR2(64),
    update_by        VARCHAR2(64),
    deleted          NUMBER(1)      DEFAULT 0
);

CREATE TABLE bc_sports_sys_schedule_log (
    id              VARCHAR2(64)   PRIMARY KEY,
    job_id          VARCHAR2(64)   NOT NULL,
    job_name        VARCHAR2(100),
    trigger_type    VARCHAR2(20)   DEFAULT 'CRON',
    status          NUMBER(1)      DEFAULT 1,
    execute_time    TIMESTAMP      DEFAULT SYSTIMESTAMP,
    finish_time     TIMESTAMP,
    duration        NUMBER(19)     DEFAULT 0,
    error_msg       VARCHAR2(4000),
    create_time     TIMESTAMP      DEFAULT SYSTIMESTAMP,
    create_by       VARCHAR2(64)
);

CREATE INDEX idx_schedule_job_status ON bc_sports_sys_schedule_job(status, deleted);
CREATE INDEX idx_schedule_log_job_id ON bc_sports_sys_schedule_log(job_id);
CREATE INDEX idx_schedule_log_time   ON bc_sports_sys_schedule_log(execute_time);

-- 5. 绑定所有菜单到超级管理员
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT rawtohex(sys_guid()), '1', id, SYSTIMESTAMP, 'admin' FROM bc_sports_sys_menu;

-- 6. 提交
COMMIT;
