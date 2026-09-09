package com.bcsport.admin.importer;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 单行上下文：行号、单元格、表头列映射，以及各服务原本各自拷贝的取值工具。
 * 表头未识别时（columnIndex 为空），带 fallbackIdx 的方法按固定列序兜底——与现有各服务 mapRow 语义一致。
 */
public final class RowCtx {

    private final int rowNum;
    private final List<Object> cells;
    private final Map<String, Integer> columnIndex;

    RowCtx(int rowNum, List<Object> cells, Map<String, Integer> columnIndex) {
        this.rowNum = rowNum;
        this.cells = cells == null ? Collections.emptyList() : cells;
        this.columnIndex = columnIndex;
    }

    /** Excel 行号（表头为第 1 行） */
    public int rowNum() {
        return rowNum;
    }

    public List<Object> cells() {
        return cells;
    }

    /** 是否识别到了表头（识别到则按字段名取列，否则按固定列序） */
    public boolean hasHeader() {
        return !columnIndex.isEmpty();
    }

    /** 字段对应的列下标；表头未识别时用 fallbackIdx，识别到但缺该列返回 null */
    public Integer indexOf(String field, int fallbackIdx) {
        return hasHeader() ? columnIndex.get(field) : Integer.valueOf(fallbackIdx);
    }

    /** 仅按表头取列，不做固定列序兜底（表头缺列即 null） */
    public Integer indexOf(String field) {
        return columnIndex.get(field);
    }

    public String str(String field, int fallbackIdx) {
        return strAt(indexOf(field, fallbackIdx));
    }

    public String str(String field) {
        return strAt(indexOf(field));
    }

    /** 去首尾空白，空串视为 null */
    public String strAt(Integer idx) {
        if (idx == null || idx < 0 || idx >= cells.size()) {
            return null;
        }
        Object v = cells.get(idx);
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    /** 解析失败返回 null（宽松）；需要严格校验的 Spec 自行判空后抛 IllegalArgumentException */
    public Long longOrNull(String field, int fallbackIdx) {
        BigDecimal d = bigDecimalOrNull(field, fallbackIdx);
        return d == null ? null : d.longValue();
    }

    public Integer intOrNull(String field, int fallbackIdx) {
        BigDecimal d = bigDecimalOrNull(field, fallbackIdx);
        return d == null ? null : d.intValue();
    }

    public BigDecimal bigDecimalOrNull(String field, int fallbackIdx) {
        String s = str(field, fallbackIdx);
        if (s == null) {
            return null;
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
