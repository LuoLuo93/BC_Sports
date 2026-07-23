package com.bcsport.admin.common;

import com.bcsport.admin.vo.FieldOption;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 字段标签工具类
 * 通过反射读取带有 @FieldLabel 注解的字段，生成下拉选项列表
 */
public class FieldLabelUtils {

    /**
     * 读取指定类中所有带有 @FieldLabel 注解的字段，返回字段选项列表
     *
     * @param clazz 实体类
     * @return 字段选项列表（value=字段名, label=中文名称）
     */
    public static List<FieldOption> getFields(Class<?> clazz) {
        List<FieldOption> options = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            FieldLabel annotation = field.getAnnotation(FieldLabel.class);
            if (annotation != null && !annotation.hidden()) {
                options.add(new FieldOption(field.getName(), annotation.value()));
            }
        }
        return options;
    }
}
