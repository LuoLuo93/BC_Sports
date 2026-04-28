-- BC体育后台管理系统数据库表结构
-- 适用于Oracle数据库

-- 删除已存在的表（如果存在）
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_user CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_role CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_menu CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_user_role CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_role_menu CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_dict_type CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_dict_data CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_log CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_dept CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_brand CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_region CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

-- 删除已存在的序列（如果存在）
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE bc_sports_seq_brand';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -2289 THEN
         RAISE;
      END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE bc_sports_seq_region';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -2289 THEN
         RAISE;
      END IF;
END;
/

-- 创建用户表
CREATE TABLE bc_sports_sys_user (
    id VARCHAR2(64) NOT NULL,
    username VARCHAR2(50) NOT NULL,
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
    deleted NUMBER(1) DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_user PRIMARY KEY (id),
    CONSTRAINT bc_sports_uk_sys_user_username UNIQUE (username)
);

-- 创建角色表
CREATE TABLE bc_sports_sys_role (
    id VARCHAR2(64) NOT NULL,
    role_name VARCHAR2(50) NOT NULL,
    role_code VARCHAR2(50) NOT NULL,
    description VARCHAR2(200),
    status NUMBER(1) DEFAULT 1,
    sort NUMBER(10) DEFAULT 0,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_role PRIMARY KEY (id),
    CONSTRAINT bc_sports_uk_sys_role_code UNIQUE (role_code)
);

-- 创建菜单表
CREATE TABLE bc_sports_sys_menu (
    id VARCHAR2(64) NOT NULL,
    parent_id VARCHAR2(64) DEFAULT '0',
    menu_name VARCHAR2(50) NOT NULL,
    icon VARCHAR2(50),
    menu_type NUMBER(1) DEFAULT 0,
    path VARCHAR2(200),
    component VARCHAR2(200),
    permission VARCHAR2(100),
    sort NUMBER(10) DEFAULT 0,
    status NUMBER(1) DEFAULT 1,
    visible NUMBER(1) DEFAULT 1,
    description VARCHAR2(200),
    custom_class VARCHAR2(50),
    icon_color VARCHAR2(20),
    badge VARCHAR2(10),
    badge_color VARCHAR2(20),
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_menu PRIMARY KEY (id)
);

-- 创建用户角色关联表
CREATE TABLE bc_sports_sys_user_role (
    id VARCHAR2(64) NOT NULL,
    user_id VARCHAR2(64) NOT NULL,
    role_id VARCHAR2(64) NOT NULL,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    CONSTRAINT bc_sports_pk_sys_user_role PRIMARY KEY (id),
    CONSTRAINT bc_sports_uk_user_role UNIQUE (user_id, role_id)
);

-- 创建角色菜单关联表
CREATE TABLE bc_sports_sys_role_menu (
    id VARCHAR2(64) NOT NULL,
    role_id VARCHAR2(64) NOT NULL,
    menu_id VARCHAR2(64) NOT NULL,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    CONSTRAINT bc_sports_pk_sys_role_menu PRIMARY KEY (id),
    CONSTRAINT bc_sports_uk_role_menu UNIQUE (role_id, menu_id)
);

-- 创建字典类型表
CREATE TABLE bc_sports_sys_dict_type (
    id VARCHAR2(64) NOT NULL,
    dict_name VARCHAR2(100) NOT NULL,
    dict_type VARCHAR2(100) NOT NULL,
    status NUMBER(1) DEFAULT 1,
    remark VARCHAR2(500),
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_dict_type PRIMARY KEY (id),
    CONSTRAINT bc_sports_uk_dict_type UNIQUE (dict_type)
);

-- 创建字典数据表
CREATE TABLE bc_sports_sys_dict_data (
    id VARCHAR2(64) NOT NULL,
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
    deleted NUMBER(1) DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_dict_data PRIMARY KEY (id)
);

-- 创建操作日志表
CREATE TABLE bc_sports_sys_log (
    id VARCHAR2(64) NOT NULL,
    module VARCHAR2(50),
    operation VARCHAR2(50),
    method VARCHAR2(200),
    params VARCHAR2(2000),
    ip VARCHAR2(50),
    user_id VARCHAR2(64),
    username VARCHAR2(50),
    operation_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    status NUMBER(1) DEFAULT 1,
    error_msg VARCHAR2(2000),
    CONSTRAINT bc_sports_pk_sys_log PRIMARY KEY (id)
);

-- 创建部门表
CREATE TABLE bc_sports_sys_dept (
    id VARCHAR2(64) NOT NULL,
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
    deleted NUMBER(1) DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_dept PRIMARY KEY (id)
);

-- 创建品牌表
CREATE SEQUENCE bc_sports_seq_brand START WITH 1 INCREMENT BY 1;
CREATE TABLE bc_sports_sys_brand (
    id VARCHAR2(64) NOT NULL,
    brand_name VARCHAR2(100) NOT NULL,
    brand_logo VARCHAR2(200),
    description VARCHAR2(500),
    status NUMBER(1) DEFAULT 1,
    sort NUMBER(10) DEFAULT 0,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_brand PRIMARY KEY (id)
);

-- 创建地区管理表 (树状结构)
CREATE SEQUENCE bc_sports_seq_region START WITH 1 INCREMENT BY 1;
CREATE TABLE bc_sports_sys_region (
    id VARCHAR2(64) NOT NULL,
    parent_id VARCHAR2(64) DEFAULT '0',
    region_name VARCHAR2(100) NOT NULL,
    region_code VARCHAR2(50),
    region_type NUMBER(1) DEFAULT 1, -- 1:一级地区, 2:二级地区, 3:三级地区
    sort NUMBER(10) DEFAULT 0,
    status NUMBER(1) DEFAULT 1,
    remark VARCHAR2(500),
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_region PRIMARY KEY (id)
);

-- 插入初始数据

-- 动态导出的系统最新数据

-- 表 bc_sports_sys_user 的数据
INSERT INTO bc_sports_sys_user (id, username, password, salt, nickname, email, phone, avatar, status, last_login_time, last_login_ip, remark, dept_id, sort, create_time, update_time, create_by, update_by, deleted) VALUES ('1', 'admin', '24dbb1466b59426658e644b2df4d9a28', 'f12cd76134534fc5', '超级管理员', NULL, '15261167827', NULL, 1, NULL, NULL, NULL, '2043944904329003010', 0, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:50:44', 'YYYY-MM-DD HH24:MI:SS'), NULL, 'admin', 0);
INSERT INTO bc_sports_sys_user (id, username, password, salt, nickname, email, phone, avatar, status, last_login_time, last_login_ip, remark, dept_id, sort, create_time, update_time, create_by, update_by, deleted) VALUES ('2043948466173992961', 'user', 'ccc475618097787d761ea67f382165f7', '745381717aa34feb', '普通用户', NULL, NULL, NULL, 1, NULL, NULL, NULL, '2043944904329003010', 1, TO_TIMESTAMP('2026-04-14 15:04:23', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 15:04:23', 'YYYY-MM-DD HH24:MI:SS'), 'admin', 'admin', 0);

-- 表 bc_sports_sys_role 的数据
INSERT INTO bc_sports_sys_role (id, role_name, role_code, description, status, sort, create_time, update_time, create_by, update_by, deleted) VALUES ('2043951069029031938', 'user', '123', NULL, 1, 0, TO_TIMESTAMP('2026-04-14 15:14:44', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 15:21:04', 'YYYY-MM-DD HH24:MI:SS'), 'admin', 'admin', 1);
INSERT INTO bc_sports_sys_role (id, role_name, role_code, description, status, sort, create_time, update_time, create_by, update_by, deleted) VALUES ('1', '超级管理员', 'admin', '掌握系统最高生杀大权', 1, 1, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_role (id, role_name, role_code, description, status, sort, create_time, update_time, create_by, update_by, deleted) VALUES ('2043948036631126017', '普通用户', 'user', NULL, 1, 1, TO_TIMESTAMP('2026-04-14 15:02:41', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 15:27:07', 'YYYY-MM-DD HH24:MI:SS'), 'admin', 'admin', 0);

-- 表 bc_sports_sys_menu 的数据
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_DIR', '0', '系统管理', 'bi-gear', 0, '/system', NULL, 10, 1, 1, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_USER', 'SYS_DIR', '用户管理', 'bi-people', 1, '/system/user', 'user:query', 1, 1, 1, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_USER_ADD', 'SYS_USER', '用户新增', NULL, 2, NULL, 'user:add', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_USER_EDIT', 'SYS_USER', '用户修改', NULL, 2, NULL, 'user:edit', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_USER_DEL', 'SYS_USER', '用户删除', NULL, 2, NULL, 'user:delete', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_USER_RESET', 'SYS_USER', '密码重置', NULL, 2, NULL, 'user:resetPassword', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_ROLE', 'SYS_DIR', '角色管理', 'bi-person-badge', 1, '/system/role', 'role:query', 2, 1, 1, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_ROLE_ADD', 'SYS_ROLE', '角色新增', NULL, 2, NULL, 'role:add', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_ROLE_EDIT', 'SYS_ROLE', '角色修改', NULL, 2, NULL, 'role:edit', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_ROLE_DEL', 'SYS_ROLE', '角色删除', NULL, 2, NULL, 'role:delete', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_MENU', 'SYS_DIR', '菜单管理', 'bi-menu-button-wide', 1, '/system/menu', 'menu:query', 99, 1, 1, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 15:36:32', 'YYYY-MM-DD HH24:MI:SS'), NULL, 'admin', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_MENU_ADD', 'SYS_MENU', '菜单新增', NULL, 2, NULL, 'menu:add', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_MENU_EDIT', 'SYS_MENU', '菜单修改', NULL, 2, NULL, 'menu:edit', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_MENU_DEL', 'SYS_MENU', '菜单删除', NULL, 2, NULL, 'menu:delete', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_DEPT', 'SYS_DIR', '部门管理', 'bi-diagram-3', 1, '/system/dept', 'dept:query', 3, 1, 1, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 15:36:45', 'YYYY-MM-DD HH24:MI:SS'), NULL, 'admin', 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_DEPT_ADD', 'SYS_DEPT', '部门新增', NULL, 2, NULL, 'dept:add', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_DEPT_EDIT', 'SYS_DEPT', '部门修改', NULL, 2, NULL, 'dept:edit', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('SYS_DEPT_DEL', 'SYS_DEPT', '部门删除', NULL, 2, NULL, 'dept:delete', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('BI_DIR', '0', 'BI管理', 'bi-pie-chart', 0, '/bi', NULL, 20, 1, 1, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('BI_BRAND', 'BI_DIR', '品牌管理', 'bi-tags', 1, '/bi/brand', 'bi:brand:query', 1, 1, 1, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('BI_BRAND_ADD', 'BI_BRAND', '品牌新增', NULL, 2, NULL, 'bi:brand:add', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('BI_BRAND_EDIT', 'BI_BRAND', '品牌修改', NULL, 2, NULL, 'bi:brand:edit', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('BI_BRAND_DEL', 'BI_BRAND', '品牌删除', NULL, 2, NULL, 'bi:brand:delete', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('BI_REGION', 'BI_DIR', '地区管理', 'bi-geo-alt', 1, '/bi/region', 'bi:region:query', 2, 1, 1, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('BI_REGION_ADD', 'BI_REGION', '地区新增', NULL, 2, NULL, 'bi:region:add', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('BI_REGION_EDIT', 'BI_REGION', '地区修改', NULL, 2, NULL, 'bi:region:edit', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) VALUES ('BI_REGION_DEL', 'BI_REGION', '地区删除', NULL, 2, NULL, 'bi:region:delete', 0, 1, 0, NULL, NULL, TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);

-- 表 bc_sports_sys_user_role 的数据
INSERT INTO bc_sports_sys_user_role (id, user_id, role_id, create_time, create_by) VALUES ('7b327d1963314282acdd7a45dccd0588', '1', '1', TO_TIMESTAMP('2026-04-14 14:50:35', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_user_role (id, user_id, role_id, create_time, create_by) VALUES ('1835dc2b685e470ea78b5883f9ea1c17', '2043948466173992961', '2043948036631126017', TO_TIMESTAMP('2026-04-14 15:04:23', 'YYYY-MM-DD HH24:MI:SS'), 'admin');

-- 表 bc_sports_sys_role_menu 的数据
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('83435D80442F4E7F9134D753CE76C03A', '1', 'BI_BRAND', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('21B4DA995E35483586B71D48927F9452', '1', 'BI_BRAND_ADD', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('558209D67F5045588B3B0034C955F562', '1', 'BI_BRAND_DEL', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('B8EA034C266B4D539F5A1CB47E8AC38E', '1', 'BI_BRAND_EDIT', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('72ED238B13BB49D2A2D77FE0C5D6674B', '1', 'BI_DIR', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('27CE82442FCE4336A5A78C645D4AE764', '1', 'BI_REGION', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('6F00507322DE48889D6814DE66BFED24', '1', 'BI_REGION_ADD', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('167A228BEC4441EF9AFDE416FF7F3168', '1', 'BI_REGION_DEL', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('701D3472664A49F1B0B401A38F56CD48', '1', 'BI_REGION_EDIT', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('9C8EAFAC043145D38F813648A3A2BFB5', '1', 'SYS_DEPT', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('6377B7BC86B5448DA5C145F5E5B9E54D', '1', 'SYS_DEPT_ADD', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('B4154731C3E746039D51B3EF4FE92B6D', '1', 'SYS_DEPT_DEL', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('AC907DC7EE5E4017836C23264FB159E8', '1', 'SYS_DEPT_EDIT', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('BD2A3ADD338B41879FDB18478F11D693', '1', 'SYS_DIR', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('45EB891E419D40049F4998DF2D6595DF', '1', 'SYS_MENU', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CA5729B70EB5427DA79E18C828B7B512', '1', 'SYS_MENU_ADD', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CD18BB80B76D448CA6449FC5E9C44E7A', '1', 'SYS_MENU_DEL', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('5CD11A9441F54E0D850CDFB0B2ACF378', '1', 'SYS_MENU_EDIT', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('070F831DE9E547B0A73B0A63CF984C14', '1', 'SYS_ROLE', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('B4AC6E70E55845DCA7E78FAE8D981208', '1', 'SYS_ROLE_ADD', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('F2B58690E08F4DBB9B0D304A5459390C', '1', 'SYS_ROLE_DEL', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('9A357BD7DE9A428D9DE4E3594052766E', '1', 'SYS_ROLE_EDIT', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('A535472066BE4326B1FF964A0A568A78', '1', 'SYS_USER', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('D3E756D45CDD4452AD93B485A72B0CD1', '1', 'SYS_USER_ADD', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('1A70C95B750940249BA07532570ECD84', '1', 'SYS_USER_DEL', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('FDBDEE6B2AF646E08B324F7748D7A2AF', '1', 'SYS_USER_EDIT', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('124E2B4CD86C467D9CC626ED952D87FE', '1', 'SYS_USER_RESET', TO_TIMESTAMP('2026-04-14 14:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('10225210a3a2476f8c96074d45f6e57a', '2043948036631126017', 'BI_DIR', TO_TIMESTAMP('2026-04-14 15:03:04', 'YYYY-MM-DD HH24:MI:SS'), NULL);
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('b7687d44a6f04c1baad04a3de12db46a', '2043948036631126017', 'BI_BRAND', TO_TIMESTAMP('2026-04-14 15:03:04', 'YYYY-MM-DD HH24:MI:SS'), NULL);
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('4755474d89bc439cb8e55ee19515715c', '2043948036631126017', 'BI_BRAND_DEL', TO_TIMESTAMP('2026-04-14 15:03:04', 'YYYY-MM-DD HH24:MI:SS'), NULL);
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('14f7421205374a498033ff6db9dd2230', '2043948036631126017', 'BI_BRAND_EDIT', TO_TIMESTAMP('2026-04-14 15:03:04', 'YYYY-MM-DD HH24:MI:SS'), NULL);
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('11ee23cb6c6b448781047d596f48fe7a', '2043948036631126017', 'BI_BRAND_ADD', TO_TIMESTAMP('2026-04-14 15:03:04', 'YYYY-MM-DD HH24:MI:SS'), NULL);
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('fb92fb5184504b92aa29ae671a4f499c', '2043948036631126017', 'BI_REGION', TO_TIMESTAMP('2026-04-14 15:03:04', 'YYYY-MM-DD HH24:MI:SS'), NULL);
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('162274c085b04de8ac7681487737ff5c', '2043948036631126017', 'BI_REGION_EDIT', TO_TIMESTAMP('2026-04-14 15:03:04', 'YYYY-MM-DD HH24:MI:SS'), NULL);
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('cdd0fc761b994ff5a4d931dea81b3a3f', '2043948036631126017', 'BI_REGION_ADD', TO_TIMESTAMP('2026-04-14 15:03:04', 'YYYY-MM-DD HH24:MI:SS'), NULL);
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('e1f48d38094c43c7b09ae6c500ed436d', '2043948036631126017', 'BI_REGION_DEL', TO_TIMESTAMP('2026-04-14 15:03:04', 'YYYY-MM-DD HH24:MI:SS'), NULL);

-- 表 bc_sports_sys_dict_type 的数据

-- 表 bc_sports_sys_dict_data 的数据

-- 表 bc_sports_sys_dept 的数据
INSERT INTO bc_sports_sys_dept (id, parent_id, dept_name, sort, leader, phone, email, status, create_time, update_time, create_by, update_by, deleted) VALUES ('2043944865334558722', '0', '边城体育用品股份有限公司', 0, NULL, NULL, NULL, 1, TO_TIMESTAMP('2026-04-14 14:50:05', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:50:05', 'YYYY-MM-DD HH24:MI:SS'), 'admin', 'admin', 0);
INSERT INTO bc_sports_sys_dept (id, parent_id, dept_name, sort, leader, phone, email, status, create_time, update_time, create_by, update_by, deleted) VALUES ('2043944904329003010', '2043944865334558722', '信息技术部', 0, NULL, NULL, NULL, 1, TO_TIMESTAMP('2026-04-14 14:50:14', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 14:50:14', 'YYYY-MM-DD HH24:MI:SS'), 'admin', 'admin', 0);

-- 表 bc_sports_sys_brand 的数据
INSERT INTO bc_sports_sys_brand (id, brand_name, brand_logo, description, status, sort, create_time, update_time, create_by, update_by, deleted) VALUES ('2', '诺诗兰', NULL, NULL, 1, 0, TO_TIMESTAMP('2026-04-14 15:06:05', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 15:06:05', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_brand (id, brand_name, brand_logo, description, status, sort, create_time, update_time, create_by, update_by, deleted) VALUES ('3', 'LOWA', NULL, NULL, 1, 0, TO_TIMESTAMP('2026-04-14 15:06:10', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 15:06:10', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_brand (id, brand_name, brand_logo, description, status, sort, create_time, update_time, create_by, update_by, deleted) VALUES ('4', 'LIKE', NULL, NULL, 1, 0, TO_TIMESTAMP('2026-04-14 15:06:16', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 15:06:16', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);

-- 表 bc_sports_sys_region 的数据
INSERT INTO bc_sports_sys_region (id, parent_id, region_name, region_code, region_type, sort, status, remark, create_time, update_time, create_by, update_by, deleted) VALUES ('1', '0', '销售东区', NULL, 1, 1, 1, NULL, TO_TIMESTAMP('2026-04-14 15:38:35', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 15:45:34', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_region (id, parent_id, region_name, region_code, region_type, sort, status, remark, create_time, update_time, create_by, update_by, deleted) VALUES ('2', '1', '沪杭广业务组', NULL, 2, 0, 1, NULL, TO_TIMESTAMP('2026-04-14 15:44:16', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 15:44:16', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_region (id, parent_id, region_name, region_code, region_type, sort, status, remark, create_time, update_time, create_by, update_by, deleted) VALUES ('3', '1', '苏皖省区', NULL, 2, 0, 1, NULL, TO_TIMESTAMP('2026-04-14 15:44:23', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 15:44:23', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);
INSERT INTO bc_sports_sys_region (id, parent_id, region_name, region_code, region_type, sort, status, remark, create_time, update_time, create_by, update_by, deleted) VALUES ('4', '0', '总部', NULL, 1, 0, 1, NULL, TO_TIMESTAMP('2026-04-14 15:45:18', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-04-14 15:45:18', 'YYYY-MM-DD HH24:MI:SS'), NULL, NULL, 0);

COMMIT;
