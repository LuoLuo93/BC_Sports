-- ==========================================================
-- 定时任务推送通知配置
-- ==========================================================

-- 添加企微群机器人webhook配置项
INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_time, create_by)
VALUES (rawtohex(sys_guid()), 'schedule.notify.webhookUrl', '', '企微群机器人Webhook', 'schedule', 1, '企业微信群机器人的webhook URL，用于接收定时任务执行通知', SYSTIMESTAMP, 'system');

-- 添加默认推送策略配置项
INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_time, create_by)
VALUES (rawtohex(sys_guid()), 'schedule.notify.defaultStrategy', 'FAIL_ONLY', '默认推送策略', 'schedule', 2, '新建定时任务时的默认推送策略(ALWAYS/FAIL_ONLY/DISABLED)', SYSTIMESTAMP, 'system');

-- 如需删除配置项：
-- DELETE FROM bc_sports_sys_config WHERE config_key LIKE 'schedule.notify.%';

COMMIT;
