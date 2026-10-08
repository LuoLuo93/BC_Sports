package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 数仓库存查询(DWD_STOCK_DAILY)查询条件
 */
@Data
public class DwdStockQueryDTO {

    /** 业务日 YYYYMMDD(必填，单日查询；口径=正推 ad_pi_id IS NULL) */
    private Integer billdate;

    /** 店仓ID(可选，精确匹配) */
    private Long storeId;

    /** 货号模糊匹配(m_product.name，色码级SKU号) */
    private String productCode;

    /** 品名模糊匹配(m_product.goodsname) */
    private String productName;

    /** true=聚合到 店仓×商品 粒度(合并批次)；默认空/false=批次明细粒度 */
    private Boolean groupByProduct;
}
