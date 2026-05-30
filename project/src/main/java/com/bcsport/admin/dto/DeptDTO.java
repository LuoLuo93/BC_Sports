package com.bcsport.admin.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 部门数据传输对象
 */
@Data
public class DeptDTO {
    
    private String id;

    private String parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 50, message = "部门名称长度不能超过50个字符")
    private String deptName;

    @NotNull(message = "排序不能为空")
    private Integer sort;

    private String leader;

    private String phone;

    private String email;

    @NotNull(message = "状态不能为null")
    private Integer status;
}
