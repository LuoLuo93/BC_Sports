-- ============================================================
-- Agent 打印代理 - Oracle 建表脚本
-- ============================================================

-- 1. Agent 表
CREATE TABLE print_agent (
    id              VARCHAR2(64)    NOT NULL,
    agent_id        VARCHAR2(50)    NOT NULL,
    agent_name      VARCHAR2(100),
    printers        VARCHAR2(500),
    status          NUMBER(1)       DEFAULT 0 NOT NULL,
    last_heartbeat  TIMESTAMP,
    create_time     TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    update_time     TIMESTAMP,
    CONSTRAINT pk_print_agent PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_print_agent_id ON print_agent (agent_id);

COMMENT ON TABLE  print_agent IS '打印代理';
COMMENT ON COLUMN print_agent.id IS '主键';
COMMENT ON COLUMN print_agent.agent_id IS 'Agent唯一标识';
COMMENT ON COLUMN print_agent.agent_name IS 'Agent名称';
COMMENT ON COLUMN print_agent.printers IS '打印机列表（逗号分隔）';
COMMENT ON COLUMN print_agent.status IS '状态 0离线 1在线';
COMMENT ON COLUMN print_agent.last_heartbeat IS '最后心跳时间';
COMMENT ON COLUMN print_agent.create_time IS '创建时间';
COMMENT ON COLUMN print_agent.update_time IS '更新时间';

-- 2. 打印任务表
CREATE TABLE print_task (
    id              VARCHAR2(64)    NOT NULL,
    task_id         VARCHAR2(64)    NOT NULL,
    order_no        VARCHAR2(30),
    order_id        VARCHAR2(64),
    material_number VARCHAR2(100),
    material_name   VARCHAR2(200),
    style_number    VARCHAR2(100),
    color           VARCHAR2(50),
    brand_name      VARCHAR2(100),
    kind_name       VARCHAR2(100),
    size_name       VARCHAR2(50),
    print_qty       NUMBER(6)       DEFAULT 0 NOT NULL,
    template_file   VARCHAR2(200),
    printer_name    VARCHAR2(200),
    print_data      CLOB,
    agent_id        VARCHAR2(50),
    status          NUMBER(1)       DEFAULT 0 NOT NULL,
    error_msg       VARCHAR2(500),
    create_time     TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    print_time      TIMESTAMP,
    retry_count     NUMBER(3)       DEFAULT 0 NOT NULL,
    CONSTRAINT pk_print_task PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_print_task_id ON print_task (task_id);
CREATE INDEX idx_print_task_agent ON print_task (agent_id, status);
CREATE INDEX idx_print_task_order ON print_task (order_id);

COMMENT ON TABLE  print_task IS '打印任务';
COMMENT ON COLUMN print_task.id IS '主键';
COMMENT ON COLUMN print_task.task_id IS '任务ID';
COMMENT ON COLUMN print_task.order_no IS '申请单号';
COMMENT ON COLUMN print_task.order_id IS '申请单ID';
COMMENT ON COLUMN print_task.material_number IS '货号';
COMMENT ON COLUMN print_task.material_name IS '货品名称';
COMMENT ON COLUMN print_task.style_number IS '款号';
COMMENT ON COLUMN print_task.color IS '颜色';
COMMENT ON COLUMN print_task.brand_name IS '品牌';
COMMENT ON COLUMN print_task.kind_name IS '类别';
COMMENT ON COLUMN print_task.size_name IS '尺码';
COMMENT ON COLUMN print_task.print_qty IS '打印数量';
COMMENT ON COLUMN print_task.template_file IS '模板文件名';
COMMENT ON COLUMN print_task.printer_name IS '打印机名称';
COMMENT ON COLUMN print_task.print_data IS '打印数据JSON';
COMMENT ON COLUMN print_task.agent_id IS '目标Agent';
COMMENT ON COLUMN print_task.status IS '状态 0待打印 1打印中 2成功 3失败 4已暂停(断纸换纸后续打)';
COMMENT ON COLUMN print_task.error_msg IS '错误信息';
COMMENT ON COLUMN print_task.create_time IS '创建时间';
COMMENT ON COLUMN print_task.print_time IS '打印时间';
COMMENT ON COLUMN print_task.retry_count IS '重试次数';

COMMIT;
