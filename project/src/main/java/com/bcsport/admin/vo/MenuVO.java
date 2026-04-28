package com.bcsport.admin.vo;

import lombok.Data;
import java.util.List;

/**
 * 菜单视图对象
 */
@Data
public class MenuVO {
    
    private String id;
    
    private String parentId;
    
    private String menuName;
    
    private String icon;
    
    private Integer menuType;
    
    private String path;
    
    private String permission;
    
    private Integer sort;
    
    private Integer status;
    
    private Integer visible;
    
    private String iconColor;
    
    private String description;
    
    /**
     * 子菜单列表
     */
    private List<MenuVO> children;
}
