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
        // 无条件时默认查第一页（分页保护，避免全表一次性加载）
        PageResult<Map<String, Object>> result = stickerPrintService.searchProducts(pageQuery, queryDTO.getMaterialNumber(), queryDTO.getStyleNumber(), queryDTO.getMaterialName(), queryDTO.getBrandId());
        return Result.success(result);
    }

    @GetMapping("/brands")
    @RequiresPermissions("sticker:data:query")
    public Result<?> brands() {
        return Result.success(stickerPrintService.getBrands());
    }

    /**
     * 保存货品可编辑字段（执行标准/EAN13/面料编码/面料成分/辅料编码/辅料成分），写回 ERP M_PRODUCT。
     * 基本信息（货号/品名/品牌/价格等）不更新，避免侵入 ERP 主数据。
     * body: { materialNumber, executionStandard, ean13, fabCode, fabElement, acCode, accElement }
     */
    @PutMapping("/material")
    @RequiresPermissions("sticker:data:edit")
    public Result<?> updateMaterial(@RequestBody Map<String, Object> body) {
        String materialNumber = body.get("materialNumber") == null ? null : body.get("materialNumber").toString();
        String executionStandard = body.get("executionStandard") == null ? null : body.get("executionStandard").toString();
        String ean13 = body.get("ean13") == null ? null : body.get("ean13").toString();
        String fabCode = body.get("fabCode") == null ? null : body.get("fabCode").toString();
        String fabElement = body.get("fabElement") == null ? null : body.get("fabElement").toString();
        String acCode = body.get("acCode") == null ? null : body.get("acCode").toString();
        String accElement = body.get("accElement") == null ? null : body.get("accElement").toString();

        stickerPrintService.updateEditableFields(materialNumber, executionStandard, ean13,
                fabCode, fabElement, acCode, accElement);
        return Result.success("保存成功");
    }
}
