package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 库存明细追溯(月结期末 + 构成流水)查询条件
 * 定位一个 店仓×商品×属性实例 组合在某业务日的库存构成
 */
@Data
public class DwdStockFlowQueryDTO {

    /** 业务日 YYYYMMDD(必填,须 >= 20260801:7月末为倒推补录无月结流水可追溯) */
    private Integer billdate;

    /** 店仓ID(必填) */
    private Long storeId;

    /** 商品ID(必填) */
    private Long productId;

    /** 属性实例ID(可空:无批次属性的商品) */
    private Long attrInstanceId;
}
