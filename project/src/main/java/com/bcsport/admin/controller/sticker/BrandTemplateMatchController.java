package com.bcsport.admin.controller.sticker;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.sticker.BrandTemplateMatch;
import com.bcsport.admin.erpmapper.BjerpProductMapper;
import com.bcsport.admin.service.sticker.BrandTemplateMatchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sticker/brand-template")
@Api(tags = "品牌模板关系管理")
@Slf4j
public class BrandTemplateMatchController {

    @Autowired
    private BrandTemplateMatchService service;

    @Autowired
    private BjerpProductMapper bjerpProductMapper;

    @GetMapping("/page")
    @ApiOperation("分页查询")
    @RequiresPermissions("sticker:brand-template:query")
    public Result<PageResult<BrandTemplateMatch>> page(@Valid PageQuery pageQuery,
                                                       @RequestParam(required = false) String brandName,
                                                       @RequestParam(required = false) String kindName) {
        return Result.success(service.page(pageQuery, brandName, kindName));
    }

    @GetMapping("/list")
    @ApiOperation("全部列表（供下拉选择）")
    @RequiresPermissions("sticker:brand-template:query")
    public Result<List<BrandTemplateMatch>> list() {
        return Result.success(service.listAll());
    }

    @GetMapping("/templates")
    @ApiOperation("去重模板列表（供字段映射选择）")
    @RequiresPermissions("sticker:brand-template:query")
    public Result<List<String>> templateNames() {
        return Result.success(service.getDistinctTemplateNames());
    }

    @GetMapping("/kinds")
    @ApiOperation("类别列表（从ERP查询）")
    @RequiresPermissions("sticker:brand-template:query")
    public Result<?> getKinds() {
        return Result.success(bjerpProductMapper.getKinds());
    }

    @GetMapping("/{id}")
    @ApiOperation("详情")
    @RequiresPermissions("sticker:brand-template:query")
    public Result<BrandTemplateMatch> getById(@PathVariable String id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    @ApiOperation("新增")
    @RequiresPermissions("sticker:brand-template:add")
    public Result<?> create(@Valid @RequestBody BrandTemplateMatch entity) {
        service.create(entity);
        return Result.success("新增成功");
    }

    @PutMapping("/{id}")
    @ApiOperation("修改")
    @RequiresPermissions("sticker:brand-template:edit")
    public Result<?> update(@PathVariable String id, @Valid @RequestBody BrandTemplateMatch entity) {
        service.update(id, entity);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除")
    @RequiresPermissions("sticker:brand-template:delete")
    public Result<?> delete(@PathVariable String id) {
        service.delete(id);
        return Result.success("删除成功");
    }

    @PostMapping("/import")
    @ApiOperation("Excel 批量导入（upsert）")
    @RequiresPermissions("sticker:brand-template:import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.paramError("文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls"))) {
            return Result.paramError("仅支持 .xlsx / .xls 格式");
        }

        List<BrandTemplateMatch> rows = new ArrayList<>();
        List<String> parseErrors = new ArrayList<>();
        int total;
        try (ExcelReader reader = ExcelUtil.getReader(file.getInputStream())) {
            List<Map<String, Object>> rawRows = reader.readAll();
            total = rawRows.size();
            for (int i = 0; i < rawRows.size(); i++) {
                int rowNum = i + 2; // 第1行是表头
                Map<String, Object> raw = rawRows.get(i);
                try {
                    String brandName = getCellString(raw, "品牌名称", "品牌", "brandName", "BRANDNAME");
                    String kindName = getCellString(raw, "类别名称", "类别", "kindName", "KINDNAME");
                    if (brandName == null || brandName.isBlank()) {
                        parseErrors.add("第" + rowNum + "行：品牌名称不能为空");
                        continue;
                    }
                    if (kindName == null || kindName.isBlank()) {
                        parseErrors.add("第" + rowNum + "行：类别名称不能为空");
                        continue;
                    }
                    String templateName = getCellString(raw, "打印模板", "模板", "templateName", "TEMPLATENAME");
                    if (templateName == null || templateName.isBlank()) {
                        parseErrors.add("第" + rowNum + "行：打印模板不能为空");
                        continue;
                    }
                    BrandTemplateMatch row = new BrandTemplateMatch();
                    row.setBrandName(brandName.trim());
                    row.setKindName(kindName.trim());
                    row.setTemplateName(templateName.trim());
                    row.setPrinterName(getCellString(raw, "默认打印机", "打印机", "printerName", "PRINTERNAME"));
                    row.setRemark(getCellString(raw, "备注", "remark", "REMARK"));
                    String status = getCellString(raw, "状态", "isActive", "ISACTIVE");
                    if (status != null && !status.isBlank()) {
                        row.setIsActive(status.trim().equals("1") || status.trim().contains("启用") ? 1 : 0);
                    } else {
                        row.setIsActive(1);
                    }
                    rows.add(row);
                } catch (Exception e) {
                    parseErrors.add("第" + rowNum + "行：" + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("读取 Excel 失败", e);
            return Result.error("导入失败：" + e.getMessage());
        }

        // 执行 upsert（含品牌/类别名称反查 ID）
        List<String> bizErrors = new ArrayList<>();
        int success = rows.isEmpty() ? 0 : service.importBatch(rows, bizErrors);
        int fail = total - success;

        List<String> errors = new ArrayList<>(parseErrors.size() + bizErrors.size());
        errors.addAll(parseErrors);
        errors.addAll(bizErrors);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        return Result.success(result);
    }

    @GetMapping("/template")
    @ApiOperation("下载导入模板")
    @RequiresPermissions("sticker:brand-template:import")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                URLEncoder.encode("品牌模板关系导入模板.xlsx", StandardCharsets.UTF_8.name()));

        ExcelWriter writer = ExcelUtil.getWriter(true);
        try {
            writer.addHeaderAlias("brandName", "品牌名称");
            writer.addHeaderAlias("kindName", "类别名称");
            writer.addHeaderAlias("templateName", "打印模板");
            writer.addHeaderAlias("printerName", "默认打印机");
            writer.addHeaderAlias("remark", "备注");
            writer.addHeaderAlias("isActive", "状态");

            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("brandName", "示例品牌");
            sample.put("kindName", "示例类别");
            sample.put("templateName", "NL_85X55_1L5C.btw");
            sample.put("printerName", "");
            sample.put("remark", "");
            sample.put("isActive", "1");

            writer.write(Collections.singletonList(sample), true);
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }

    /**
     * 多别名兼容取单元格值（支持中文表头/驼峰/大写）
     */
    private static String getCellString(Map<String, Object> row, String... keys) {
        if (row == null) return null;
        for (String key : keys) {
            Object val = row.get(key);
            if (val != null) {
                String s = val.toString().trim();
                if (!s.isEmpty()) return s;
            }
        }
        return null;
    }
}
