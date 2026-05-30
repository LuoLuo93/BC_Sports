package com.bcsport.admin.controller;

import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.MenuDTO;
import com.bcsport.admin.service.MenuService;
import com.bcsport.admin.util.ShiroSecurityUtils;
import com.bcsport.admin.vo.MenuVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 菜单管理控制器 */
@Slf4j
@RestController
@RequestMapping("/api/menu")
@Api(tags = "菜单管理")
public class MenuController {
    
    @Autowired
    private MenuService menuService;
    
    /**
     * 获取菜单树形结构
     */
    @GetMapping("/tree")
    @ApiOperation("获取菜单树形结构")
    @RequiresPermissions("menu:query")
    public Result<List<MenuVO>> getMenuTree() {
        List<MenuVO> menuTree = menuService.getMenuTree();
        return Result.success(menuTree);
    }
    
    /**
     * 获取用户菜单名称
     */
    @GetMapping("/userTree")
    @ApiOperation("获取用户菜单名称")
    public Result<List<MenuVO>> getUserMenuTree() {
        String userId = ShiroSecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        List<MenuVO> menuTree = menuService.getUserMenuTree(userId);
        return Result.success(menuTree);
    }
    
    /**
     * 根据父菜单ID查询子菜单
     */
    @GetMapping("/children")
    @ApiOperation("根据父菜单ID查询子菜单")
    @RequiresPermissions("menu:query")
    public Result<List<MenuVO>> listMenusByParentId(@RequestParam(defaultValue = "0") String parentId) {
        List<MenuVO> menus = menuService.listMenusByParentIdVO(parentId);
        return Result.success(menus);
    }
    
    /**
     * 根据ID查询菜单
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询菜单")
    @RequiresPermissions("menu:query")
    public Result<MenuVO> getMenuById(@PathVariable String id) {
        MenuVO menuVO = menuService.getMenuVOById(id);
        if (menuVO == null) {
            return Result.notFound("菜单不存在");
        }
        return Result.success(menuVO);
    }
    
    /**
     * 新增菜单
     */
    @PostMapping
    @ApiOperation("新增菜单")
    @RequiresPermissions("menu:add")
    @OperLog(module = "菜单管理", operation = "新增菜单")
    public Result<?> addMenu(@Valid @RequestBody MenuDTO menuDTO) {
        boolean success = menuService.addMenu(menuDTO);
        if (success) {
            return Result.success("新增菜单成功", null);
        }
        return Result.error("新增菜单失败");
    }
    
    /**
     * 更新菜单
     */
    @PutMapping("/{id}")
    @ApiOperation("更新菜单")
    @RequiresPermissions("menu:edit")
    @OperLog(module = "菜单管理", operation = "更新菜单")
    public Result<?> updateMenu(@PathVariable String id, @Valid @RequestBody MenuDTO menuDTO) {
        menuDTO.setId(id);
        boolean success = menuService.updateMenu(menuDTO);
        if (success) {
            return Result.success("更新菜单成功", null);
        }
        return Result.error("更新菜单失败");
    }
    
    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除菜单")
    @RequiresPermissions("menu:delete")
    @OperLog(module = "菜单管理", operation = "删除菜单")
    public Result<?> deleteMenu(@PathVariable String id) {
        boolean success = menuService.deleteMenu(id);
        if (success) {
            return Result.success("删除菜单成功", null);
        }
        return Result.error("删除菜单失败");
    }
}
