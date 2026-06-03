package com.bcsport.admin.controller.sticker;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.sticker.StickerDataQueryDTO;
import com.bcsport.admin.dto.sticker.StickerPrintQueryDTO;
import com.bcsport.admin.entity.sticker.StickerPrintOrder;
import com.bcsport.admin.service.sticker.StickerPrintService;
import com.bcsport.admin.vo.StickerPrintOrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/api/sticker/print")
@Api(tags = "贴纸打印")
public class StickerPrintController {

    @Autowired
    private StickerPrintService stickerPrintService;

    @GetMapping("/page")
    @ApiOperation("分页查询打印单")
    @RequiresPermissions("sticker:print:query")
    public Result<PageResult<StickerPrintOrderVO>> page(@Valid PageQuery pageQuery, StickerPrintQueryDTO queryDTO) {
        return Result.success(stickerPrintService.pageOrders(pageQuery, queryDTO));
    }

    @GetMapping("/{orderId}")
    @ApiOperation("查询打印单详情")
    @RequiresPermissions("sticker:print:query")
    public Result<StickerPrintOrderVO> getDetail(@PathVariable String orderId) {
        return Result.success(stickerPrintService.getOrderDetailVO(orderId));
    }

    @PostMapping
    @ApiOperation("创建打印单")
    @RequiresPermissions("sticker:print:add")
    public Result<?> create(@RequestBody StickerPrintOrder order) {
        String id = stickerPrintService.createOrder(order);
        return Result.success(id);
    }

    @PutMapping("/{orderId}")
    @ApiOperation("更新打印单")
    @RequiresPermissions("sticker:print:edit")
    public Result<?> update(@PathVariable String orderId, @RequestBody StickerPrintOrder order) {
        stickerPrintService.updateOrder(orderId, order);
        return Result.success("更新成功");
    }

    @PostMapping("/{orderId}/submit")
    @ApiOperation("提交打印单")
    @RequiresPermissions("sticker:print:edit")
    public Result<?> submit(@PathVariable String orderId) {
        stickerPrintService.submitOrder(orderId);
        return Result.success("提交成功");
    }

    @PostMapping("/{orderId}/review")
    @ApiOperation("审核打印单")
    @RequiresPermissions("sticker:print:review")
    public Result<?> review(@PathVariable String orderId, @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            return Result.paramError("审核状态不能为空");
        }
        int status;
        try {
            status = Integer.parseInt(statusStr);
        } catch (NumberFormatException e) {
            return Result.paramError("审核状态格式错误");
        }
        if (status != 2 && status != 3) {
            return Result.paramError("审核状态只能是 2(通过) 或 3(驳回)");
        }
        String reviewRemark = body.get("reviewRemark");
        stickerPrintService.reviewOrder(orderId, status, reviewRemark);
        return Result.success("审核完成");
    }

    @DeleteMapping("/{orderId}")
    @ApiOperation("删除打印单")
    @RequiresPermissions("sticker:print:delete")
    public Result<?> delete(@PathVariable String orderId) {
        stickerPrintService.deleteOrder(orderId);
        return Result.success("删除成功");
    }

    @GetMapping("/product/search")
    @ApiOperation("搜索货品")
    @RequiresPermissions("sticker:print:query")
    public Result<?> searchProducts(StickerDataQueryDTO queryDTO) {
        return Result.success(stickerPrintService.searchProducts(queryDTO.getMaterialNumber(), queryDTO.getStyleNumber(), queryDTO.getMaterialName(), queryDTO.getBrandId()));
    }

    @GetMapping("/product/brands")
    @ApiOperation("获取品牌列表")
    @RequiresPermissions("sticker:print:query")
    public Result<?> getBrands() {
        return Result.success(stickerPrintService.getBrands());
    }

    @PostMapping("/{orderId}/bartender-print")
    @ApiOperation("BarTender打印")
    @RequiresPermissions("sticker:print:execute")
    public Result<?> bartenderPrint(@PathVariable String orderId) {
        boolean ok = stickerPrintService.bartenderPrint(orderId);
        if (ok) {
            return Result.success("打印指令已发送");
        } else {
            return Result.error("打印失败，请检查BarTender服务");
        }
    }
}
