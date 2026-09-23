package com.bcsport.admin.exporter;

import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导出唯一流程骨架（与导入侧 ExcelImportRunner 对称）。各模块只提供 {@link ExcelExportSpec}，本类负责：
 * 行数熔断检查 → 分批 fetch → BigExcelWriter(SXSSF 流式)逐行写 → flush 到响应流。
 * 全程只在内存保留一个批次，十万行级导出不会撑爆堆。
 * 每次 write 无共享可变状态，线程安全。
 * <p>
 * 导出为只读操作，审计暂走 controller 上的 @OperLog 操作日志，不建统一导出日志表；
 * 将来需要"导出日志"面板时，在 {@link #write} 的收尾处挂 ExportLogRecorder 即可（与导入侧对称）。
 */
@Slf4j
@Component
public class ExcelExportRunner {

    public <T> long write(ExcelExportSpec<T> spec, OutputStream out) throws IOException {
        long total = spec.count();
        if (total > spec.maxRows()) {
            throw new IllegalArgumentException(
                    "导出数据量 " + total + " 行超过上限 " + spec.maxRows() + " 行，请增加筛选条件缩小范围后重试");
        }
        List<ExportColumn<T>> columns = spec.columns();
        List<Object> headers = new ArrayList<>(columns.size());
        for (ExportColumn<T> col : columns) {
            headers.add(col.header());
        }
        log.info("{} 开始导出: 预计 {} 行, {} 列, 每批 {} 行", spec.logLabel(), total, columns.size(), spec.batchSize());

        BigExcelWriter writer = ExcelUtil.getBigWriter();
        long written;
        try {
            writer.writeRow(headers);
            written = 0;
            while (written < total) {
                List<T> batch = spec.fetch(written, spec.batchSize());
                if (batch == null || batch.isEmpty()) {
                    // 数据在导出过程中被删减，少于 count：按实际取到的行数收尾
                    log.warn("{} 取数提前结束: 预计 {} 行, 实际 {} 行", spec.logLabel(), total, written);
                    break;
                }
                for (T row : batch) {
                    List<Object> cells = new ArrayList<>(columns.size());
                    for (ExportColumn<T> col : columns) {
                        cells.add(cellValue(col.value().apply(row)));
                    }
                    writer.writeRow(cells);
                }
                written += batch.size();
                if (batch.size() < spec.batchSize() && written < total) {
                    // 不足一批说明数据已取尽（fetch 实现保证批内连续），避免再发一次空查询
                    break;
                }
            }
            writer.flush(out);
        } finally {
            writer.close();
        }
        log.info("{} 导出完成: 实际写出 {} 行", spec.logLabel(), written);
        return written;
    }

    /** null 落单元格为空白；其余类型（数字/字符串）原样交给 POI 按对应单元格类型写 */
    private Object cellValue(Object v) {
        return v == null ? "" : v;
    }
}
