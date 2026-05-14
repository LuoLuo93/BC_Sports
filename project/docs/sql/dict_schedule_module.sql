-- 定时任务模块字典
-- dict_value 作为模块编码（与后端 ScheduleTaskRegistry 中的 module 值一致）
-- 排序字段 sort 控制下拉框显示顺序

INSERT INTO bc_sports_sys_dict_type (id, dict_name, dict_type, status, remark, create_time, update_time, create_by, update_by, deleted)
VALUES ('DT_SCHEDULE_MODULE', '定时任务模块', 'sys_schedule_module', 1, '定时任务所属业务模块分类', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, create_by, update_by, deleted)
VALUES ('DD_SCH_MOD_IHR',  'sys_schedule_module', 'IHR 人力',   'IHR',  1, 1, 'IHR360 人事管理', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, create_by, update_by, deleted)
VALUES ('DD_SCH_MOD_QW',   'sys_schedule_module', '企业微信',   'QW',   2, 1, '企业微信通讯录/客户同步', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, create_by, update_by, deleted)
VALUES ('DD_SCH_MOD_YDKL', 'sys_schedule_module', '云盯客流',   'YDKL', 3, 1, '云盯客流/天气数据同步', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, create_by, update_by, deleted)
VALUES ('DD_SCH_MOD_DEMO', 'sys_schedule_module', '示例',       'DEMO', 4, 1, '演示/测试用', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, create_by, update_by, deleted)
VALUES ('DD_SCH_MOD_OTHER','sys_schedule_module', '其他',       'OTHER',5, 1, '其他自定义模块', SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0);

COMMIT;
