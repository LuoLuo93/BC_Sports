package com.bcsport.admin.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 菜单数据传输对象
 */
@Data
public class MenuDTO {
    
    private String id;
    
    /**
     * 父菜单ID
     */
    private String parentId;
    
    /**
     * 菜单名称
     */
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50个字符")
    private String menuName;
    
    /**
     * 菜单图标
     */
    private String icon;
    
    /**
     * 菜单类型：0-目录，1-菜单，2-按钮/权限
     */
    private Integer menuType;
    
    /**
     * 路由路径
     */
    private String path;
    
    /**
     * 权限标识
     */
    private String permission;
    
    /**
     * 排序
     */
    private Integer sort;
    
    /**
     * 状态（0：禁用，1：启用）
     */
    private Integer status;
    
    /**
     * 是否可见：0-隐藏，1-显示
     */
    private Integer visible;
    
    /**
     * 图标颜色
     */
    private String iconColor;
    
    /**
     * 菜单描述
     */
    private String description;
}
