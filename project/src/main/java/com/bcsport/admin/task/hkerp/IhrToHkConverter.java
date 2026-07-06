package com.bcsport.admin.task.hkerp;

import com.bcsport.admin.entity.hkerp.HkBasPersonnel;
import com.bcsport.admin.entity.hkerp.HkShopStockSportCity;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;
import com.bcsport.admin.hkerpmapper.HkBasPersonnelMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * IHR员工详情 → 旧版HK ERP Bas_Personnel 字段转换器
 * <p>
 * 移植自源项目 interfaceForHK 的 BasPersonnelController#insertNewPersonnel 组装逻辑：
 * 1. 通过部门名称查 HKERP Bas_DepartMent 得 departmentId（查不到兜底 BC000069）
 * 2. 通过部门名称查 HKERP Bas_Shop 得 shopId/stockId/sportCityId（判断是否销售人员）
 * 3. 组装 HkBasPersonnel（固定常量 + IHR字段 + 映射字段）
 * <p>
 * 与伯俊链路的 IhrToBjErpConverter 不同：伯俊按名称直接匹配 ERP 对象，
 * HK 链路必须通过 Bas_DepartMent / Bas_Shop 两张映射表转换 ID。
 * <p>
 * PersonnelID 生成：纯计算，不查库。前缀 + 序号补零凑满10位。
 * 前缀和起始序号由调用方（Service）一次性从 queryMaxNum 解析后传入，批量内固定前缀、序号自增。
 */
@Slf4j
@Component
public class IhrToHkConverter {

    /** 部门查不到时的兜底部门ID（源项目原值，企业微信部门） */
    private static final String DEFAULT_DEPARTMENT_ID = "BC000069";
    /** PersonnelID 总长度 */
    private static final int PERSONNEL_ID_LENGTH = 10;
    /** PersonnelID 前缀长度（前2位） */
    private static final int PREFIX_LENGTH = 2;
    /** PersonnelID 序号起始位置 */
    private static final int SEQ_START = 5;
    /** PersonnelID 序号结束位置（不含） */
    private static final int SEQ_END = 10;

    @Autowired
    private HkBasPersonnelMapper basPersonnelMapper;

    /**
     * 入职新增：组装 Bas_Personnel（全新员工用）
     *
     * @param detail        IHR员工详情
     * @param personnelPrefix PersonnelID 前2位前缀（批量内固定，由 Service 一次性从 queryMaxNum 取）
     * @param personnelSeq  PersonnelID 序号递增器（调用一次自增一次）
     */
    public HkBasPersonnel toCreate(IhrEmployeeDetail detail, String personnelPrefix, AtomicLong personnelSeq) {
        String departmentName = detail.getDepartmentName();

        // 1. 通过部门名称查 ERP 部门ID（查不到统一划到企业微信 BC000069）
        String departmentId = basPersonnelMapper.queryIdByDepartmentName(departmentName);
        if (departmentId == null) {
            departmentId = DEFAULT_DEPARTMENT_ID;
        }

        // 2. 查 ShopID/StockID/SportCityID（同时用于判断是否销售人员）
        HkShopStockSportCity shop = basPersonnelMapper.queryShopIdByDepartmentName(departmentName);

        // 3. 生成新 PersonnelID（纯计算：前缀 + 序号补零）
        String personnelId = buildPersonnelId(personnelPrefix, personnelSeq.incrementAndGet());

        // 4. 组装实体
        HkBasPersonnel p = new HkBasPersonnel();
        p.setCompanyId("BC");
        p.setPersonnelId(personnelId);
        p.setPersonnelCode(detail.getStaffNo());
        p.setPersonnelName(detail.getStaffName());
        p.setPersonnelTypeId("00000000000011");
        p.setDepartmentId(departmentId);
        // ERP Bas_Personnel.Sex 为 int 列，取值 0=男 1=女；IHR 存 MALE/FEMALE 枚举，需转换
        p.setSex(mapSex(detail.getSex()));
        p.setMobilePhone(detail.getMobileNo());
        p.setIsEmployee("1");
        p.setIsBuyer("0");
        p.setIsPicker("0");
        p.setIsSurveyor("0");
        p.setAllowUsed("1");
        p.setCheckState("1");
        p.setLockState("0");
        p.setSyncState("0");
        p.setIsRecipients("0");
        p.setLan("0");
        p.setPersonnelStatus("0");

        if (shop != null) {
            // 销售人员：开启收银员/营业员/仓管员等角色
            p.setShopId(shop.getShopId());
            p.setSportCityId(shop.getSportCityId());
            p.setStockId(shop.getStockId());
            p.setAllShopName(departmentName);
            p.setIsSdCashier("1");
            p.setIsAssistant("1");
            p.setIsEmCashier("1");
            p.setIsEmPerson("1");
            p.setIsStocker("1");
        } else {
            // 非销售人员：全部置空/0
            p.setShopId("");
            p.setSportCityId("");
            p.setStockId("");
            p.setAllShopName("");
            p.setIsAssistant("0");
            p.setIsSdCashier("0");
            p.setIsEmCashier("0");
            p.setIsEmPerson("0");
            p.setIsStocker("0");
        }
        return p;
    }

    /**
     * 从 queryMaxNum 的返回值解析出 PersonnelID 前2位前缀。
     *
     * @param maxNum 当前 Max(PersonnelID)，预期长度≥10
     * @return 前2位前缀，无效时返回 null
     */
    public static String extractPrefix(String maxNum) {
        if (maxNum == null || maxNum.length() < PERSONNEL_ID_LENGTH) {
            return null;
        }
        return maxNum.substring(0, PREFIX_LENGTH);
    }

    /**
     * 从 queryMaxNum 的返回值解析出 PersonnelID 后5位序号。
     *
     * @param maxNum 当前 Max(PersonnelID)，预期长度≥10
     * @return 后5位序号数值，无效或不可解析时返回 0
     */
    public static long extractSeq(String maxNum) {
        if (maxNum == null || maxNum.length() < PERSONNEL_ID_LENGTH) {
            return 0L;
        }
        try {
            return Long.parseLong(maxNum.substring(SEQ_START, SEQ_END));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 生成新 PersonnelID：前缀 + 序号补零凑满10位。
     */
    private static String buildPersonnelId(String prefix, long seq) {
        String suffix = String.valueOf(seq);
        StringBuilder sb = new StringBuilder(prefix);
        while (sb.length() + suffix.length() < PERSONNEL_ID_LENGTH) {
            sb.append('0');
        }
        sb.append(suffix);
        return sb.toString();
    }

    /**
     * 性别映射：IHR 枚举 → ERP int（0=男 1=女）。
     * 无法识别的值返回 null（由数据库列默认值兜底，避免写入脏数据）。
     */
    private static String mapSex(String ihrSex) {
        if (ihrSex == null) {
            return null;
        }
        switch (ihrSex.toUpperCase()) {
            case "MALE":   return "0";
            case "FEMALE": return "1";
            default:       return null;
        }
    }
}

