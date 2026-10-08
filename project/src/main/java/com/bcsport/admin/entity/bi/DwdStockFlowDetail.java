package com.bcsport.admin.entity.bi;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 库存明细追溯结果:某组合在某业务日的库存构成分解
 * 口径与 dw_rp_stock_daily 正推一致:库存 = 月结期末 + (月结末日, 业务日] 的流水累加
 */
@Data
public class DwdStockFlowDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 锚点月结期间 YYYYMM(最近已月结月) */
    private Integer yearmonth;

    /** 锚点期间ID(monthstore 定位用) */
    private Long cPeriodId;

    /** 锚点月结末日 YYYYMMDD */
    private Integer periodDateend;

    /** 月结期末数量(组合在月结明细无行时为 0,如新店/新货) */
    private BigDecimal monthQty;

    /** 构成流水合计(月结末日 < changedate <= 业务日, qtychange<>0) */
    private BigDecimal flowSum;

    /** 推算库存 = monthQty + flowSum(与列表行的 total 对账) */
    private BigDecimal stockQty;

    /** 构成流水明细(按业务日升序) */
    private List<FlowItem> flows;

    /** 单条流水 */
    @Data
    public static class FlowItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 业务日 YYYYMMDD */
        private Integer changedate;

        /** 单据号 */
        private String docno;

        /** 单据类型(入/出库分类) */
        private String doctype;

        /** 业务类型 */
        private String billtype;

        /** 库存变动数量(+入/-出) */
        private BigDecimal qtychange;
    }
}
