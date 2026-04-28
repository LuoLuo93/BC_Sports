package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 用户查询传输对象
 */
@Data
public class UserQueryDTO {
    
    /**
     * 用户名     */
    private String username;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 手机号     */
    private String phone;
    
    /**
     * 状态（0：禁用，1：启用）
     */
    private Integer status;
    
    /**
     * 部门ID
     */
    private String deptId;
}
