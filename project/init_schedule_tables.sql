-- ==========================================================
-- 定时任务管理模块 - 建表及菜单权限初始化 (Oracle)
-- ==========================================================

-- 1. 任务配置表
CREATE TABLE bc_sports_sys_schedule_job (
    id               VARCHAR2(64)   PRIMARY KEY,
    job_name         VARCHAR2(100)  NOT NULL,
    task_key         VARCHAR2(100)  NOT NULL,
    cron_expression  VARCHAR2(100)  NOT NULL,
    status           NUMBER(1)      DEFAULT 0,
    module           VARCHAR2(50),
    sort             NUMBER(10)     DEFAULT 0,
    remark           VARCHAR2(500),
    create_time      TIMESTAMP      DEFAULT SYSTIMESTAMP,
    update_time      TIMESTAMP      DEFAULT SYSTIMESTAMP,
    create_by        VARCHAR2(64),
    update_by        VARCHAR2(64),
    deleted          NUMBER(1)      DEFAULT 0
);

COMMENT ON TABLE  bc_sports_sys_schedule_job IS '定时任务配置表';
COMMENT ON COLUMN bc_sports_sys_schedule_job.job_name        IS '任务名称';
COMMENT ON COLUMN bc_sports_sys_schedule_job.task_key        IS '预设任务标识';
COMMENT ON COLUMN bc_sports_sys_schedule_job.cron_expression IS 'cron表达式';
COMMENT ON COLUMN bc_sports_sys_schedule_job.status          IS '状态(0暂停 1运行)';
COMMENT ON COLUMN bc_sports_sys_schedule_job.module          IS '任务模块(IHR/QW/DEMO/OTHER)';
COMMENT ON COLUMN bc_sports_sys_schedule_job.sort            IS '排序号';

CREATE INDEX idx_schedule_job_status ON bc_sports_sys_schedule_job(status, deleted);

-- 2. 执行日志表
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

COMMENT ON TABLE  bc_sports_sys_schedule_log IS '定时任务执行日志表';
COMMENT ON COLUMN bc_sports_sys_schedule_log.trigger_type IS '触发类型(CRON/MANUAL)';
COMMENT ON COLUMN bc_sports_sys_schedule_log.status      IS '执行状态(0失败 1成功)';
COMMENT ON COLUMN bc_sports_sys_schedule_log.duration    IS '执行耗时(毫秒)';

CREATE INDEX idx_schedule_log_job_id ON bc_sports_sys_schedule_log(job_id);
CREATE INDEX idx_schedule_log_time   ON bc_sports_sys_schedule_log(execute_time);

-- 3. 菜单权限数据
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, visible)
VALUES ('MONITOR_DIR', '0', '运维监控', 'bi-activity', 0, '/monitor', NULL, 15, 1);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, visible)
VALUES ('MONITOR_SCHEDULE', 'MONITOR_DIR', '定时任务', 'bi-clock-history', 1, '/monitor/schedule', 'monitor:schedule:query', 1, 1);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible)
VALUES ('MONITOR_SCHEDULE_ADD', 'MONITOR_SCHEDULE', '任务新增', 2, 'monitor:schedule:add', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible)
VALUES ('MONITOR_SCHEDULE_EDIT', 'MONITOR_SCHEDULE', '任务修改', 2, 'monitor:schedule:edit', 0);

INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, menu_type, permission, visible)
VALUES ('MONITOR_SCHEDULE_DEL', 'MONITOR_SCHEDULE', '任务删除', 2, 'monitor:schedule:delete', 0);

-- 4. 绑定到超级管理员角色 (Role ID = '1')
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (rawtohex(sys_guid()), '1', 'MONITOR_DIR', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (rawtohex(sys_guid()), '1', 'MONITOR_SCHEDULE', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (rawtohex(sys_guid()), '1', 'MONITOR_SCHEDULE_ADD', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (rawtohex(sys_guid()), '1', 'MONITOR_SCHEDULE_EDIT', SYSTIMESTAMP, 'admin');
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
VALUES (rawtohex(sys_guid()), '1', 'MONITOR_SCHEDULE_DEL', SYSTIMESTAMP, 'admin');

COMMIT;
