package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.dto.MenuDTO;
import com.bcsport.admin.entity.Menu;
import com.bcsport.admin.mapper.MenuMapper;
import com.bcsport.admin.service.MenuService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.MenuVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类 */
@Slf4j
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {
    
    @Override
    public List<MenuVO> getMenuTree() {
        List<MenuVO> allMenus = listAllMenusVO();
        return buildMenuTree(allMenus, "0");
    }
    
    @Override
    public List<MenuVO> getUserMenuTree(String userId) {
        if ("1".equals(userId)) {
            return getMenuTree();
        }
        
        List<Menu> userMenus = baseMapper.selectMenusByUserId(userId);
        List<MenuVO> voList = BeanCopyUtils.copyList(userMenus, MenuVO.class);
        
        return buildMenuTree(voList, "0");
    }
    
    @Override
    public List<MenuVO> getRoleMenuTree(String roleId) {
        // 管理员角色返回所有菜单
        if ("1".equals(roleId)) {
            return getMenuTree();
        }

        // 根据角色ID查询菜单
        List<Menu> roleMenus = baseMapper.selectMenusByRoleId(roleId);
        List<MenuVO> voList = BeanCopyUtils.copyList(roleMenus, MenuVO.class);

        return buildMenuTree(voList, "0");
    }
    
    @Override
    public List<MenuVO> listAllMenusVO() {
        log.info("查询所有菜单列表");
        LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Menu::getStatus, 1);
        queryWrapper.orderByAsc(Menu::getSort);
        List<Menu> menus = baseMapper.selectList(queryWrapper);
        return BeanCopyUtils.copyList(menus, MenuVO.class);
    }
    
    @Override
    public List<MenuVO> listMenusByParentIdVO(String parentId) {
        LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Menu::getParentId, parentId);
        queryWrapper.eq(Menu::getStatus, 1);
        queryWrapper.orderByAsc(Menu::getSort);
        List<Menu> menus = baseMapper.selectList(queryWrapper);
        return BeanCopyUtils.copyList(menus, MenuVO.class);
    }

    @Override
    public MenuVO getMenuVOById(String id) {
        Menu menu = getById(id);
        return BeanCopyUtils.copy(menu, MenuVO.class);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addMenu(MenuDTO menuDTO) {
        Menu menu = BeanCopyUtils.copy(menuDTO, Menu.class);
        if (menu.getId() == null || menu.getId().isEmpty()) {
            menu.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
        }

        // 设置默认值（审计字段由MybatisPlusAutoFillHandler 自动填充）
        menu.setStatus(1);
        menu.setVisible(1);
        return baseMapper.insert(menu) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMenu(MenuDTO menuDTO) {
        Menu menu = BeanCopyUtils.copy(menuDTO, Menu.class);
        // 审计字段由MybatisPlusAutoFillHandler 自动填充
        return baseMapper.updateById(menu) > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMenu(String id) {
        if (hasChildren(id)) {
            throw new RuntimeException("该菜单下有子菜单，无法删除");
        }

        // 使用 MyBatis-Plus 逻辑删除（@TableLogic 自动处理）
        return removeById(id);
    }

    @Override
    public boolean hasChildren(String menuId) {
        LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Menu::getParentId, menuId);
        // 检查所有子菜单（包括禁用的），防止删除后产生孤儿数据
        return baseMapper.selectCount(queryWrapper) > 0;
    }
    
    @Override
    public List<String> getPermissionsByUserId(String userId) {
        if ("1".equals(userId)) {
            List<String> adminPermissions = new ArrayList<>();
            adminPermissions.add("*");
            return adminPermissions;
        }
        return baseMapper.getPermissionsByUserId(userId);
    }

    @Override
    public void testMenuQuery() {
        try {
            List<MenuVO> menus = listAllMenusVO();
            log.info("菜单查询测试成功，记录数: {}", menus.size());
        } catch (Exception e) {
            log.error("菜单查询测试失败", e);
            throw e;
        }
    }
    
    /**
     * 构建菜单树形结构
     */
    private List<MenuVO> buildMenuTree(List<MenuVO> menus, String parentId) {
        List<MenuVO> tree = new ArrayList<>();
        Map<String, List<MenuVO>> grouped = menus.stream()
                .collect(Collectors.groupingBy(MenuVO::getParentId));
        
        List<MenuVO> children = grouped.get(parentId);
        if (children != null) {
            for (MenuVO child : children) {
                List<MenuVO> subChildren = buildMenuTree(menus, child.getId());
                child.setChildren(subChildren);
                tree.add(child);
            }
        }
        
        tree.sort((a, b) -> {
            if (a.getSort() == null || b.getSort() == null) {
                return 0;
            }
            return a.getSort().compareTo(b.getSort());
        });
        
        return tree;
    }
}
