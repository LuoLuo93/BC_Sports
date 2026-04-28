package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 角色查询传输对象
 */
@Data
public class RoleQueryDTO {
    private String roleName;
    private String roleCode;
    private Integer status;
}
