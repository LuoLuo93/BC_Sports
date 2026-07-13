package com.bcsport.admin.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelWriter;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.erpmapper.LzCustomerMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 揽众客户押金资料管理（数据源：bjerp LZCUSTOMERINFOR）
 */
@Slf4j
@RestController
@RequestMapping("/api/lz-customer")
@Api(tags = "揽众客户押金资料")
public class LzCustomerController {

    private static final int BATCH_SIZE = 200;

    @Autowired
    private LzCustomerMapper lzCustomerMapper;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    @ApiOperation("分页查询揽众客户押金资料")
    @RequiresPermissions("erp:lzCustomer:query")
    public Result<PageResult<Map<String, Object>>> page(PageQuery pageQuery,
                                                         @RequestParam(required = false) String shopCode,
                                                         @RequestParam(required = false) String shopName,
                                                         @RequestParam(required = false) String shopBoss) {
        int safePageSize = Math.min(pageQuery.getPageSize() != null ? pageQuery.getPageSize() : 10, 500);
        int safePageNum = Math.max(pageQuery.getPageNum() != null ? pageQuery.getPageNum() : 1, 1);
        long offset = (long) (safePageNum - 1) * safePageSize;

        long total = lzCustomerMapper.countLzCustomers(shopCode, shopName, shopBoss);
        List<Map<String, Object>> list = lzCustomerMapper.searchLzCustomers(shopCode, shopName, shopBoss, offset, safePageSize);

        PageResult<Map<String, Object>> pageResult = new PageResult<>();
        pageResult.setPageNum((long) safePageNum);
        pageResult.setPageSize((long) safePageSize);
        pageResult.setTotal(total);
        pageResult.setPages((total + safePageSize - 1) / safePageSize);
        pageResult.setRecords(list);
        pageResult.setHasPrevious(safePageNum > 1);
        pageResult.setHasNext(safePageNum < pageResult.getPages());

        return Result.success(pageResult);
    }

    /**
     * 查询单条
     */
    @GetMapping("/{id}")
    @ApiOperation("查询单条揽众客户资料")
    @RequiresPermissions("erp:lzCustomer:query")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> data = lzCustomerMapper.getLzCustomerById(id);
        if (data == null) {
            return Result.error("数据不存在");
        }
        return Result.success(data);
    }

    /**
     * 新增
     */
    @PostMapping
    @ApiOperation("新增揽众客户押金资料")
    @RequiresPermissions("erp:lzCustomer:add")
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        Map<String, Object> map = new HashMap<>();
        map.put("shopCode", body.get("shopCode"));
        map.put("shopName", body.get("shopName"));
        map.put("shopBoss", body.get("shopBoss"));
        map.put("fundingLimit", body.get("fundingLimit"));
        map.put("fundingRatio", body.get("fundingRatio"));
        lzCustomerMapper.insertLzCustomer(map);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/{id}")
    @ApiOperation("修改揽众客户押金资料")
    @RequiresPermissions("erp:lzCustomer:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("shopCode", body.get("shopCode"));
        map.put("shopName", body.get("shopName"));
        map.put("shopBoss", body.get("shopBoss"));
        map.put("fundingLimit", body.get("fundingLimit"));
        map.put("fundingRatio", body.get("fundingRatio"));
        lzCustomerMapper.updateLzCustomer(map);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除揽众客户押金资料")
    @RequiresPermissions("erp:lzCustomer:delete")
    public Result<Void> delete(@PathVariable Long id) {
        lzCustomerMapper.deleteLzCustomer(id);
        return Result.success();
    }

    /**
     * Excel 批量导入（以店铺代码为业务键，存在则更新，不存在则新增）
     */
    @PostMapping("/import")
    @ApiOperation("Excel批量导入揽众客户资料")
    @RequiresPermissions("erp:lzCustomer:import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.paramError("请上传 Excel 文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return Result.paramError("仅支持 .xlsx 或 .xls 格式的 Excel 文件");
        }

        List<Map<String, Object>> allRows = new ArrayList<>();
        int total = 0;
        int success = 0;
        int fail = 0;
        List<String> errors = new ArrayList<>();

        try (ExcelReader reader = ExcelUtil.getReader(file.getInputStream())) {
            List<Map<String, Object>> rawRows = reader.readAll();
            total = rawRows.size();

            for (int i = 0; i < rawRows.size(); i++) {
                Map<String, Object> raw = rawRows.get(i);
                int rowNum = i + 2; // Excel 行号（第1行是表头）
                try {
                    String shopCode = getCellString(raw, "店铺代码", "shopcode", "SHOPCODE");
                    if (shopCode == null || shopCode.trim().isEmpty()) {
                        fail++;
                        errors.add("第" + rowNum + "行：店铺代码不能为空");
                        continue;
                    }
                    Map<String, Object> row = new HashMap<>();
                    row.put("shopCode", shopCode.trim());
                    row.put("shopName", getCellString(raw, "店铺名称", "shopname", "SHOPNAME"));
                    row.put("shopBoss", getCellString(raw, "门店所属联营老板", "shopboss", "SHOPBOSS"));
                    row.put("fundingLimit", getCellString(raw, "资金额度", "fundinglimit", "FUNDINGLIMIT"));
                    row.put("fundingRatio", getCellString(raw, "资金倍率", "fundingratio", "FUNDINGRATIO"));
                    allRows.add(row);
                } catch (Exception e) {
                    fail++;
                    errors.add("第" + rowNum + "行：" + e.getMessage());
                }
            }

            // 分批 merge
            for (int i = 0; i < allRows.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allRows.size());
                lzCustomerMapper.mergeBatch(allRows.subList(i, end));
                success += end - i;
            }
        } catch (Exception e) {
            log.error("揽众资料导入失败: {}", e.getMessage(), e);
            return Result.error("导入失败：" + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        return Result.success(result);
    }

    /**
     * 下载导入模板
     */
    @GetMapping("/template")
    @ApiOperation("下载导入模板")
    @RequiresPermissions("erp:lzCustomer:import")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                URLEncoder.encode("揽众客户资料导入模板.xlsx", StandardCharsets.UTF_8.name()));

        ExcelWriter writer = ExcelUtil.getWriter(true);
        try {
            writer.addHeaderAlias("shopCode", "店铺代码");
            writer.addHeaderAlias("shopName", "店铺名称");
            writer.addHeaderAlias("shopBoss", "门店所属联营老板");
            writer.addHeaderAlias("fundingLimit", "资金额度");
            writer.addHeaderAlias("fundingRatio", "资金倍率");

            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("shopCode", "NL001");
            sample.put("shopName", "示例店铺名称");
            sample.put("shopBoss", "示例老板");
            sample.put("fundingLimit", "100000");
            sample.put("fundingRatio", "1.5");

            writer.write(Collections.singletonList(sample), true);
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }

    /**
     * 从 Excel 行中按多个可能的列名取值（兼容中英文表头）
     */
    private static String getCellString(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object val = row.get(key);
            if (val != null) {
                String str = val.toString().trim();
                if (!str.isEmpty()) return str;
            }
        }
        return null;
    }
}
