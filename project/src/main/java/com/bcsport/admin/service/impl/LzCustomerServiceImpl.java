package com.bcsport.admin.service.impl;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.bcsport.admin.erpmapper.LzCustomerMapper;
import com.bcsport.admin.service.LzCustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 揽众客户押金资料服务实现（F63: 导入逻辑自 Controller 下沉）
 */
@Slf4j
@Service
public class LzCustomerServiceImpl implements LzCustomerService {

    private static final int BATCH_SIZE = 200;

    @Autowired
    private LzCustomerMapper lzCustomerMapper;

    @Override
    public Map<String, Object> importExcel(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("请上传 Excel 文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new IllegalArgumentException("仅支持 .xlsx 或 .xls 格式的 Excel 文件");
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
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        return result;
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
