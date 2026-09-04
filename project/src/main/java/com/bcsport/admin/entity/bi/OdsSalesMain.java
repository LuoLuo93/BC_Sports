package com.bcsport.admin.entity.bi;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 销售主明细 ODS 合并表（对应 BI_DW.ODS_SALES_MAIN）
 * 走 bidw 数据源(BI_DW schema)，纯查询页面，无主键无审计字段，不继承 BaseEntity
 * 字段映射由 OdsSalesMainMapper.xml 显式别名完成
 */
@Data
public class OdsSalesMain implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 单据ID */
    private Long billId;

    /** 单据号 */
    private String billNo;

    /** 单据日期(YYYYMMDD) */
    private Long billDate;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date billTime;

    /** CRM会员卡号 */
    private String vipCode;

    /** CRM会员手机号 */
    private String vipMobile;

    /** 网单来源单号 */
    private String omsSourcecode;

    /** 销售类型 */
    private String salesType;

    /** 单据明细ID(零售明细ID/销售明细ID) */
    private Long itemId;

    /** 营业员ID */
    private Long billPosId;

    /** 营业员CODE */
    private String billPosCode;

    /** 营业员NAME */
    private String billPosName;

    /** 店铺ID */
    private Long storeId;

    /** 店铺CODE */
    private String storeCode;

    /** 店铺NAME */
    private String storeName;

    /** 货品货号 */
    private String productCode;

    /** 货品款号 */
    private String productStyleNo;

    /** 货品名称 */
    private String productName;

    /** 货品颜色 */
    private String colorsalias;

    /** 货品条码 */
    private String barcode;

    /** 货品尺码 */
    private String sizes;

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

    /** 主播ID */
    private String anchorSummaryid;

    /** 主播名称 */
    private String anchorSummaryname;
}
