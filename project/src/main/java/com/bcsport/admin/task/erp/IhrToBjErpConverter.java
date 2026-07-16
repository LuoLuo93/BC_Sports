package com.bcsport.admin.task.erp;

import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * IHR员工 → 伯俊ERP员工(表14630) 字段映射转换器
 */
public class IhrToBjErpConverter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 将IHR员工详情转为伯俊ERP员工字段（新增用）
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

        // BEGIN_DATE - 入职日期
        putIfNotNull(data, "BEGIN_DATE", formatDate(detail.getEnrollInDate()));
        // HANDSET - 手机号
        putIfNotNull(data, "HANDSET", detail.getMobileNo());
        // EMAIL - 电子邮件
        putIfNotNull(data, "EMAIL", firstNonNull(detail.getEmail(), detail.getWorkEmail()));

        return data;
    }

    /**
     * 将IHR员工详情转为伯俊ERP员工字段（修改用，增量更新）
     * 只包含非空字段，避免覆盖ERP中的已有数据
     */
    public static JSONObject toModifyParams(IhrEmployeeDetail detail) {
        JSONObject data = new JSONObject();

        // ak - 伯俊框架的alternate key，用员工工号定位记录
        data.set("ak", detail.getStaffNo());
        // 必填字段（即使为空也必须传入，否则伯俊接口报错）
        data.set("NAME", detail.getStaffName());                          // 【必填】姓名
        data.set("C_CUSTOMER_ID__NAME", "边城体育");                        // 【必填】经销商（固定值）
        data.set("C_STORE_ID__NAME", detail.getDepartmentName());          // 【必填】店仓（IHR员工部门）
        data.set("INCUMBENCY_STS", mapStaffStatus(detail.getStaffStatus())); // 【必填】在职状况
        data.set("ISSALER", "Y");                                          // 营业员标识（与入职一致）

        // 只更新手机号，其他不动（不传部门字段 C_DEPARTMENT_ID__NAME，
        // 伯俊 C_DEPARTMENT 表为空，传部门会导致"部门不存在"报错）
        putIfNotNull(data, "HANDSET", detail.getMobileNo());               // 手机号

        return data;
    }

    /**
     * 离职员工修改参数（只更新在职状态和离职日期）
     */
    public static JSONObject toLeavingParams(IhrEmployeeDetail detail) {
        JSONObject data = new JSONObject();
        // ak - 伯俊框架的alternate key，用员工工号定位记录
        data.set("ak", detail.getStaffNo());
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
            case "LEAVED": return "离职";
            case "PROBATION": return "试用";
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

    private static String firstNonNull(String a, String b) {
        if (a != null && !a.isEmpty()) return a;
        return b;
    }
}
