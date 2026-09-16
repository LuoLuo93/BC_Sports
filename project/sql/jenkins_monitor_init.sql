-- ==========================================================
-- Jenkins构建监控初始化(BC_SPORTS 身份执行,幂等可重复)
-- 1. 状态表 BC_SPORTS_JENKINS_JOB_STATE(每任务一行,记上次完成构建号防重复告警)
-- 2. 配置项 jenkins.monitor.baseUrl(Jenkins地址,匿名只读API)
-- 3. 调度任务 sys.jenkins.monitor(默认每5分钟,FAIL_ONLY=任务自身异常也推群)
-- 配套代码: task/sys/JenkinsMonitorTask + ScheduleTaskRegistry注册
-- ==========================================================
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE;

-- 1. 状态表
DECLARE
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_cnt FROM user_tables WHERE table_name = 'BC_SPORTS_JENKINS_JOB_STATE';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE q'[
      CREATE TABLE BC_SPORTS_JENKINS_JOB_STATE (
          JOB_NAME           VARCHAR2(200) NOT NULL,
          LAST_BUILD_NUMBER  NUMBER,
          LAST_RESULT        VARCHAR2(20),
          LAST_BUILD_TIME    TIMESTAMP(6),
          LAST_DURATION_MS   NUMBER,
          LAST_SUCCESS_BUILD NUMBER,
          LAST_SUCCESS_TIME  TIMESTAMP(6),
          NEXT_BUILD_NUMBER  NUMBER,
          HEALTH_SCORE       NUMBER,
          COLOR              VARCHAR2(20),
          BUILDABLE          NUMBER(1),
          IN_QUEUE           NUMBER(1),
          JOB_URL            VARCHAR2(500),
          UPDATE_TIME        TIMESTAMP(6) DEFAULT SYSTIMESTAMP,
          CONSTRAINT PK_BC_JENKINS_JOB_STATE PRIMARY KEY (JOB_NAME)
      )]';
    EXECUTE IMMEDIATE 'COMMENT ON TABLE BC_SPORTS_JENKINS_JOB_STATE IS ''Jenkins构建监控状态(每任务一行,监控任务全量同步含停用,页面默认只显示启用)''';
    EXECUTE IMMEDIATE 'COMMENT ON COLUMN BC_SPORTS_JENKINS_JOB_STATE.JOB_NAME IS ''Jenkins任务名''';
    EXECUTE IMMEDIATE 'COMMENT ON COLUMN BC_SPORTS_JENKINS_JOB_STATE.LAST_BUILD_NUMBER IS ''上次看到的完成构建号''';
    EXECUTE IMMEDIATE 'COMMENT ON COLUMN BC_SPORTS_JENKINS_JOB_STATE.LAST_RESULT IS ''上次构建结果(SUCCESS/FAILURE/UNSTABLE/ABORTED/NOT_BUILT)''';
    EXECUTE IMMEDIATE 'COMMENT ON COLUMN BC_SPORTS_JENKINS_JOB_STATE.UPDATE_TIME IS ''上次更新时间''';
    DBMS_OUTPUT.PUT_LINE('BC_SPORTS_JENKINS_JOB_STATE created');
  ELSE
    DBMS_OUTPUT.PUT_LINE('BC_SPORTS_JENKINS_JOB_STATE already exists, skip');
  END IF;
END;
/

-- 2. 配置项
INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_by, update_by)
SELECT 'CFG_JENKINS_BASE_URL', 'jenkins.monitor.baseUrl', 'http://192.168.5.151:8085',
       'Jenkins地址', 'monitor', 1, 'Jenkins构建监控任务轮询用(匿名只读API,末尾不带斜杠)', 'admin', 'admin'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_config WHERE config_key = 'jenkins.monitor.baseUrl');

-- 3. 调度任务(每5分钟,启用)
INSERT INTO bc_sports_sys_schedule_job
  (ID, JOB_NAME, TASK_KEY, CRON_EXPRESSION, STATUS, REMARK, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY, DELETED, MODULE, SORT, PARAMS, NOTIFY_STRATEGY)
SELECT 'JOB_JENKINS_MONITOR', 'SYS-Jenkins构建监控', 'sys.jenkins.monitor', '0 */5 * * *', 1,
       'Jenkins构建失败企微群告警(5分钟轮询,首次只记基线;恢复成功也通知)', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0, 'SYS', 3, NULL, 'FAIL_ONLY'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_schedule_job WHERE TASK_KEY = 'sys.jenkins.monitor');

COMMIT;
EXIT;
