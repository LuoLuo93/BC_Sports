package com.bcsport.admin.service.impl;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.sax.handler.RowHandler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.qywx.BasFirstAdd;
import com.bcsport.admin.entity.qywx.BasFirstAddImportLog;
import com.bcsport.admin.qywxmapper.BasFirstAddImportLogMapper;
import com.bcsport.admin.qywxmapper.BasFirstAddMapper;
import com.bcsport.admin.service.BasFirstAddService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 客户首次添加记录导入实现
 * 采用 SAX 流式读取 + 分批入库，支持百万级行数据，内存占用低
 */
@Slf4j
@Service
public class BasFirstAddServiceImpl implements BasFirstAddService {

    /** SQL Server 单条 INSERT 参数上限 2100，每行 5 个字段，最多 420 行，取 400 留余量 */
    private static final int BATCH_SIZE = 400;
    private static final int MAX_ERRORS = 100;
    /** 行数安全上限（200万），防止恶意超大文件 */
    private static final int MAX_ROWS = 2_000_000;

    /** 表头别名 → 字段标识（兼容中英文表头） */
    private static final Map<String, String> HEADER_ALIAS = new HashMap<>();
    static {
        HEADER_ALIAS.put("客户id", "customerId");
        HEADER_ALIAS.put("客户ID", "customerId");
        HEADER_ALIAS.put("customer_id", "customerId");
        HEADER_ALIAS.put("customerid", "customerId");
        HEADER_ALIAS.put("首次添加时间", "firstAddTime");
        HEADER_ALIAS.put("first_add_time", "firstAddTime");
        HEADER_ALIAS.put("首次添加人部门", "firstAdderDept");
        HEADER_ALIAS.put("first_adder_dept", "firstAdderDept");
        HEADER_ALIAS.put("首次添加人店铺", "firstAdderStore");
        HEADER_ALIAS.put("first_adder_store", "firstAdderStore");
        HEADER_ALIAS.put("首次添加人", "firstAdder");
        HEADER_ALIAS.put("first_adder", "firstAdder");
    }

    @Autowired
    private BasFirstAddMapper basFirstAddMapper;

    @Autowired
    private BasFirstAddImportLogMapper logMapper;

    @Autowired
    @Qualifier("qywxTransactionManager")
    private PlatformTransactionManager transactionManager;

    @Override
    public PageResult<BasFirstAdd> page(PageQuery pageQuery, String customerId, String firstAdder) {
        Page<BasFirstAdd> page = pageQuery.toPage();
        LambdaQueryWrapper<BasFirstAdd> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(customerId)) {
            wrapper.like(BasFirstAdd::getCustomerId, customerId);
        }
        if (StringUtils.hasText(firstAdder)) {
            wrapper.like(BasFirstAdd::getFirstAdder, firstAdder);
        }
        wrapper.orderByDesc(BasFirstAdd::getId);
        Page<BasFirstAdd> result = basFirstAddMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) throws Exception {
        // 0. 探测文件真实格式（魔数），拒绝伪 Excel（HTML/CSV 改扩展名）
        String realFormat = detectFormat(file);
        log.info("Bas_FirstAdd 导入文件真实格式: {}", realFormat);
        if (!"xlsx".equals(realFormat) && !"xls".equals(realFormat)) {
            return buildResult(0, 0, 0, Collections.singletonList(
                    "文件不是标准的 Excel 格式（检测为 " + realFormat + "），请用 Excel 打开后另存为 .xlsx 再上传"));
        }

        AtomicInteger total = new AtomicInteger(0);
        AtomicInteger success = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        // 列索引映射：表头 → 字段
        Map<String, Integer> columnIndex = new HashMap<>();
        // 缓冲区（去重用 Set + 实体列表）
        Set<String> batchIds = new HashSet<>();
        List<BasFirstAdd> buffer = new ArrayList<>(BATCH_SIZE);

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        RowHandler handler = (sheetIndex, rowIndex, rowCells) -> {
            // 第 0 行为表头：仅在尚未识别到表头时建立列映射（多 sheet 场景下用第一个有效表头）
            if (rowIndex == 0) {
                if (columnIndex.isEmpty() && rowCells != null) {
                    for (int i = 0; i < rowCells.size(); i++) {
                        Object h = rowCells.get(i);
                        if (h == null) continue;
                        String field = HEADER_ALIAS.get(String.valueOf(h).trim());
                        if (field != null) columnIndex.put(field, i);
                    }
                }
                return;
            }
            // 跳过空行
            if (rowCells == null || rowCells.isEmpty()) return;

            int rowNum = (int) rowIndex + 1;
            int cnt = total.incrementAndGet();
            if (cnt > MAX_ROWS) {
                if (errors.size() < MAX_ERRORS) {
                    errors.add("数据超过 " + MAX_ROWS + " 行上限，已停止处理");
                }
                return;
            }

            try {
                BasFirstAdd entity = mapRow(rowCells, columnIndex);
                String key = entity.getCustomerId();
                if (key == null || key.isEmpty()) {
                    if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：客户id不能为空");
                    return;
                }
                // 文件内去重：同一文件内相同 customerId 只保留最后一条
                if (batchIds.contains(key)) {
                    // 覆盖之前那条：从 buffer 中移除旧的，再加入新的
                    buffer.removeIf(e -> key.equals(e.getCustomerId()));
                } else {
                    batchIds.add(key);
                }
                buffer.add(entity);

                // 缓冲区满则 MERGE 入库并清空
                if (buffer.size() >= BATCH_SIZE) {
                    flushBatch(txTemplate, buffer, batchIds, success, errors);
                }
            } catch (Exception e) {
                if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：解析异常 - " + e.getMessage());
            }
        };

        // 2. SAX 流式读取全部 sheet（数据可能在非第一个 sheet，如汇总页之后的数据页）
        readAllSheets(file, realFormat, handler);

        // 3. 入库剩余缓冲
        if (!buffer.isEmpty()) {
            flushBatch(txTemplate, buffer, batchIds, success, errors);
        }

        int fail = total.get() - success.get();
        if (fail > MAX_ERRORS && !errors.isEmpty()) {
            errors.add("...共 " + fail + " 条未导入，仅显示前 " + MAX_ERRORS + " 条");
        }
        log.info("Bas_FirstAdd 导入完成: total={}, success={}, fail={}", total.get(), success.get(), fail);

        // 4. 记录导入日志
        String status = (total.get() == 0) ? "FAILED" : (fail == 0 ? "SUCCESS" : "PARTIAL");
        saveImportLog(file, total.get(), success.get(), fail, status, errors);

        return buildResult(total.get(), success.get(), fail, errors);
    }

    @Override
    public PageResult<BasFirstAddImportLog> logPage(PageQuery pageQuery) {
        Page<BasFirstAddImportLog> page = logMapper.selectPage(pageQuery.toPage(),
                new LambdaQueryWrapper<BasFirstAddImportLog>().orderByDesc(BasFirstAddImportLog::getId));
        return PageResult.of(page);
    }

    /**
     * 保存导入日志
     */
    private void saveImportLog(MultipartFile file, int total, int success, int fail, String status, List<String> errors) {
        try {
            BasFirstAddImportLog logEntity = new BasFirstAddImportLog();
            logEntity.setFileName(file.getOriginalFilename());
            logEntity.setFileSize(file.getSize());
            logEntity.setTotalCount(total);
            logEntity.setSuccessCount(success);
            logEntity.setFailCount(fail);
            logEntity.setStatus(status);
            if (!errors.isEmpty()) {
                String msg = String.join("\n", errors);
                logEntity.setErrorMsg(msg.length() > 4000 ? msg.substring(0, 4000) : msg);
            }
            logEntity.setCreateBy(com.bcsport.admin.util.ShiroSecurityUtils.getCurrentUsername());
            logEntity.setCreateTime(java.time.LocalDateTime.now());
            logMapper.insert(logEntity);
        } catch (Exception e) {
            log.warn("保存导入日志失败: {}", e.getMessage());
        }
    }

    /**
     * MERGE upsert 入库 + 清空缓冲（独立短事务）
     * 一条 SQL 同时处理插入和更新，无需预加载全表 ID
     */
    private void flushBatch(TransactionTemplate txTemplate, List<BasFirstAdd> buffer,
                            Set<String> batchIds, AtomicInteger success, List<String> errors) {
        if (buffer.isEmpty()) return;
        List<BasFirstAdd> toWrite = new ArrayList<>(buffer);
        buffer.clear();
        batchIds.clear();
        txTemplate.execute(status -> {
            basFirstAddMapper.mergeBatch(toWrite);
            return null;
        });
        success.addAndGet(toWrite.size());
    }

    /**
     * 读取全部 sheet（xlsx 复用同一个 OPCPackage；xls 逐个尝试到越界）
     */
    private void readAllSheets(MultipartFile file, String format, RowHandler handler) throws Exception {
        if ("xlsx".equals(format)) {
            org.apache.poi.openxml4j.opc.OPCPackage pkg = org.apache.poi.openxml4j.opc.OPCPackage.open(file.getInputStream());
            try {
                int sheetCount = pkg.getPartsByName(java.util.regex.Pattern.compile("/xl/worksheets/.*\\.xml")).size();
                log.info("Bas_FirstAdd xlsx 共 {} 个 sheet", sheetCount);
                cn.hutool.poi.excel.sax.Excel07SaxReader saxReader = new cn.hutool.poi.excel.sax.Excel07SaxReader(handler);
                for (int s = 0; s < sheetCount; s++) {
                    saxReader.read(pkg, s);
                }
            } finally {
                pkg.revert();
            }
        } else {
            // xls：逐个 sheet 尝试，越界则停止
            for (int s = 0; s < 20; s++) {
                try {
                    ExcelUtil.readBySax(file.getInputStream(), s, handler);
                } catch (Exception e) {
                    break;
                }
            }
        }
    }

    /**
     * 按列索引将单元格映射到实体（表头未识别时按固定顺序兜底）
     */
    private BasFirstAdd mapRow(List<Object> cells, Map<String, Integer> columnIndex) {
        BasFirstAdd e = new BasFirstAdd();
        boolean useHeader = !columnIndex.isEmpty();
        e.setCustomerId(cellStr(cells, useHeader ? columnIndex.get("customerId") : 0));
        e.setFirstAddTime(cellStr(cells, useHeader ? columnIndex.get("firstAddTime") : 1));
        e.setFirstAdderDept(cellStr(cells, useHeader ? columnIndex.get("firstAdderDept") : 2));
        e.setFirstAdderStore(cellStr(cells, useHeader ? columnIndex.get("firstAdderStore") : 3));
        e.setFirstAdder(cellStr(cells, useHeader ? columnIndex.get("firstAdder") : 4));
        return e;
    }

    private String cellStr(List<Object> cells, Integer idx) {
        if (idx == null || idx < 0 || idx >= cells.size()) return null;
        Object v = cells.get(idx);
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * 通过文件头魔数探测真实格式
     * xlsx: ZIP 头 PK\x03\x04；xls: OLE 复合文档头 D0CF11E0；其余多为 HTML/CSV/XML
     */
    private String detectFormat(MultipartFile file) throws Exception {
        byte[] head = new byte[8];
        try (java.io.InputStream in = file.getInputStream()) {
            int read = in.read(head);
            if (read < 4) return "unknown(空文件)";
        }
        // PK 头 = xlsx(zip)
        if ((head[0] & 0xFF) == 0x50 && (head[1] & 0xFF) == 0x4B) {
            return "xlsx";
        }
        // OLE 复合文档 = xls
        if ((head[0] & 0xFF) == 0xD0 && (head[1] & 0xFF) == 0xCF
                && (head[2] & 0xFF) == 0x11 && (head[3] & 0xFF) == 0xE0) {
            return "xls";
        }
        // 文本类（HTML/XML/CSV）
        String preview = new String(head, java.nio.charset.StandardCharsets.ISO_8859_1).trim();
        String lower = preview.toLowerCase();
        if (lower.startsWith("<") || preview.contains("<table") || preview.contains("<html")
                || preview.contains("<?xml")) {
            return "HTML/XML（伪Excel）";
        }
        if (lower.contains(",") || lower.contains("\t") || lower.contains(";")) {
            return "CSV/文本（伪Excel）";
        }
        return "未知格式";
    }

    private Map<String, Object> buildResult(int total, int success, int fail, List<String> errors) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        return result;
    }
}
