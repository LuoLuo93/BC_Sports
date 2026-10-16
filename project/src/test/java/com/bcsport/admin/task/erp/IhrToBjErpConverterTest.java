package com.bcsport.admin.task.erp;

import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REST v3 变更协议转换器单测。
 * 契约（用户口径）：变更不做字段级差异比对，**全字段以 IHR 最新数据覆盖**；
 * partial_update 必须 true（否则 ERP 会把未传字段清空——实测踩坑验证），未传字段由其保护。
 */
public class IhrToBjErpConverterTest {

    private static IhrEmployeeDetail detail(String staffNo, String name, String dept,
                                            String status, String position, String mobile) {
        IhrEmployeeDetail d = new IhrEmployeeDetail();
        d.setStaffNo(staffNo);
        d.setStaffName(name);
        d.setDepartmentName(dept);
        d.setStaffStatus(status);
        d.setPositionName(position);
        d.setMobileNo(mobile);
        return d;
    }

    @Test
    public void modifyAlwaysCarriesIdAndPartialUpdateTrue() {
        JSONObject p = IhrToBjErpConverter.toModifyParams(
                detail("20250704286", "陈士华", "济南银座振兴街NL", "IN_SERVICE", "导购", "15064059740"), 6603L);
        assertEquals(14630, p.getInt("table"));
        assertEquals(6603L, p.getLong("id"));
        // 保命栓：partial_update 必须 true，否则 ERP 会把未传字段清空
        assertEquals(Boolean.TRUE, p.getBool("partial_update"));
    }

    @Test
    public void modifySendsFullIhrFieldSet() {
        // 全字段覆盖：姓名/经销商/店仓/在职状况/营业员/店长/手机号 一次性按 IHR 最新值发送
        JSONObject p = IhrToBjErpConverter.toModifyParams(
                detail("20250704286", "陈士华", "济南银座振兴街NL", "IN_SERVICE", "导购", "15064059740"), 6603L);
        assertEquals("陈士华", p.getStr("NAME"));
        assertEquals("边城体育", p.getStr("C_CUSTOMER_ID__NAME"));
        assertEquals("济南银座振兴街NL", p.getStr("C_STORE_ID__NAME"));
        assertEquals("在职", p.getStr("INCUMBENCY_STS"));
        assertEquals("Y", p.getStr("ISSALER"));
        assertEquals("", p.getStr("ISSTOREMAN"));
        assertEquals("15064059740", p.getStr("HANDSET"));
        assertNull(p.getStr("ak"));
    }

    @Test
    public void modifyStoreManagerFlagFollowsIhrPosition() {
        JSONObject mgr = IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "A店", "IN_SERVICE", "店长", "138"), 1L);
        assertEquals("Y", mgr.getStr("ISSTOREMAN"));

        JSONObject staff = IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "A店", "IN_SERVICE", "副店长", "138"), 1L);
        assertEquals("", staff.getStr("ISSTOREMAN"));  // 非精确"店长"一律非店长，ERP 侧落为 N
    }

    @Test
    public void modifyStatusMappedFromIhr() {
        assertEquals("在职", IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "A店", "IN_SERVICE", "导购", "138"), 1L).getStr("INCUMBENCY_STS"));
        assertEquals("试用", IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "A店", "PROBATION", "导购", "138"), 1L).getStr("INCUMBENCY_STS"));
    }

    @Test
    public void blankIhrMobileNeverClearsErpPhone() {
        // IHR 手机号为空 → 不发 HANDSET，ERP 已有手机号保持不变（partial_update 保护）
        JSONObject p = IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "A店", "IN_SERVICE", "导购", " "), 1L);
        assertNull(p.getStr("HANDSET"));
    }

    @Test
    public void leavingParamsUseIdProtocol() {
        IhrEmployeeDetail d = detail("20250704286", "陈士华", "A店", "LEAVED", "导购", "138");
        d.setLeaveDate("2026-10-20");
        JSONObject p = IhrToBjErpConverter.toLeavingParams(d, 6603L);
        assertEquals(6603L, p.getLong("id"));
        assertEquals(Boolean.TRUE, p.getBool("partial_update"));
        assertEquals("离职", p.getStr("INCUMBENCY_STS"));
        assertEquals(20261020, p.getInt("END_DATE"));
        assertNull(p.getStr("ak"));
    }

    @Test
    public void createParamsUnchangedNoLocatorShift() {
        // 入职协议未变：仍以 NO=工号 创建，不带 id/partial_update
        JSONObject p = IhrToBjErpConverter.toCreateParams(
                detail("20250704286", "陈士华", "济南银座振兴街NL", "IN_SERVICE", "导购", "15064059740"));
        assertEquals("20250704286", p.getStr("NO"));
        assertNull(p.getStr("id"));
        assertNull(p.getBool("partial_update"));
        assertEquals("Y", p.getStr("IS_NEED_SYNC_CRM"));
    }
}
