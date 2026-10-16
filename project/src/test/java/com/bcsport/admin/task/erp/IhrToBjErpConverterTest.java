package com.bcsport.admin.task.erp;

import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;
import com.bcsport.admin.vo.ErpEmployeeCurrent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REST v3 变更协议转换器单测：
 * id 定位 + partial_update=true 最小化发送，重点防护
 * 「partial_update 漏传导致未传字段被 ERP 清空」与「店名漂移误发店仓字段」两类事故。
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

    private static ErpEmployeeCurrent current(Long id, String name, String store,
                                              String sts, String handset, String issaler, String isstoreman) {
        ErpEmployeeCurrent c = new ErpEmployeeCurrent();
        c.setId(id);
        c.setName(name);
        c.setStoreName(store);
        c.setIncumbencySts(sts);
        c.setHandset(handset);
        c.setIssaler(issaler);
        c.setIsstoreman(isstoreman);
        return c;
    }

    @Test
    public void modifyAlwaysCarriesIdAndPartialUpdateTrue() {
        JSONObject p = IhrToBjErpConverter.toModifyParams(
                detail("20250704286", "陈士华", "济南银座振兴街NL", "IN_SERVICE", "导购", "15064059740"),
                6603L,
                current(6603L, "陈士华", "济南银座振兴街NL", "I", "15064059740", "Y", ""),
                false);
        assertEquals(14630, p.getInt("table"));
        assertEquals(6603L, p.getLong("id"));
        // 保命栓：partial_update 必须 true，否则 ERP 会把未传字段清空
        assertEquals(Boolean.TRUE, p.getBool("partial_update"));
    }

    @Test
    public void noDiffSendsOnlyLocator() {
        JSONObject p = IhrToBjErpConverter.toModifyParams(
                detail("20250704286", "陈士华", "济南银座振兴街NL", "IN_SERVICE", "导购", "15064059740"),
                6603L,
                current(6603L, "陈士华", "济南银座振兴街NL", "I", "15064059740", "Y", ""),
                false);
        assertEquals(3, p.size(), "无差异时只应含 table/id/partial_update 三个定位键");
    }

    @Test
    public void nameDiffSendsNameOnly() {
        JSONObject p = IhrToBjErpConverter.toModifyParams(
                detail("1", "陈士华", "A店", "IN_SERVICE", "导购", "13800000000"),
                1L, current(1L, "陈士桦", "A店", "I", "13800000000", "Y", ""), false);
        assertEquals("陈士华", p.getStr("NAME"));
        assertNull(p.getStr("HANDSET"));
        assertNull(p.getStr("C_STORE_ID__NAME"));
    }

    @Test
    public void storeNotChangedNeverSent() {
        // IHR 部门名与 ERP 店名一致 → 绝不传店仓（漂移场景被隔离在店仓字段之外）
        JSONObject p = IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "济南银座振兴街NL", "IN_SERVICE", "导购", "138"),
                1L, current(1L, "张三", " 济南银座振兴街NL ", "I", "139", "Y", ""), false);
        assertNull(p.getStr("C_STORE_ID__NAME"));
        // 手机号差异不受店名影响，照常发送
        assertEquals("138", p.getStr("HANDSET"));
    }

    @Test
    public void storeChangedSendsIhrDept() {
        JSONObject p = IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "新店NL", "IN_SERVICE", "导购", "138"),
                1L, current(1L, "张三", "旧店NL", "I", "138", "Y", ""), true);
        assertEquals("新店NL", p.getStr("C_STORE_ID__NAME"));
    }

    @Test
    public void blankIhrMobileNeverClearsErpPhone() {
        // IHR 手机号为空 → 不发 HANDSET，ERP 已有手机号保持不变（与旧口径一致）
        JSONObject p = IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "A店", "IN_SERVICE", "导购", " "),
                1L, current(1L, "张三", "A店", "I", "13900000000", "Y", ""), false);
        assertNull(p.getStr("HANDSET"));
    }

    @Test
    public void statusOnlyPulledBackWhenErpLeft() {
        // ERP=I 且 IHR 在职 → 不发状态
        JSONObject ok = IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "A店", "IN_SERVICE", "导购", "138"),
                1L, current(1L, "张三", "A店", "I", "138", "Y", ""), false);
        assertNull(ok.getStr("INCUMBENCY_STS"));

        // ERP=D(离职) 而 IHR 在职 → 拉回在职
        JSONObject back = IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "A店", "IN_SERVICE", "导购", "138"),
                1L, current(1L, "张三", "A店", "D", "138", "Y", ""), false);
        assertEquals("在职", back.getStr("INCUMBENCY_STS"));

        // ERP=E(罕见码) 保守不动
        JSONObject rare = IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "A店", "IN_SERVICE", "导购", "138"),
                1L, current(1L, "张三", "A店", "E", "138", "Y", ""), false);
        assertNull(rare.getStr("INCUMBENCY_STS"));
    }

    @Test
    public void storemanFlagSyncedOnlyOnDiff() {
        // 升店长：现值空 → 发Y
        JSONObject up = IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "A店", "IN_SERVICE", "店长", "138"),
                1L, current(1L, "张三", "A店", "I", "138", "Y", ""), false);
        assertEquals("Y", up.getStr("ISSTOREMAN"));

        // 卸店长：现值Y → 发空串
        JSONObject down = IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "A店", "IN_SERVICE", "导购", "138"),
                1L, current(1L, "张三", "A店", "I", "138", "Y", "Y"), false);
        assertEquals("", down.getStr("ISSTOREMAN"));

        // 一致 → 不发
        JSONObject same = IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "A店", "IN_SERVICE", "导购", "138"),
                1L, current(1L, "张三", "A店", "I", "138", "Y", ""), false);
        assertNull(same.getStr("ISSTOREMAN"));
    }

    @Test
    public void salerFlagAlignedWhenNotY() {
        JSONObject p = IhrToBjErpConverter.toModifyParams(
                detail("1", "张三", "A店", "IN_SERVICE", "导购", "138"),
                1L, current(1L, "张三", "A店", "I", "138", "N", ""), false);
        assertEquals("Y", p.getStr("ISSALER"));
    }

    @Test
    public void trailingSpacesTreatedAsEqual() {
        // 历史数据尾随空格不算差异，避免无意义的写请求
        JSONObject p = IhrToBjErpConverter.toModifyParams(
                detail("1", " 张三 ", "A店", "IN_SERVICE", "导购", " 138 "),
                1L, current(1L, "张三", "A店", "I", "138", "Y", ""), false);
        assertEquals(3, p.size());
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
    }
}
