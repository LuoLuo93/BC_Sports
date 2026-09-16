-- ==========================================================
-- Jenkins监控明细页升级(2026-09-16,BC_SPORTS 身份执行,幂等可重复)
-- 前置: jenkins_monitor_init.sql 已执行(状态表/配置/调度行)
-- 1. 状态表补充展示字段(任务状态色/健康分/耗时/最近成功/下一号/链接等)
-- 2. 菜单 MONITOR_JENKINS 由外链跳转改为内部明细页 /monitor/jenkins
--    (外链入口保留在页面头部"打开Jenkins控制台"按钮)
-- 已在生产库执行(2026-09-16)。
-- ==========================================================
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE;

DECLARE
  v_cnt NUMBER;
  PROCEDURE add_col(p_col VARCHAR2, p_type VARCHAR2, p_comment VARCHAR2) IS
  BEGIN
    SELECT COUNT(*) INTO v_cnt FROM user_tab_columns
     WHERE table_name = 'BC_SPORTS_JENKINS_JOB_STATE' AND column_name = p_col;
    IF v_cnt = 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE BC_SPORTS_JENKINS_JOB_STATE ADD ' || p_col || ' ' || p_type;
      EXECUTE IMMEDIATE 'COMMENT ON COLUMN BC_SPORTS_JENKINS_JOB_STATE.' || p_col || ' IS ''' || p_comment || '''';
      DBMS_OUTPUT.PUT_LINE(p_col || ' added');
    ELSE
      DBMS_OUTPUT.PUT_LINE(p_col || ' exists, skip');
    END IF;
  END;
BEGIN
  add_col('LAST_BUILD_TIME',    'TIMESTAMP(6)', '最近完成构建提交时间');
  add_col('LAST_DURATION_MS',   'NUMBER',       '最近完成构建耗时(毫秒)');
  add_col('LAST_SUCCESS_BUILD', 'NUMBER',       '最近成功构建号');
  add_col('LAST_SUCCESS_TIME',  'TIMESTAMP(6)', '最近成功构建时间');
  add_col('NEXT_BUILD_NUMBER',  'NUMBER',       '下一构建号');
  add_col('HEALTH_SCORE',       'NUMBER',       '健康分0-100');
  add_col('COLOR',              'VARCHAR2(20)', 'Jenkins状态色(blue/red/yellow/disabled/_anime)');
  add_col('BUILDABLE',          'NUMBER(1)',    '是否启用(1启用/0停用)');
  add_col('IN_QUEUE',           'NUMBER(1)',    '是否排队中');
  add_col('JOB_URL',            'VARCHAR2(500)','任务链接');
END;
/

UPDATE bc_sports_sys_menu
   SET PATH = '/monitor/jenkins',
       PERMISSION = 'monitor:jenkins:query',
       ICON = 'bi-card-list',
       DESCRIPTION = 'Jenkins任务状态明细(默认显示启用任务,含停用切换)',
       UPDATE_TIME = SYSTIMESTAMP
 WHERE ID = 'MONITOR_JENKINS'
   AND PATH LIKE 'http%';

COMMIT;
EXIT;
