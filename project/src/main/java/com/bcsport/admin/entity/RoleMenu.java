package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 角色菜单关联实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("bc_sports_sys_role_menu")
public class RoleMenu {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    private String id;
    
    /**
     * 角色ID
     */
    private String roleId;
    
    /**
     * 菜单ID
     */
    private String menuId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 创建时间
     */
    private String createBy;
}
