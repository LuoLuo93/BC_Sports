package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.DwdStockFlowQueryDTO;
import com.bcsport.admin.dto.DwdStockQueryDTO;
import com.bcsport.admin.entity.bi.DwStoreOption;
import com.bcsport.admin.entity.bi.DwdStockDaily;
import com.bcsport.admin.entity.bi.DwdStockFlowDetail;

import java.util.List;

/**
 * 数仓库存查询 Service（BI_DW.DWD_STOCK_DAILY）
 */
public interface DwdStockDailyService {

    /**
     * 分页查询每日库存（billdate 必填；可选店仓/货号/品名；批次明细或商品汇总粒度）
     */
    PageResult<DwdStockDaily> page(PageQuery pageQuery, DwdStockQueryDTO queryDTO);

    /**
     * 店仓下拉选项（主客户全部店仓）
     */
    List<DwStoreOption> stores();

    /**
     * 明细追溯：某组合某业务日的库存构成（锚点月结期末 + 构成流水）
     */
    DwdStockFlowDetail flowDetail(DwdStockFlowQueryDTO queryDTO);
}
