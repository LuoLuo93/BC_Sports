package com.bcsport.admin.controller.sticker;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.service.sticker.StickerPrintService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/sticker/data")
public class StickerDataController {

    @Autowired
    private StickerPrintService stickerPrintService;

    @GetMapping("/page")
    @RequiresPermissions("sticker:data:query")
    public Result<PageResult<Map<String, Object>>> page(
            @Valid PageQuery pageQuery,
            @RequestParam(required = false) String materialNumber,
            @RequestParam(required = false) String styleNumber,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String brandId) {
        boolean noFilter = (materialNumber == null || materialNumber.trim().isEmpty())
                && (styleNumber == null || styleNumber.trim().isEmpty())
                && (materialName == null || materialName.trim().isEmpty())
                && (brandId == null || brandId.trim().isEmpty());
        if (noFilter) {
            return Result.paramError("请至少输入一个搜索条件");
        }
        PageResult<Map<String, Object>> result = stickerPrintService.searchProducts(pageQuery, materialNumber, styleNumber, materialName, brandId);
        return Result.success(result);
    }

    @GetMapping("/brands")
    @RequiresPermissions("sticker:data:query")
    public Result<?> brands() {
        return Result.success(stickerPrintService.getBrands());
    }
}
