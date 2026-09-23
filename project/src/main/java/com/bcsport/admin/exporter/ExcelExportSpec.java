package com.bcsport.admin.exporter;

import java.util.List;

/**
 * Excel 导出规格：各模块只声明列与数据来源，流式写/分批捞/行数熔断由 {@link ExcelExportRunner} 统一处理。
 * 与导入侧 ExcelImportSpec 对称。行类型自定（贴纸资料为 Map&lt;String,Object&gt;）。
 */
public interface ExcelExportSpec<T> {

    /** 日志标识（如"贴纸资料导出"） */
    String logLabel();

    /** 总行数（写出前先 count，供进度日志与行数熔断） */
    long count();

    /** 分批取数：返回 [offset, offset+limit) 的一批行，取尽返回空列表 */
    List<T> fetch(long offset, int limit);

    /** 列定义：顺序即 Excel 列序 */
    List<ExportColumn<T>> columns();

    /** 每批捞取行数（同时是内存中最多驻留的行数） */
    default int batchSize() {
        return 2000;
    }

    /** 行数熔断上限：超过直接拒绝导出，防止误把超大数据源整表拖出 */
    default long maxRows() {
        return 200_000;
    }
}
