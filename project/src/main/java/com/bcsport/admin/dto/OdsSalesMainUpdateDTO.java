package com.bcsport.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 数仓销售查看(ODS_SALES_MAIN)编辑入参
 * 行唯一身份 = BILL_ID + ITEM_ID(单据号+货号+尺码不唯一，不能作行定位)
 * 可编辑归属维度字段 + 数量/金额字段
 * 注意：SP_FILL_ODS_SALES_MAIN 按日期范围重灌会覆盖人工修改
 */
@Data
public class OdsSalesMainUpdateDTO {

    /** 单据ID(行定位键) */
    @NotNull(message = "单据ID不能为空")
    private Long billId;

    /** 单据明细ID(行定位键) */
    @NotNull(message = "明细ID不能为空")
    private Long itemId;

    /** 销售类型 */
    private String salesType;

    /** 店铺CODE */
    private String storeCode;

    /** 店铺名称 */
    private String storeName;

    /** 营业员CODE */
    private String billPosCode;

    /** 营业员名称 */
    private String billPosName;

    /** CRM会员卡号 */
    private String vipCode;

    /** CRM会员手机号 */
    private String vipMobile;

    /** 网单来源单号 */
    private String omsSourcecode;

    /** 主播ID */
    private String anchorSummaryid;

    /** 主播名称 */
    private String anchorSummaryname;

    /** 数量 */
    private BigDecimal qty;

    /** 零售价 */
    private BigDecimal retailPrice;

    /** 零售金额 */
    private BigDecimal retailAmount;

    /** 成交金额 */
    private BigDecimal transactionAmount;

    /** 业绩金额 */
    private BigDecimal revenue;

    /** 重算业绩 */
    private BigDecimal recalcRevenue;
}
