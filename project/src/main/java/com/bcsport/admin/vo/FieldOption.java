package com.bcsport.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用字段选项（用于下拉选择）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FieldOption {
    /** 字段值，如 "materialNumber" */
    private String value;
    /** 显示标签，如 "物料号/货号" */
    private String label;
}
