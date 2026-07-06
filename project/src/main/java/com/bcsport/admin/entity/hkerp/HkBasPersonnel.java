package com.bcsport.admin.entity.hkerp;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * ERP职员资料表（旧版HK ERP直写链路）
 * <p>
 * 对应源项目 interfaceForHK 的 BasPersonnel 实体，表 Bas_Personnel。
 * 由 HR 入职/离职/变更同步逻辑直接读写。
 */
@Data
@TableName("Bas_Personnel")
public class HkBasPersonnel implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 公司ID，固定 "BC" */
    private String companyId;
    /** 职员ID（主键，业务生成的10位编号） */
    private String personnelId;
    /** 职员代码（员工工号） */
    private String personnelCode;
    /** 职员姓名 */
    private String personnelName;
    /** 职员类型ID，固定 "00000000000011" */
    private String personnelTypeId;
    /** 部门ID */
    private String departmentId;
    /** 性别 */
    private String sex;
    /** 手机号 */
    private String mobilePhone;
    /** 是否业务员 */
    private String isEmployee;
    /** 是否采购员 */
    private String isBuyer;
    /** 是否捡货人 */
    private String isPicker;
    /** 是否验货员 */
    private String isSurveyor;
    /** 是否营业员 */
    private String isAssistant;
    /** 店铺ID */
    private String shopId;
    /** 允许使用（1启用 0禁用） */
    private String allowUsed;
    /** 审核状态（必填） */
    private String checkState;
    /** 锁定状态 */
    private String lockState;
    /** 同步状态 */
    private String syncState;
    /** 修改时间 */
    private String modifyDtm;
    /** 运动城ID */
    private String sportCityId;
    /** 是否SD收银员 */
    private String isSdCashier;
    /** 是否EM收银员 */
    private String isEmCashier;
    /** 是否EM人员 */
    private String isEmPerson;
    /** 是否仓管员 */
    private String isStocker;
    /** 仓库ID */
    private String stockId;
    /** 是否领料人 */
    private String isRecipients;
    /** 语言 */
    private String lan;
    /** 全店铺名称（部门名称） */
    private String allShopName;
    /** 在职状态（0在职 2离职） */
    private String personnelStatus;
}
