package com.bcsport.admin.controller;

import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.OdsSalesMainQueryDTO;
import com.bcsport.admin.dto.OdsSalesMainUpdateDTO;
import com.bcsport.admin.entity.SysImportLog;
import com.bcsport.admin.entity.bi.OdsSalesMain;
import com.bcsport.admin.service.OdsSalesMainService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数仓销售查看（BI_DW.ODS_SALES_MAIN 查询 + 编辑）
 * Excel 期初导入功能已按用户决定删除（2026-09-09）；导入日志保留历史查询（统一日志表）。
 */
@Slf4j
@RestController
@RequestMapping("/api/bi/dw-sales")
@Api(tags = "数仓销售查看")
public class OdsSalesMainController {

    @Autowired
    private OdsSalesMainService odsSalesMainService;

    /**
     * 分页查询销售主明细
     */
    @GetMapping("/page")
    @ApiOperation("分页查询销售主明细")
    @RequiresPermissions("bi:dw-sales:query")
    public Result<PageResult<OdsSalesMain>> page(PageQuery pageQuery, OdsSalesMainQueryDTO queryDTO) {
        return Result.success(odsSalesMainService.page(pageQuery, queryDTO));
    }

    /**
     * 编辑归属维度字段
     */
    @PostMapping("/update")
    @ApiOperation("编辑销售明细归属维度字段")
    @RequiresPermissions("bi:dw-sales:edit")
    // 事实表人工改数必须留痕：@OperLog 把 OdsSalesMainUpdateDTO(billId/itemId/改动字段)序列化进 sys_log
    @OperLog(module = "数仓销售", operation = "编辑销售明细")
    public Result<?> update(@Valid @RequestBody OdsSalesMainUpdateDTO dto) {
        boolean success = odsSalesMainService.update(dto);
        return success ? Result.success("修改成功") : Result.error("记录不存在(可能已被ETL重灌)，请刷新后重试");
    }

    /**
     * 导入日志分页查询（历史记录）
     */
    @GetMapping("/import-log/page")
    @ApiOperation("导入日志分页查询")
    @RequiresPermissions("bi:dw-sales:query")
    public Result<PageResult<SysImportLog>> importLogPage(PageQuery pageQuery) {
        return Result.success(odsSalesMainService.logPage(pageQuery));
    }
}
