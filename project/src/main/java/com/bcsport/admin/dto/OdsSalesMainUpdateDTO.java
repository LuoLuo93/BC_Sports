package com.bcsport.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 数仓销售查看(ODS_SALES_MAIN)编辑入参
 * 行唯一身份 = BILL_ID + ITEM_ID(单据号+货号+尺码不唯一，不能作行定位)
 * 仅允许编辑：店铺、营业员、数量/金额；货品/单据/会员/主播等只读
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

    /** 店铺CODE */
    private String storeCode;

    /** 店铺名称 */
    private String storeName;

    /** 营业员CODE */
    private String billPosCode;

    /** 营业员名称 */
    private String billPosName;

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
