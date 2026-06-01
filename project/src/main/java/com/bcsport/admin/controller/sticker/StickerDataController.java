package com.bcsport.admin.controller.sticker;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.sticker.StickerDataQueryDTO;
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
    public Result<PageResult<Map<String, Object>>> page(@Valid PageQuery pageQuery, StickerDataQueryDTO queryDTO) {
        boolean noFilter = (queryDTO.getMaterialNumber() == null || queryDTO.getMaterialNumber().trim().isEmpty())
                && (queryDTO.getStyleNumber() == null || queryDTO.getStyleNumber().trim().isEmpty())
                && (queryDTO.getMaterialName() == null || queryDTO.getMaterialName().trim().isEmpty())
                && (queryDTO.getBrandId() == null || queryDTO.getBrandId().trim().isEmpty());
        if (noFilter) {
            return Result.paramError("请至少输入一个搜索条件");
        }
        PageResult<Map<String, Object>> result = stickerPrintService.searchProducts(pageQuery, queryDTO.getMaterialNumber(), queryDTO.getStyleNumber(), queryDTO.getMaterialName(), queryDTO.getBrandId());
        return Result.success(result);
    }

    @GetMapping("/brands")
    @RequiresPermissions("sticker:data:query")
    public Result<?> brands() {
        return Result.success(stickerPrintService.getBrands());
    }
}
