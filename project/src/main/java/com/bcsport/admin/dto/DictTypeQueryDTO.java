package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 字典类型查询传输对象
 */
@Data
public class DictTypeQueryDTO {

    /**
     * 字典名称（模糊查询，可空）
     */
    private String dictName;

    /**
     * 字典类型编码（模糊查询，可空）
     */
    private String dictType;
}
