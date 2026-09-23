package com.bcsport.admin.exporter;

import java.util.function.Function;

/**
 * 导出列定义：表头文案 + 从行对象取单元格值的函数。
 */
public record ExportColumn<T>(String header, Function<T, Object> value) {

    public static <T> ExportColumn<T> of(String header, Function<T, Object> value) {
        return new ExportColumn<>(header, value);
    }
}
