package com.bcsport.admin.task.erp;

import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * IHR员工 → 伯俊ERP员工(表14630) 字段映射转换器
 * <p>
 * 2026-08-18 伯俊升级 REST v3：ObjectModify 定位参数 ak 废弃，改为
 * id(记录数字主键, Number) + partial_update=true(只更新传入字段, 不传则未传字段清空)。
 * 新增(ObjectCreate)协议未变，仍以 NO=工号 走唯一索引。
 * <p>
 * 变更报文策略（用户口径）：不做字段级差异比对，**全字段以 IHR 最新数据覆盖**——
 * partial_update=true 保证未传字段(入职日期/邮箱等)不受影响；
 * 手机号 IHR 为空时不传，避免 IHR 缺数清掉 ERP 已有手机号。
 */
public class IhrToBjErpConverter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 将IHR员工详情转为伯俊ERP员工字段（新增用，协议未变）
     */
    public static JSONObject toCreateParams(IhrEmployeeDetail detail) {
        JSONObject data = new JSONObject();

        // NO - 工号
        data.set("NO", detail.getStaffNo());
        // NAME - 姓名 【必填】
        data.set("NAME", detail.getStaffName());

        // C_CUSTOMER_ID__NAME - 经销商 【必填】固定值
        data.set("C_CUSTOMER_ID__NAME", "边城体育");
        // C_STORE_ID__NAME - 店仓 【必填】取 IHR 员工部门（与变更逻辑一致，不再写死）
        data.set("C_STORE_ID__NAME", detail.getDepartmentName());

        // INCUMBENCY_STS - 在职状况 【必填】
        data.set("INCUMBENCY_STS", mapStaffStatus(detail.getStaffStatus()));
        // ISSALER - 是否营业员（Y=营业员）。同步的终端店铺员工均为营业员
        data.set("ISSALER", "Y");
        // ISSTOREMAN - 是否店长（Y=店长）。职位精确匹配"店长"时为Y，否则留空（ERP 侧规范存为 N）
        data.set("ISSTOREMAN", "店长".equals(detail.getPositionName()) ? "Y" : "");

        // BEGIN_DATE - 入职日期
        putIfNotNull(data, "BEGIN_DATE", formatDate(detail.getEnrollInDate()));
        // HANDSET - 手机号
        putIfNotNull(data, "HANDSET", detail.getMobileNo());
        // EMAIL - 电子邮件
        putIfNotNull(data, "EMAIL", firstNonNull(detail.getEmail(), detail.getWorkEmail()));

        // IS_NEED_SYNC_CRM - 是否需要同步CRM（入职同步ERP时固定传Y）
        data.set("IS_NEED_SYNC_CRM", "Y");

        return data;
    }

    /**
     * 将IHR员工详情转为伯俊ERP员工修改参数（REST v3：id 定位 + 全字段 IHR 最新覆盖）
     * <p>
     * 变更同步以 IHR 为准全字段覆盖（NAME/经销商/店仓/在职状况/营业员/店长/手机号），
     * 调用方须先校验 IHR 部门名在 ERP 店仓精确存在（漂移如淮南/济南时明确失败等人工核正）。
     *
     * @param detail IHR员工详情（最新）
     * @param erpId  ERP记录数字ID（HR_EMPLOYEE.ID，工号定位）
     */
    public static JSONObject toModifyParams(IhrEmployeeDetail detail, Long erpId) {
        JSONObject data = new JSONObject();
        data.set("table", "14630");
        // REST v3: id 数字定位 + partial_update 必须为 true，否则未传字段会被清空（实测验证）
        data.set("id", erpId);
        data.set("partial_update", true);

        data.set("NAME", detail.getStaffName());
        data.set("C_CUSTOMER_ID__NAME", "边城体育");
        data.set("C_STORE_ID__NAME", detail.getDepartmentName());
        data.set("INCUMBENCY_STS", mapStaffStatus(detail.getStaffStatus()));
        data.set("ISSALER", "Y");
        data.set("ISSTOREMAN", "店长".equals(detail.getPositionName()) ? "Y" : "");

        // 手机号：IHR 非空才传（IHR 缺数不清空 ERP 已有值）；其余未传字段由 partial_update 保护
        putIfNotBlank(data, "HANDSET", detail.getMobileNo());

        return data;
    }

    /**
     * 离职员工修改参数（只更新在职状态和离职日期），REST v3 id 协议（同步迁移，功能仍禁用）
     */
    public static JSONObject toLeavingParams(IhrEmployeeDetail detail, Long erpId) {
        JSONObject data = new JSONObject();
        data.set("table", "14630");
        data.set("id", erpId);
        data.set("partial_update", true);
        data.set("INCUMBENCY_STS", "离职");
        putIfNotNull(data, "END_DATE", formatDate(detail.getLeaveDate()));
        return data;
    }

    // ==================== 工具方法 ====================

    /**
     * IHR staffStatus → 伯俊 INCUMBENCY_STS
     */
    private static String mapStaffStatus(String staffStatus) {
        if (staffStatus == null) return null;
        switch (staffStatus) {
            case "IN_SERVICE": return "在职";
            case "PROBATION": return "试用";
            case "LEAVED":
            case "QUIT":       return "离职";   // IHR 离职有两种状态值：LEAVED 和 QUIT
            default: return staffStatus;
        }
    }

    /**
     * 格式化日期为伯俊要求的 yyyyMMdd 格式
     * 支持 yyyy-MM-dd 和 yyyyMMdd 两种输入格式
     */
    private static String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        // 已经是 yyyyMMdd 格式
        if (dateStr.matches("\\d{8}")) return dateStr;
        // yyyy-MM-dd 格式
        if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                LocalDate ld = LocalDate.parse(dateStr);
                return ld.format(DATE_FMT);
            } catch (Exception e) {
                return dateStr.replace("-", "");
            }
        }
        return dateStr;
    }

    private static void putIfNotNull(JSONObject data, String key, String value) {
        if (value != null && !value.isEmpty()) {
            data.set(key, value);
        }
    }

    /** 空白(null/空串/纯空格)不传，防止 IHR 缺数清掉 ERP 已有手机号 */
    private static void putIfNotBlank(JSONObject data, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            data.set(key, value);
        }
    }

    private static String firstNonNull(String a, String b) {
        if (a != null && !a.isEmpty()) return a;
        return b;
    }
}
