package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色视图对象
 */
@Data
public class RoleVO {

    private String id;
    
    private String roleName;
    
    private String roleCode;
    
    private String description;
    
    private Integer status;
    
    private Integer sort;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
