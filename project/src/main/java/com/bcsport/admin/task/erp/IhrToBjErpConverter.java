package com.bcsport.admin.task.erp;

import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;
import com.bcsport.admin.vo.ErpEmployeeCurrent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * IHR员工 → 伯俊ERP员工(表14630) 字段映射转换器
 * <p>
 * 2026-08-18 伯俊升级 REST v3：ObjectModify 定位参数 ak 废弃，改为
 * id(记录数字主键, Number) + partial_update=true(只更新传入字段, 不传则未传字段清空)。
 * 新增(ObjectCreate)协议未变，仍以 NO=工号 走唯一索引。
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
        // ISSTOREMAN - 是否店长（Y=店长）。职位精确匹配"店长"时为Y，否则留空
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
     * 将IHR员工详情转为伯俊ERP员工修改参数（REST v3：id 定位 + partial_update 最小化发送）
     * <p>
     * 只发与 ERP 现值有差异的字段：
     * <ul>
     *   <li>店仓未变化不传 → IHR部门名与ERP店名漂移(如淮南/济南)不再阻塞手机号、状态等其他变动</li>
     *   <li>店仓变化(storeChanged=true)时才传 C_STORE_ID__NAME，调用方须先校验目标店名在ERP店仓存在</li>
     *   <li>手机号 IHR 为空不传（不清空 ERP 已有手机号，与旧口径一致）</li>
     *   <li>在职状况只在 ERP=离职(D) 而 IHR 在职时拉回，ERP 罕见码(E)保守不动</li>
     * </ul>
     *
     * @param detail       IHR员工详情
     * @param erpId        ERP记录数字ID（HR_EMPLOYEE.ID）
     * @param current      ERP现值快照
     * @param storeChanged 是否调店（IHR部门名≠ERP现店名 且 目标店名已在ERP校验存在）
     * @return params；除 table/id/partial_update 外无业务字段 = 无差异（调用方可不发请求直接标记成功）
     */
    public static JSONObject toModifyParams(IhrEmployeeDetail detail, Long erpId,
                                            ErpEmployeeCurrent current, boolean storeChanged) {
        JSONObject data = new JSONObject();
        data.set("table", "14630");
        // REST v3: id 数字定位 + partial_update 必须为 true，否则未传字段会被清空（实测验证）
        data.set("id", erpId);
        data.set("partial_update", true);

        // 姓名
        if (!eq(detail.getStaffName(), current.getName())) {
            data.set("NAME", detail.getStaffName());
        }

        // 店仓：只在调店时发送
        if (storeChanged) {
            data.set("C_STORE_ID__NAME", detail.getDepartmentName());
        }

        // 在职状况：ERP=D(离职) 而 IHR 在职/试用 → 拉回；其余码不主动覆盖
        String erpSts = current.getIncumbencySts();
        if ("D".equals(erpSts) && ("IN_SERVICE".equals(detail.getStaffStatus())
                || "PROBATION".equals(detail.getStaffStatus()))) {
            data.set("INCUMBENCY_STS", mapStaffStatus(detail.getStaffStatus()));
        }

        // 营业员标识：同步人群固定Y，现值非Y时拉齐
        if (!"Y".equals(nullToEmpty(current.getIssaler()))) {
            data.set("ISSALER", "Y");
        }

        // 店长标识：职位精确匹配"店长"为Y，否则空串；与现值不同才发
        String wantStoreMan = "店长".equals(detail.getPositionName()) ? "Y" : "";
        if (!eq(wantStoreMan, nullToEmpty(current.getIsstoreman()))) {
            data.set("ISSTOREMAN", wantStoreMan);
        }

        // 手机号：IHR非空且与现值不同才发；IHR为空不碰ERP已有手机号
        if (notBlank(detail.getMobileNo()) && !eq(detail.getMobileNo(), current.getHandset())) {
            data.set("HANDSET", detail.getMobileNo());
        }

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

    private static String firstNonNull(String a, String b) {
        if (a != null && !a.isEmpty()) return a;
        return b;
    }

    /** 空安全等值比较（两侧 null/空白视为相等，两侧 trim 后比较，兼容历史数据尾随空格） */
    private static boolean eq(String a, String b) {
        return nullToEmpty(a).trim().equals(nullToEmpty(b).trim());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
