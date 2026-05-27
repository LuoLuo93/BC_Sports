package com.bcsport.admin.task;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScheduleTaskRegistry {

    /**
     * 模块常量
     */
    public static final String MODULE_IHR = "IHR";
    public static final String MODULE_QYWX = "QW";
    public static final String MODULE_DEMO = "DEMO";
    public static final String MODULE_YDKL = "YDKL";
    public static final String MODULE_OTHER = "OTHER";
    public static final String MODULE_NXCRM = "NXCRM";

    private static final Map<String, TaskOption> TASK_MAP = new LinkedHashMap<>();

    static {
        // === IHR 人力模块 ===
        register("ihr.token.refresh", "IHR-刷新Token", "ihrTokenTask", "refresh", "刷新iHR360 OAuth Token", MODULE_IHR, 1);
        register("ihr.department", "IHR-同步部门", "ihrDepartmentTask", "sync", "从iHR360同步部门组织架构", MODULE_IHR, 2);
        register("ihr.employee.all", "IHR-同步员工数据", "ihrEmployeeTask", "syncAll", "按顺序同步员工ID→新增→调整→基本信息→详情", MODULE_IHR, 3);

        // === QW 企业微信模块 ===
        // === QW 企业微信基础信息模块 ===
        register("qywx.token.refresh", "QW-刷新Token", "qywxTokenTask", "refresh", "刷新企业微信AccessToken", MODULE_QYWX, 1);
        register("qywx.full.sync", "QW-一键同步", "qywxFullSyncTask", "syncAll", "一键同步：部门→成员→详情→扩展属性", MODULE_QYWX, 2);
        //register("qywx.department.sync", "QW-同步部门", "qywxDepartmentTask", "sync", "同步企业微信部门列表", MODULE_QYWX, 3);
        //register("qywx.department.member.sync", "QW-同步部门成员", "qywxDepartmentMemberTask", "sync", "同步企业微信部门成员列表", MODULE_QYWX, 4);
        //register("qywx.attrs.sync", "QW-同步成员扩展属性", "qywxAttrsBaseTask", "sync", "同步企业微信成员的extattr.attrs到VX_attrsBase表", MODULE_QYWX, 5);

        // === QW 企业微信开通客户联系成员 ===
        register("qywx.follow.user.detail.sync", "QW-一键同步客户联系成员及详情", "qywxFollowUserDetailSyncTask", "syncAll", "一键同步：客户联系成员→成员详情", MODULE_QYWX, 6);
        //register("qywx.follow.user.sync", "QW-同步客户联系成员", "qywxFollowUserTask", "sync", "同步配置了客户联系功能的成员列表", MODULE_QYWX, 7);
        //register("qywx.department.member.detail.sync.follow", "QW-同步成员详情[仅客户联系]", "qywxDepartmentMemberDetailTask", "syncFromFollowUser", "仅同步配置了客户联系功能的成员详情", MODULE_QYWX, 8);
        //register("qywx.customer.detail.sync", "QW-同步客户详情", "qywxCustomerDetailTask", "sync", "同步企业微信外部联系人详情", MODULE_QYWX, 9);
        register("qywx.group.chat.and.stat.sync", "QW-一键同步群聊相关", "qywxGroupChatAndStatSyncTask", "syncAll", "一键同步：客户联系成员→群聊列表→群聊统计→群发消息→朋友圈", MODULE_QYWX, 13);
        register("qywx.employee.lifecycle.sync", "QW-员工生命周期整合同步", "qywxEmployeeLifecycleSyncTask", "syncAll", "一键同步：企微基础信息→新员工入职→员工信息变更→员工离职", MODULE_QYWX, 14);
        //register("qywx.new.employee.sync", "QW-录入企微新员工", "qywxNewEmployeeSyncTask", "sync", "从IHR新入职员工数据同步到企微通讯录", MODULE_QYWX, 15);
        //register("qywx.employee.update.sync", "QW-更新企微员工信息", "qywxEmployeeUpdateSyncTask", "sync", "从IHR变更员工数据更新企微通讯录信息", MODULE_QYWX, 16);
        //register("qywx.employee.leave.sync", "QW-企微员工离职同步", "qywxEmployeeLeaveSyncTask", "sync", "从IHR离职员工数据删除企微通讯录成员", MODULE_QYWX, 17);

        //register("qywx.group.chat.sync", "QW-同步企微群", "qywxGroupChatTask", "sync", "同步企业微信群列表和群成员", MODULE_QYWX, 10);
        //register("qywx.group.chat.stat", "QW-同步群聊统计", "qywxGroupChatStatTask", "sync", "同步企业微信群聊统计数据", MODULE_QYWX, 11);
        //register("qywx.mass.message", "QW-同步群发消息", "qywxMassMessageTask", "sync", "同步企业微信群发消息记录", MODULE_QYWX, 12);
        //register("qywx.moment", "QW-同步朋友圈", "qywxMomentTask", "sync", "同步企业微信朋友圈", MODULE_QYWX, 14);

        // === QW 企业微信标签模块 ===
        register("qywx.tag.sync", "QW-同步企业标签库", "qywxCustomerTagTask", "syncTags", "从企微同步企业客户标签到本地", MODULE_QYWX, 20);






        // === YDKL 云盯客流模块 ===
        register("ydkl.sync.all", "云盯客流-一键同步", "ydSyncAllTask", "syncAll", "一键同步：客流数据→天气数据", MODULE_YDKL, 3);
        // register("ydkl.customerflow.sync", "云盯客流-同步客流数据", "ydCustomerFlowTask", "sync", "从云盯API同步昨日门店客流数据", MODULE_YDKL, 1);
        //  register("ydkl.weather.sync", "云盯客流-同步天气数据", "ydWeatherTask", "sync", "从云盯API同步昨日门店天气数据", MODULE_YDKL, 2);

        // === NXCRM 南讯CRM模块 ===
        register("nxcrm.token.refresh", "NXCRM-刷新Token", "nxcrmTokenRefreshTask", "refreshToken", "刷新南讯CRM AccessToken", MODULE_NXCRM, 1);
        register("nxcrm.fill.shopId", "NXCRM-填充shopId", "nxcrmTagScheduleTask", "fillShopId", "按nasOuid查询会员店铺信息并更新shopId", MODULE_NXCRM, 2);
        register("nxcrm.tag.category.sync", "NXCRM-同步标签分类", "nxcrmTagCategorySyncTask", "syncTagCategories", "从南讯CRM同步标签分类列表", MODULE_NXCRM, 3);
        register("nxcrm.order.sync", "NXCRM-同步订单", "nxcrmOrderSyncTask", "syncOrders", "从IHR同步订单到南讯CRM", MODULE_NXCRM, 4);
        register("nxcrm.tag.increment.sync", "NXCRM-同步增量标签", "nxcrmTagIncrementSyncTask", "syncIncrementTags", "从南讯CRM同步增量标签数据", MODULE_NXCRM, 5,
            new ParamDef("startTime", "开始时间", "date"),
            new ParamDef("endTime", "结束时间", "date"));
        register("nxcrm.member.tag.push", "NXCRM-推送会员标签", "nxcrmMemberTagPushTask", "pushMemberTags", "从IHR读取会员标签数据并推送到南讯CRM", MODULE_NXCRM, 6);

        // === 示例任务 ===
        register("demoTask.noParams", "示例任务-无参数", "demoTask", "noParams", "演示定时任务基本功能", MODULE_DEMO, 1);
        register("demoTask.withParams", "示例任务-带参数", "demoTask", "withParams", "演示带参数的定时任务", MODULE_DEMO, 2);
    }

    /**
     * 注册任务（带模块和排序）
     */
    public static void register(String taskKey, String name, String beanName, String methodName, String description, String module, int sort) {
        TASK_MAP.put(taskKey, new TaskOption(taskKey, name, beanName, methodName, description, module, sort));
    }

    /**
     * 注册任务（带模块、排序和参数定义）
     */
    public static void register(String taskKey, String name, String beanName, String methodName, String description, String module, int sort, ParamDef... paramDefs) {
        TaskOption option = new TaskOption(taskKey, name, beanName, methodName, description, module, sort);
        option.setParamDefs(paramDefs != null && paramDefs.length > 0 ? Arrays.asList(paramDefs) : null);
        TASK_MAP.put(taskKey, option);
    }

    /**
     * 注册任务（兼容旧版本，无模块和排序）
     */
    public static void register(String taskKey, String name, String beanName, String methodName, String description) {
        TASK_MAP.put(taskKey, new TaskOption(taskKey, name, beanName, methodName, description, MODULE_OTHER, 999));
    }

    public static TaskOption getTask(String taskKey) {
        return TASK_MAP.get(taskKey);
    }

    public static List<TaskOption> getAllTasks() {
        return new ArrayList<>(TASK_MAP.values());
    }

    @Data
    public static class TaskOption {
        private String taskKey;
        private String name;
        private String beanName;
        private String methodName;
        private String description;
        private String module;
        private Integer sort;
        private List<ParamDef> paramDefs;

        public TaskOption(String taskKey, String name, String beanName, String methodName, String description, String module, Integer sort) {
            this.taskKey = taskKey;
            this.name = name;
            this.beanName = beanName;
            this.methodName = methodName;
            this.description = description;
            this.module = module;
            this.sort = sort;
        }
    }

    @Data
    public static class ParamDef {
        private String key;
        private String label;
        private String type;

        public ParamDef(String key, String label, String type) {
            this.key = key;
            this.label = label;
            this.type = type;
        }
    }
}
