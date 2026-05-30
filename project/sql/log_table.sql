-- 操作日志表
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

-- 索引：按模块+时间查询
CREATE INDEX idx_sys_log_module_time ON bc_sports_sys_log (module, operation_time);

-- 索引：按操作人查询
CREATE INDEX idx_sys_log_username ON bc_sports_sys_log (username);

-- 索引：按操作时间清理
CREATE INDEX idx_sys_log_operation_time ON bc_sports_sys_log (operation_time);

COMMIT;
