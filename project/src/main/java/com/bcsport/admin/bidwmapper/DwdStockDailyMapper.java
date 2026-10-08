package com.bcsport.admin.bidwmapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.dto.DwdStockFlowQueryDTO;
import com.bcsport.admin.dto.DwdStockQueryDTO;
import com.bcsport.admin.entity.bi.DwStoreOption;
import com.bcsport.admin.entity.bi.DwdStockDaily;
import com.bcsport.admin.entity.bi.DwdStockFlowDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数仓库存查询 Mapper（走 bidw 数据源，BI_DW schema）
 * 放在 bidwmapper 包下，由 BidwDataSourceConfig 自动绑定 bidw 数据源
 * 纯查询页面，不继承 BaseMapper
 */
@Mapper
public interface DwdStockDailyMapper {

    /**
     * 分页查询每日库存（billdate 必填；可选店仓/货号/品名；两种粒度：批次明细/商品汇总）
     */
    Page<DwdStockDaily> selectPage(Page<DwdStockDaily> page, @Param("q") DwdStockQueryDTO query);

    /**
     * 店仓下拉选项（主客户 c_customer_id=1 全部店仓）
     */
    List<DwStoreOption> selectStores();

    /**
     * 明细追溯-定位锚点月结期间（与存储过程 last_period 同口径：
     * 主客户1、双Y、dateend < 查询日所在月1号，取最近一个）
     */
    DwdStockFlowDetail selectAnchorPeriod(@Param("q") DwdStockFlowQueryDTO q);

    /**
     * 明细追溯-锚点月结期末数量（monthstore 定位该组合的 qtyend，无行=0）
     */
    BigDecimal selectMonthQty(@Param("q") DwdStockFlowQueryDTO q, @Param("cPeriodId") Long cPeriodId,
                              @Param("yearmonth") Integer yearmonth);

    /**
     * 明细追溯-构成流水明细（月结末日 < changedate <= 业务日，qtychange<>0，按业务日升序）
     */
    List<DwdStockFlowDetail.FlowItem> selectFlowItems(@Param("q") DwdStockFlowQueryDTO q,
                                                      @Param("periodDateend") Integer periodDateend);
}
