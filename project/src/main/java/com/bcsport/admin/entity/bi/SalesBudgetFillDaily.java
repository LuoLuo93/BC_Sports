package com.bcsport.admin.entity.bi;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 销售预算(每日)填报（对应 BI_DW.ODS_SALES_BUDGET_FILL_DAILY）
 * 走 bidw 数据源(BI_DW schema)，无审计字段，不继承 BaseEntity
 */
@Data
@TableName("ODS_SALES_BUDGET_FILL_DAILY")
public class SalesBudgetFillDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 一级地区 */
    private String regionLevel1;

    /** 二级地区 */
    private String regionLevel2;

    /** 渠道类型 */
    private String channelProperty;

    /** 渠道定义 */
    private String channelDef;

    /** 店铺名称 */
    private String storeName;

    /** 品牌名称 */
    private String brandName;

    /** 月份 */
    private String monthlyName;

    /** 预算时间，YYYY-MM-DD */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date budgetDtm;

    /** 预算金额 */
    private BigDecimal budgetAmount;

    /** 渠道性质 */
    private String businessType;

    /** 经营类型 */
    private String businessProperty;

    /** 销售类型 */
    private String salesType;
}
