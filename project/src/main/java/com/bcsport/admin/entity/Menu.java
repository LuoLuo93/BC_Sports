package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 菜单权限实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bc_sports_sys_menu")
public class Menu extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
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
     * 菜单类型名：目录，1：菜单，2：按钮/权限)
     */
    private Integer menuType;
    
    /**
     * 路由路径
     */
    private String path;
    
    /**
     * 组件路径
     */
    @TableField(exist = false)
    private String component;
    
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
     * 是否可见，0：隐藏，1：显示）
     */
    private Integer visible;
    
    /**
     * 自定义CSS)
     */
    @TableField(exist = false)
    private String customClass;
    
    /**
     * 图标颜色
     */
    private String iconColor;
    
    /**
     * 徽章内容
     */
    @TableField(exist = false)
    private String badge;
    
    /**
     * 徽章颜色
     */
    @TableField(exist = false)
    private String badgeColor;
    
    /**
     * 菜单描述
     */
    private String description;
    
    /**
     * 子菜单列表（非数据库字段)
     */
    @TableField(exist = false)
    private List<Menu> children;
}
