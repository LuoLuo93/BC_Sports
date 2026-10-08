package com.bcsport.admin.entity.bi;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 每日库存查询结果（BI_DW.DWD_STOCK_DAILY 关联店仓/商品维表）
 * 走 bidw 数据源(BI_DW schema)，纯查询页面，无主键无审计字段，不继承 BaseEntity
 * 字段映射由 DwdStockDailyMapper.xml 显式别名完成
 */
@Data
public class DwdStockDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务日 YYYYMMDD */
    private Integer billdate;

    /** 店仓ID */
    private Long storeId;

    /** 店仓编码 */
    private String storeCode;

    /** 店仓名称 */
    private String storeName;

    /** 商品ID */
    private Long productId;

    /** 货号(m_product.name，色码级SKU号) */
    private String productCode;

    /** 品名(m_product.goodsname) */
    private String productName;

    /** 品牌(m_product.card_name) */
    private String cardName;

    /** 季节(m_product.season_name) */
    private String seasonName;

    /** 性别(m_product.sex_name) */
    private String sexName;

    /** 年份(m_product.year_no) */
    private String yearNo;

    /** 批次/色码属性实例ID(商品汇总模式下不返回) */
    private Long attrInstanceId;

    /** 颜色(属性实例 VALUE1_CODE,明细模式返回) */
    private String colorName;

    /** 尺码(属性实例 VALUE2_CODE,明细模式返回) */
    private String sizeName;

    /** 吊牌价 */
    private BigDecimal pricelist;

    /** 库存数量 */
    private BigDecimal total;

    /** 吊牌价金额 */
    private BigDecimal amtList;
}
