package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.DwdStockFlowQueryDTO;
import com.bcsport.admin.dto.DwdStockQueryDTO;
import com.bcsport.admin.entity.bi.DwStoreOption;
import com.bcsport.admin.entity.bi.DwdStockDaily;
import com.bcsport.admin.entity.bi.DwdStockFlowDetail;
import com.bcsport.admin.service.DwdStockDailyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数仓库存查询（BI_DW.DWD_STOCK_DAILY 只读查询）
 * 口径:月结期末+流水累加的正推结果(ad_pi_id IS NULL),T+1 更新到昨日;
 * 倒推补录(ad_pi_id=-1,仅2026-07-26~31)不进本页面。
 */
@Slf4j
@RestController
@RequestMapping("/api/bi/dw-stock")
@Api(tags = "数仓库存查询")
public class DwdStockDailyController {

    @Autowired
    private DwdStockDailyService dwdStockDailyService;

    /**
     * 分页查询每日库存
     */
    @GetMapping("/page")
    @ApiOperation("分页查询每日库存")
    @RequiresPermissions("bi:dw-stock:query")
    public Result<PageResult<DwdStockDaily>> page(PageQuery pageQuery, DwdStockQueryDTO queryDTO) {
        // 单日全量约33万行,日期是唯一强过滤条件(billdate等值走唯一索引前缀),必填防全表扫
        if (queryDTO.getBilldate() == null) {
            return Result.paramError("业务日期必填(单日查询)");
        }
        return Result.success(dwdStockDailyService.page(pageQuery, queryDTO));
    }

    /**
     * 店仓下拉选项
     */
    @GetMapping("/stores")
    @ApiOperation("店仓下拉选项(主客户全部店仓)")
    @RequiresPermissions("bi:dw-stock:query")
    public Result<List<DwStoreOption>> stores() {
        return Result.success(dwdStockDailyService.stores());
    }

    /**
     * 明细追溯：某店仓×货号×尺码组合在某业务日的库存构成
     * （锚点月结期末 + 月结末日~业务日的构成流水，与正推口径一致）
     */
    @GetMapping("/flow-detail")
    @ApiOperation("库存构成明细追溯(月结期末+构成流水)")
    @RequiresPermissions("bi:dw-stock:query")
    public Result<DwdStockFlowDetail> flowDetail(DwdStockFlowQueryDTO queryDTO) {
        if (queryDTO.getBilldate() == null || queryDTO.getStoreId() == null || queryDTO.getProductId() == null) {
            return Result.paramError("业务日期、店仓、商品必填");
        }
        // 7月末(20260726~31)为倒推补录数据:无月结锚点/流水不完整,构成追溯会误导,直接拒绝
        if (queryDTO.getBilldate() < 20260801) {
            return Result.paramError("该日期为历史倒推补录数据(7月末),不支持构成明细追溯");
        }
        DwdStockFlowDetail detail = dwdStockDailyService.flowDetail(queryDTO);
        if (detail == null) {
            return Result.error("未找到该日期前已月结的期间,无法追溯");
        }
        return Result.success(detail);
    }
}
