package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户角色关联实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("bc_sports_sys_user_role")
public class UserRole {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    private String id;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 角色ID
     */
    private String roleId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 创建时间
     */
    private String createBy;
}
