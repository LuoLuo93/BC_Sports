package com.bcsport.admin.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 角色数据传输对象（新增或修改完 */
@Data
public class RoleDTO {

    private String id;
    
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50个字符")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码长度不能超过50个字符")
    private String roleCode;

    @Size(max = 200, message = "角色描述长度不能超过200个字符")
    private String description;
    
    private Integer status;
    
    private Integer sort;
}
