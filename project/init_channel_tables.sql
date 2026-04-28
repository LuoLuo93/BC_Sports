-- 渠道类型 & 渠道性质 建表/序列/菜单/权限 初始化脚本
-- 适用于Oracle数据库

-- ================================================
-- 1. 创建序列
-- ================================================

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE bc_sports_seq_channel_type';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -2289 THEN
         RAISE;
      END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE bc_sports_seq_channel_nature';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -2289 THEN
         RAISE;
      END IF;
END;
/

CREATE SEQUENCE bc_sports_seq_channel_type START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE bc_sports_seq_channel_nature START WITH 1 INCREMENT BY 1;

-- ================================================
-- 2. 创建表
-- ================================================

-- 删除已存在的表（如果存在）
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_channel_type CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_channel_nature CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

-- 创建渠道类型表 (树状结构)
CREATE TABLE bc_sports_sys_channel_type (
    id VARCHAR2(64) NOT NULL,
    parent_id VARCHAR2(64) DEFAULT '0',
    type_name VARCHAR2(100) NOT NULL,
    type_code VARCHAR2(50),
    sort NUMBER(10) DEFAULT 0,
    status NUMBER(1) DEFAULT 1,
    remark VARCHAR2(500),
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_channel_type PRIMARY KEY (id)
);

-- 创建渠道性质表 (树状结构)
CREATE TABLE bc_sports_sys_channel_nature (
    id VARCHAR2(64) NOT NULL,
    parent_id VARCHAR2(64) DEFAULT '0',
    nature_name VARCHAR2(100) NOT NULL,
    nature_code VARCHAR2(50),
    sort NUMBER(10) DEFAULT 0,
    status NUMBER(1) DEFAULT 1,
    remark VARCHAR2(500),
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_channel_nature PRIMARY KEY (id)
);

-- ================================================
-- 3. 插入菜单数据（放在BI管理/品牌管理下面）
-- ================================================

-- 渠道类型菜单
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) 
VALUES ('BI_CHANNEL_TYPE', 'BI_DIR', '渠道类型', 'bi-diagram-3', 1, '/bi/channel-type', 'bi:channelType:query', 3, 1, 1, NULL, NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) 
VALUES ('BI_CHANNEL_TYPE_ADD', 'BI_CHANNEL_TYPE', '渠道类型新增', NULL, 2, NULL, 'bi:channelType:add', 0, 1, 0, NULL, NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) 
VALUES ('BI_CHANNEL_TYPE_EDIT', 'BI_CHANNEL_TYPE', '渠道类型修改', NULL, 2, NULL, 'bi:channelType:edit', 0, 1, 0, NULL, NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) 
VALUES ('BI_CHANNEL_TYPE_DEL', 'BI_CHANNEL_TYPE', '渠道类型删除', NULL, 2, NULL, 'bi:channelType:delete', 0, 1, 0, NULL, NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 渠道性质菜单
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) 
VALUES ('BI_CHANNEL_NATURE', 'BI_DIR', '渠道性质', 'bi-layers', 1, '/bi/channel-nature', 'bi:channelNature:query', 4, 1, 1, NULL, NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) 
VALUES ('BI_CHANNEL_NATURE_ADD', 'BI_CHANNEL_NATURE', '渠道性质新增', NULL, 2, NULL, 'bi:channelNature:add', 0, 1, 0, NULL, NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) 
VALUES ('BI_CHANNEL_NATURE_EDIT', 'BI_CHANNEL_NATURE', '渠道性质修改', NULL, 2, NULL, 'bi:channelNature:edit', 0, 1, 0, NULL, NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted) 
VALUES ('BI_CHANNEL_NATURE_DEL', 'BI_CHANNEL_NATURE', '渠道性质删除', NULL, 2, NULL, 'bi:channelNature:delete', 0, 1, 0, NULL, NULL, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- ================================================
-- 4. 为超级管理员角色(id=1)分配权限
-- ================================================

-- 渠道类型权限
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CT_ROLE_MENU_01', '1', 'BI_CHANNEL_TYPE', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CT_ROLE_MENU_02', '1', 'BI_CHANNEL_TYPE_ADD', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CT_ROLE_MENU_03', '1', 'BI_CHANNEL_TYPE_EDIT', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CT_ROLE_MENU_04', '1', 'BI_CHANNEL_TYPE_DEL', SYSTIMESTAMP, 'admin');

-- 渠道性质权限
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CN_ROLE_MENU_01', '1', 'BI_CHANNEL_NATURE', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CN_ROLE_MENU_02', '1', 'BI_CHANNEL_NATURE_ADD', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CN_ROLE_MENU_03', '1', 'BI_CHANNEL_NATURE_EDIT', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CN_ROLE_MENU_04', '1', 'BI_CHANNEL_NATURE_DEL', SYSTIMESTAMP, 'admin');

-- ================================================
-- 5. 为普通用户角色(id=2043948036631126017)分配权限
-- ================================================

-- 渠道类型权限
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CT_ROLE_MENU_U01', '2043948036631126017', 'BI_CHANNEL_TYPE', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CT_ROLE_MENU_U02', '2043948036631126017', 'BI_CHANNEL_TYPE_ADD', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CT_ROLE_MENU_U03', '2043948036631126017', 'BI_CHANNEL_TYPE_EDIT', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CT_ROLE_MENU_U04', '2043948036631126017', 'BI_CHANNEL_TYPE_DEL', SYSTIMESTAMP, 'admin');

-- 渠道性质权限
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CN_ROLE_MENU_U01', '2043948036631126017', 'BI_CHANNEL_NATURE', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CN_ROLE_MENU_U02', '2043948036631126017', 'BI_CHANNEL_NATURE_ADD', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CN_ROLE_MENU_U03', '2043948036631126017', 'BI_CHANNEL_NATURE_EDIT', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by) VALUES ('CN_ROLE_MENU_U04', '2043948036631126017', 'BI_CHANNEL_NATURE_DEL', SYSTIMESTAMP, 'admin');

COMMIT;
