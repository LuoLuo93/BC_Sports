package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.dto.MenuDTO;
import com.bcsport.admin.entity.Menu;
import com.bcsport.admin.vo.MenuVO;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface MenuService extends IService<Menu> {
    
    /**
     * 获取菜单树形结构
     */
    List<MenuVO> getMenuTree();
    
    /**
     * 根据用户ID获取用户菜单
     */
    List<MenuVO> getUserMenuTree(String userId);
    
    /**
     * 根据角色ID获取角色菜单
     */
    List<MenuVO> getRoleMenuTree(String roleId);
    
    /**
     * 查询所有菜单（平铺）
     */
    List<MenuVO> listAllMenusVO();
    
    /**
     * 根据父菜单ID查询子菜单
     */
    List<MenuVO> listMenusByParentIdVO(String parentId);
    
    /**
     * 根据ID查询菜单VO
     */
    MenuVO getMenuVOById(String id);
    
    /**
     * 新增菜单
     */
    boolean addMenu(MenuDTO menuDTO);
    
    /**
     * 更新菜单
     */
    boolean updateMenu(MenuDTO menuDTO);
    
    /**
     * 删除菜单
     */
    boolean deleteMenu(String id);
    
    /**
     * 检查菜单是否有子菜单
     */
    boolean hasChildren(String menuId);
    
    /**
     * 根据用户ID获取用户权限标识列表
     */
    List<String> getPermissionsByUserId(String userId);

    /**
     * 测试菜单查询，验证SQL是否正确
     */
    void testMenuQuery();
}
