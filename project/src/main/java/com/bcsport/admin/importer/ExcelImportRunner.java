package com.bcsport.admin.importer;

import com.bcsport.admin.util.ExcelSaxUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Excel 导入唯一流程骨架（R01）。各模块只提供 {@link ExcelImportSpec}，本类负责：
 * 魔数探测 → SAX 流式读全部 sheet → 表头识别 → 逐行 mapRow → 行数熔断 → 文件内去重 →
 * 缓冲满即 onBatch → 收尾 flush → fail 截断提示 → 状态计算 → 写统一导入日志。
 * 每次 run 的状态都是局部变量，线程安全。
 */
@Slf4j
@Component
public class ExcelImportRunner {

    /** 行级错误上限（与原各服务 MAX_ERRORS 一致） */
    static final int MAX_ROW_ERRORS = 100;
    /** 批入库错误上限（与原各服务 flushBatch 一致） */
    static final int MAX_BATCH_ERRORS = 200;

    private final ImportLogRecorder logRecorder;

    public ExcelImportRunner(ImportLogRecorder logRecorder) {
        this.logRecorder = logRecorder;
    }

    public <T> ImportOutcome run(MultipartFile file, ExcelImportSpec<T> spec) throws Exception {
        String label = spec.logLabel();
        String format = ExcelSaxUtils.detectFormat(file);
        log.info("{} 导入文件真实格式: {}", label, format);
        if (!"xlsx".equals(format) && !"xls".equals(format)) {
            ImportOutcome rejected = ImportOutcome.rejected(
                    "文件不是标准的 Excel 格式（检测为 " + format + "），请用 Excel 打开后另存为 .xlsx 再上传");
            logRecorder.record(spec.type(), rejected, file);
            return rejected;
        }

        Session<T> session = new Session<>(spec);
        ExcelSaxUtils.readAllSheets(file, format, session::handleRow, label);
        // 整个文件未识别到任何表头（row0 无一命中别名）时，用空映射补一次校验：
        // 否则完全无表头的文件会走固定列序兜底把所有行按位置映射全部导入（GoodsOldNew 原实现在此场景报缺列拒收），
        // 空文件也会丢失"缺少必需列"提示
        session.eofHeaderCheck();
        if (session.abortMsg != null) {
            ImportOutcome rejected = ImportOutcome.rejected(session.abortMsg);
            logRecorder.record(spec.type(), rejected, file);
            return rejected;
        }
        session.flush();
        ImportOutcome outcome = session.finish();
        logRecorder.record(spec.type(), outcome, file);
        return outcome;
    }

    /** 单次导入的全部可变状态 */
    private static final class Session<T> implements BatchCtx {

        private final ExcelImportSpec<T> spec;
        private final String label;
        private final AtomicInteger total = new AtomicInteger(0);
        private final AtomicInteger success = new AtomicInteger(0);
        private final List<String> errors = Collections.synchronizedList(new ArrayList<>());
        private final Map<String, Integer> columnIndex = new HashMap<>();
        private final Set<String> dedupKeys = new HashSet<>();
        private final List<T> buffer;
        /** 表头校验失败的消息，非 null 时丢弃后续所有行 */
        String abortMsg;

        Session(ExcelImportSpec<T> spec) {
            this.spec = spec;
            this.label = spec.logLabel();
            this.buffer = new ArrayList<>(spec.batchSize());
        }

        void handleRow(int sheetIndex, long rowIndex, List<Object> rowCells) {
            // 第 0 行为表头：仅在尚未识别到表头时建立列映射（多 sheet 场景下用第一个有效表头）
            if (rowIndex == 0) {
                if (columnIndex.isEmpty() && rowCells != null) {
                    Map<String, String> alias = spec.headerAlias();
                    for (int i = 0; i < rowCells.size(); i++) {
                        Object h = rowCells.get(i);
                        if (h == null) continue;
                        String field = alias.get(String.valueOf(h).trim());
                        if (field != null) columnIndex.put(field, i);
                    }
                    if (!columnIndex.isEmpty()) {
                        try {
                            spec.validateHeaders(columnIndex);
                        } catch (IllegalArgumentException e) {
                            abortMsg = e.getMessage();
                        }
                    }
                }
                return;
            }
            if (abortMsg != null) return;
            if (rowCells == null || rowCells.isEmpty()) return;

            int rowNum = (int) rowIndex + 1;
            int cnt = total.incrementAndGet();
            if (cnt > spec.maxRows()) {
                rowError("数据超过 " + spec.maxRows() + " 行上限，已停止处理");
                return;
            }

            try {
                T entity = spec.mapRow(new RowCtx(rowNum, rowCells, columnIndex));
                if (entity == null) {
                    total.decrementAndGet();
                    return;
                }
                String key = spec.dedupKey(entity);
                if (key != null && !dedupKeys.add(key)) {
                    buffer.removeIf(e -> key.equals(spec.dedupKey(e)));
                }
                buffer.add(entity);
                if (buffer.size() >= spec.batchSize()) {
                    flush();
                }
            } catch (NumberFormatException e) {
                // NumberFormatException 继承 IllegalArgumentException，但它是解析失败不是业务校验，文案不能裸露 JDK 信息
                rowError("第" + rowNum + "行：解析异常 - " + e.getMessage());
            } catch (IllegalArgumentException e) {
                rowError("第" + rowNum + "行：" + e.getMessage());
            } catch (Exception e) {
                rowError("第" + rowNum + "行：解析异常 - " + e.getMessage());
            }
        }

        void flush() {
            if (buffer.isEmpty()) return;
            List<T> toWrite = new ArrayList<>(buffer);
            buffer.clear();
            dedupKeys.clear();
            try {
                spec.onBatch(toWrite, this);
            } catch (Exception e) {
                log.error("{} 批量入库失败, 本批{}条已丢弃", label, toWrite.size(), e);
                error("批量入库失败: " + e.getMessage());
            }
        }

        /** 读完全部 sheet 后仍无任何表头时的补校验（见 run() 注释） */
        void eofHeaderCheck() {
            if (abortMsg == null && columnIndex.isEmpty()) {
                try {
                    spec.validateHeaders(columnIndex);
                } catch (IllegalArgumentException e) {
                    abortMsg = e.getMessage();
                }
            }
        }

        ImportOutcome finish() {
            int fail = total.get() - success.get();
            spec.onFinish(total.get(), success.get(), fail, errors);
            if (fail > MAX_ROW_ERRORS && !errors.isEmpty()) {
                errors.add("...共 " + fail + " 条未导入，仅显示前 " + MAX_ROW_ERRORS + " 条");
            }
            log.info("{} 导入完成: total={}, success={}, fail={}", label, total.get(), success.get(), fail);
            return new ImportOutcome(total.get(), success.get(), fail, errors);
        }

        private void rowError(String msg) {
            if (errors.size() < MAX_ROW_ERRORS) errors.add(msg);
        }

        @Override
        public void success(int n) {
            success.addAndGet(n);
        }

        @Override
        public void error(String msg) {
            if (errors.size() < MAX_BATCH_ERRORS) errors.add(msg);
        }
    }
}
