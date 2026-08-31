package com.bcsport.admin.controller.sticker;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.sticker.StickerDataQueryDTO;
import com.bcsport.admin.entity.sticker.StickerDataImportLog;
import com.bcsport.admin.mapper.sticker.StickerDataImportLogMapper;
import com.bcsport.admin.service.sticker.StickerDataImportService;
import com.bcsport.admin.service.sticker.StickerPrintService;
import com.bcsport.admin.util.ShiroSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sticker/data")
public class StickerDataController {

    @Autowired
    private StickerPrintService stickerPrintService;

    @Autowired
    private StickerDataImportService stickerDataImportService;

    @Autowired
    private StickerDataImportLogMapper stickerDataImportLogMapper;

    @GetMapping("/page")
    @RequiresPermissions("sticker:data:query")
    public Result<PageResult<Map<String, Object>>> page(@Valid PageQuery pageQuery, StickerDataQueryDTO queryDTO) {
        // 数据量较大，必须至少传入一个查询条件才查询，无条件直接返回空（前端已默认不查询，此处为兜底）
        if (isAllEmpty(queryDTO.getMaterialNumber(), queryDTO.getKindId(),
                queryDTO.getMaterialName(), queryDTO.getBrandId())) {
            return Result.success(PageResult.empty(pageQuery));
        }
        PageResult<Map<String, Object>> result = stickerPrintService.searchProducts(pageQuery, queryDTO.getMaterialNumber(), queryDTO.getKindId(), queryDTO.getMaterialName(), queryDTO.getBrandId());
        return Result.success(result);
    }

    private boolean isAllEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return false;
        }
        return true;
    }

    @GetMapping("/brands")
    @RequiresPermissions("sticker:data:query")
    public Result<?> brands() {
        return Result.success(stickerPrintService.getBrands());
    }

    /**
     * 类别下拉（从 ERP M_DIM DIM4 查询），供搜索栏“类别”筛选使用。
     */
    @GetMapping("/kinds")
    @RequiresPermissions("sticker:data:query")
    public Result<?> kinds() {
        return Result.success(stickerPrintService.getKinds());
    }

    /**
     * 按货号查询货品详情（贴纸资料详情页用），含矫正尺码组名回填。
     */
    @GetMapping("/{materialNumber}")
    @RequiresPermissions("sticker:data:query")
    public Result<Map<String, Object>> detail(@PathVariable String materialNumber) {
        return Result.success(stickerPrintService.getProductByMaterialNumber(materialNumber));
    }

    /**
     * 保存货品可编辑字段（执行标准/EAN13/面料编码/面料成分/辅料编码/辅料成分/矫正尺码组ID/安全类别），写回 ERP M_PRODUCT。
     * 基本信息（货号/品名/品牌/价格等）不更新，避免侵入 ERP 主数据。
     * 矫正尺码组ID 复用 M_PRODUCT.BOX_QTY_NEW 列存储。
     * body: { materialNumber, executionStandard, ean13, fabCode, fabElement, acCode, accElement, sizeGroupId, safetyCategory }
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
        String sizeGroupId = body.get("sizeGroupId") == null ? null : body.get("sizeGroupId").toString();
        String safetyCategory = body.get("safetyCategory") == null ? null : body.get("safetyCategory").toString();

        stickerPrintService.updateEditableFields(materialNumber, executionStandard, ean13,
                fabCode, fabElement, acCode, accElement, sizeGroupId, safetyCategory);
        return Result.success("保存成功");
    }

    /**
     * Excel 批量导入：按货号更新执行标准/EAN13/安全类别/4个材质字段，写回 ERP M_PRODUCT。
     * Excel 留空的字段不更新（保留库内原值），不写 NULL 清空。
     */
    @PostMapping("/import")
    @RequiresPermissions("sticker:data:import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.paramError("请上传Excel文件");
        }
        if (file.getSize() > 100 * 1024 * 1024) {
            return Result.paramError("文件大小不能超过100MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            return Result.paramError("仅支持.xlsx或.xls格式的Excel文件");
        }
        try {
            Map<String, Object> result = stickerDataImportService.importFromExcel(file);
            return Result.success(result);
        } catch (cn.hutool.poi.exceptions.POIException | org.apache.poi.ooxml.POIXMLException
                 | org.apache.poi.util.RecordFormatException e) {
            log.error("贴纸资料 Excel解析失败: {}", e.getMessage());
            String errorMsg = "Excel解析失败，请确认文件是标准的 .xlsx/.xls 格式: " + e.getMessage();
            saveFailedLog(file, errorMsg);
            return Result.error(errorMsg);
        } catch (Exception e) {
            log.error("贴纸资料导入失败: {}", e.getMessage(), e);
            String errorMsg = "导入失败：" + e.getMessage();
            saveFailedLog(file, errorMsg);
            return Result.error(errorMsg);
        }
    }

    private void saveFailedLog(MultipartFile file, String errorMsg) {
        try {
            StickerDataImportLog logEntity = new StickerDataImportLog();
            logEntity.setFileName(file.getOriginalFilename());
            logEntity.setFileSize(file.getSize());
            logEntity.setTotalCount(0);
            logEntity.setSuccessCount(0);
            logEntity.setFailCount(0);
            logEntity.setStatus("FAILED");
            logEntity.setErrorMsg(errorMsg.length() > 4000 ? errorMsg.substring(0, 4000) : errorMsg);
            logEntity.setCreateBy(ShiroSecurityUtils.getCurrentUsername());
            logEntity.setCreateTime(LocalDateTime.now());
            stickerDataImportLogMapper.insert(logEntity);
        } catch (Exception ex) {
            log.warn("保存贴纸资料失败导入日志失败: {}", ex.getMessage());
        }
    }

    /**
     * 下载导入模板：货号必填，其余列留空=不更新该字段。
     */
    @GetMapping("/template")
    @RequiresPermissions("sticker:data:import")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("贴纸资料导入模板.xlsx", StandardCharsets.UTF_8.name()));

        ExcelWriter writer = ExcelUtil.getWriter(true);
        try {
            writer.addHeaderAlias("货号", "货号");
            writer.addHeaderAlias("执行标准", "执行标准");
            writer.addHeaderAlias("EAN13", "EAN13");
            writer.addHeaderAlias("安全类别", "安全类别");
            writer.addHeaderAlias("面料成分1", "面料成分1");
            writer.addHeaderAlias("面料成分2", "面料成分2");
            writer.addHeaderAlias("辅料成分1", "辅料成分1");
            writer.addHeaderAlias("辅料成分2", "辅料成分2");

            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("货号", "NLM25001");
            sample.put("执行标准", "GB/T 22853-2019");
            sample.put("EAN13", "123456789012");
            sample.put("安全类别", "GB 31701-2015 B类");
            sample.put("面料成分1", "100%聚酯纤维");
            sample.put("面料成分2", "");
            sample.put("辅料成分1", "100%氨纶");
            sample.put("辅料成分2", "");
            writer.write(Collections.singletonList(sample), true);
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }

    /**
     * 导入日志分页查询。
     */
    @GetMapping("/import-log/page")
    @RequiresPermissions("sticker:data:query")
    public Result<PageResult<StickerDataImportLog>> importLogPage(PageQuery pageQuery) {
        return Result.success(stickerDataImportService.logPage(pageQuery));
    }
}
