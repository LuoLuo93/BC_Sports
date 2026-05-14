-- 字典类型表 + 字典数据表
-- 独立可执行脚本，带 DROP 容错

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_dict_data CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_dict_type CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

-- 字典类型表
CREATE TABLE bc_sports_sys_dict_type (
    id          VARCHAR2(64)  NOT NULL,
    dict_name   VARCHAR2(100) NOT NULL,
    dict_type   VARCHAR2(100) NOT NULL,
    status      NUMBER(1)     DEFAULT 1,
    remark      VARCHAR2(500),
    create_time TIMESTAMP     DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP     DEFAULT SYSTIMESTAMP,
    create_by   VARCHAR2(64),
    update_by   VARCHAR2(64),
    deleted     NUMBER(1)     DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_dict_type PRIMARY KEY (id),
    CONSTRAINT bc_sports_uk_dict_type     UNIQUE (dict_type)
);

COMMENT ON TABLE  bc_sports_sys_dict_type IS '字典类型表';
COMMENT ON COLUMN bc_sports_sys_dict_type.id        IS '主键ID';
COMMENT ON COLUMN bc_sports_sys_dict_type.dict_name  IS '字典名称';
COMMENT ON COLUMN bc_sports_sys_dict_type.dict_type  IS '字典编码（唯一标识）';
COMMENT ON COLUMN bc_sports_sys_dict_type.status     IS '状态 0-禁用 1-启用';
COMMENT ON COLUMN bc_sports_sys_dict_type.remark     IS '备注';
COMMENT ON COLUMN bc_sports_sys_dict_type.deleted    IS '逻辑删除 0-未删除 1-已删除';

-- 字典数据表
CREATE TABLE bc_sports_sys_dict_data (
    id          VARCHAR2(64)  NOT NULL,
    dict_type   VARCHAR2(100) NOT NULL,
    dict_label  VARCHAR2(100) NOT NULL,
    dict_value  VARCHAR2(100) NOT NULL,
    sort        NUMBER(10)    DEFAULT 0,
    status      NUMBER(1)     DEFAULT 1,
    remark      VARCHAR2(500),
    create_time TIMESTAMP     DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP     DEFAULT SYSTIMESTAMP,
    create_by   VARCHAR2(64),
    update_by   VARCHAR2(64),
    deleted     NUMBER(1)     DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_dict_data PRIMARY KEY (id)
);

COMMENT ON TABLE  bc_sports_sys_dict_data IS '字典数据表';
COMMENT ON COLUMN bc_sports_sys_dict_data.id         IS '主键ID';
COMMENT ON COLUMN bc_sports_sys_dict_data.dict_type   IS '所属字典编码';
COMMENT ON COLUMN bc_sports_sys_dict_data.dict_label  IS '字典标签（显示值）';
COMMENT ON COLUMN bc_sports_sys_dict_data.dict_value  IS '字典键值（实际值）';
COMMENT ON COLUMN bc_sports_sys_dict_data.sort        IS '排序';
COMMENT ON COLUMN bc_sports_sys_dict_data.status      IS '状态 0-禁用 1-启用';
COMMENT ON COLUMN bc_sports_sys_dict_data.deleted     IS '逻辑删除 0-未删除 1-已删除';

-- 初始示例数据
INSERT INTO bc_sports_sys_dict_type (id, dict_name, dict_type, status, remark, create_time, update_time, create_by, update_by, deleted)
VALUES ('DT_USER_SEX', '用户性别', 'sys_user_sex', 1, '系统用户性别属性标识', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_dict_type (id, dict_name, dict_type, status, remark, create_time, update_time, create_by, update_by, deleted)
VALUES ('DT_SHOW_HIDE', '显示状态', 'sys_show_hide', 1, '控制元素显示或隐藏', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_dict_type (id, dict_name, dict_type, status, remark, create_time, update_time, create_by, update_by, deleted)
VALUES ('DT_NOTICE_TYPE', '通知类型', 'sys_notice_type', 1, '公告、通知、运维警示等', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- sys_user_sex 数据
INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted) VALUES ('DD_SEX_M', 'sys_user_sex', '男', 'male', 1, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);
INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted) VALUES ('DD_SEX_F', 'sys_user_sex', '女', 'female', 2, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);
INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted) VALUES ('DD_SEX_U', 'sys_user_sex', '未知', 'unknown', 3, 0, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- sys_show_hide 数据
INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted) VALUES ('DD_SHOW_Y', 'sys_show_hide', '显示', '1', 1, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);
INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted) VALUES ('DD_SHOW_N', 'sys_show_hide', '隐藏', '0', 2, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- sys_notice_type 数据
INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted) VALUES ('DD_NOTICE_1', 'sys_notice_type', '通知', 'notice', 1, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);
INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted) VALUES ('DD_NOTICE_2', 'sys_notice_type', '公告', 'announcement', 2, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

COMMIT;
