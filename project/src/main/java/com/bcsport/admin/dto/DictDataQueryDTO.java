package com.bcsport.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字典数据查询传输对象
 */
@Data
public class DictDataQueryDTO {

    @NotBlank(message = "字典类型不能为空")
    private String dictType;
}
