package com.bcsport.admin.vo;

import lombok.Data;

/**
 * 伯俊ERP员工现值（HR_EMPLOYEE JOIN C_STORE 只读快照）
 * 供人员变动同步做最小化字段比对：REST v3 协议下 ObjectModify
 * 以 id 定位 + partial_update=true 只更新传入字段，
 * 未发生变化的字段不传（也避免 IHR部门名与ERP店名漂移时卡店仓校验）。
 */
@Data
public class ErpEmployeeCurrent {

    /** HR_EMPLOYEE.ID，REST v3 ObjectModify 的 id 定位参数 */
    private Long id;

    /** 姓名（NO 工号定位改 id 后，姓名仅作比对字段） */
    private String name;

    /** 店仓名（C_STORE.NAME，当前值） */
    private String storeName;

    /** 在职状况 ERP 码：I=在职 D=离职 E=极少见(保守不动) */
    private String incumbencySts;

    /** 手机号 */
    private String handset;

    /** 是否营业员 Y/N */
    private String issaler;

    /** 是否店长 Y/N/空 */
    private String isstoreman;
}
