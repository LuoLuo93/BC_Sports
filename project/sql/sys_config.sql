-- ============================================================
-- 系统配置表
-- ============================================================
CREATE TABLE bc_sports_sys_config (
  id            VARCHAR2(64)   NOT NULL,
  config_key    VARCHAR2(100)  NOT NULL,
  config_value  VARCHAR2(500),
  config_name   VARCHAR2(200),
  config_group  VARCHAR2(50),
  sort          NUMBER(4)      DEFAULT 0,
  remark        VARCHAR2(500),
  create_time   TIMESTAMP      DEFAULT SYSTIMESTAMP,
  update_time   TIMESTAMP      DEFAULT SYSTIMESTAMP,
  create_by     VARCHAR2(64),
  update_by     VARCHAR2(64),
  deleted       NUMBER(1)      DEFAULT 0,
  CONSTRAINT bc_sports_pk_sys_config PRIMARY KEY (id),
  CONSTRAINT bc_sports_uk_config_key UNIQUE (config_key)
);

COMMENT ON TABLE  bc_sports_sys_config IS '系统配置表';
COMMENT ON COLUMN bc_sports_sys_config.config_key   IS '配置键';
COMMENT ON COLUMN bc_sports_sys_config.config_value  IS '配置值';
COMMENT ON COLUMN bc_sports_sys_config.config_name   IS '配置名称';
COMMENT ON COLUMN bc_sports_sys_config.config_group  IS '配置分组: basic/security';
COMMENT ON COLUMN bc_sports_sys_config.sort          IS '排序';

-- ============================================================
-- 预置数据：基础配置
-- ============================================================
INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_by, update_by)
VALUES ('CFG_SYS_NAME',       'sys.name',       'BC体育数据管理系统',  '系统名称',      'basic',    1, '显示在页面标题和登录页', 'admin', 'admin');

INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_by, update_by)
VALUES ('CFG_PAGE_SIZE',      'sys.pageSize',    '20',                 '默认分页大小',  'basic',    2, '每页显示条数: 10/20/50/100', 'admin', 'admin');

INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_by, update_by)
VALUES ('CFG_DATE_FORMAT',    'sys.dateFormat',  'yyyy-MM-dd HH:mm:ss','日期格式',     'basic',    3, '系统统一日期显示格式', 'admin', 'admin');

INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_by, update_by)
VALUES ('CFG_TIMEZONE',       'sys.timezone',    'GMT+8',              '时区',         'basic',    4, '系统时区设置', 'admin', 'admin');

-- ============================================================
-- 预置数据：安全策略
-- ============================================================
INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_by, update_by)
VALUES ('CFG_PWD_MIN_LEN',    'security.passwordMinLength',    '6',     '密码最小长度',        'security', 1, '最少字符数', 'admin', 'admin');

INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_by, update_by)
VALUES ('CFG_PWD_UPPER',      'security.passwordRequireUpper', 'false', '密码必须包含大写字母', 'security', 2, 'true/false', 'admin', 'admin');

INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_by, update_by)
VALUES ('CFG_PWD_NUMBER',     'security.passwordRequireNumber','true',  '密码必须包含数字',    'security', 3, 'true/false', 'admin', 'admin');

INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_by, update_by)
VALUES ('CFG_LOGIN_RETRY',    'security.loginMaxRetry',        '5',     '登录失败锁定次数',    'security', 4, '连续失败N次后锁定', 'admin', 'admin');

INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_by, update_by)
VALUES ('CFG_LOGIN_LOCK',     'security.loginLockMinutes',     '30',    '锁定时间(分钟)',      'security', 5, '锁定持续时间', 'admin', 'admin');

INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_by, update_by)
VALUES ('CFG_SESSION_TIMEOUT','security.sessionTimeout',       '120',   '会话超时(分钟)',      'security', 6, '超过此时间未操作自动登出', 'admin', 'admin');

INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_by, update_by)
VALUES ('CFG_CAPTCHA_ENABLED','security.captchaEnabled',       'true',  '验证码开关',          'security', 7, '登录是否需要验证码', 'admin', 'admin');

-- ============================================================
-- 菜单权限
-- ============================================================
-- 系统设置菜单（不出现在侧边栏，由头像下拉进入）
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted)
VALUES ('SYS_CONFIG', '0', '系统设置', 'bi-gear', 1, '/settings', 'system:config:query', 99, 1, 0, '系统参数配置', '', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 查询权限按钮
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted)
VALUES ('SYS_CONFIG_QUERY', 'SYS_CONFIG', '查询配置', 2, 'system:config:query', 1, 1, 0, '', '', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 修改权限按钮
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, sort, status, visible, description, icon_color, create_time, update_time, create_by, update_by, deleted)
VALUES ('SYS_CONFIG_EDIT', 'SYS_CONFIG', '修改配置', 2, 'system:config:edit', 2, 1, 0, '', '', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

-- 给管理员角色(role_id=1)分配菜单权限
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (SYS_GUID(), '1', 'SYS_CONFIG', SYSTIMESTAMP, 'admin');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (SYS_GUID(), '1', 'SYS_CONFIG_QUERY', SYSTIMESTAMP, 'admin');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (SYS_GUID(), '1', 'SYS_CONFIG_EDIT', SYSTIMESTAMP, 'admin');

COMMIT;
