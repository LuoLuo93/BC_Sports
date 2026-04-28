package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 部门查询传输对象
 */
@Data
public class DeptQueryDTO {
    private String deptName;
    private Integer status;
}
