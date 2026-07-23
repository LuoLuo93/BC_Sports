package com.bcsport.admin.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记实体字段的中文显示名称
 * 用于自动读取可用字段列表（如打印字段映射下拉框）
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldLabel {
    /** 字段的中文显示名称 */
    String value();

    /** 是否在字段映射数据源下拉中隐藏（内部修正用字段等，默认不隐藏） */
    boolean hidden() default false;
}
