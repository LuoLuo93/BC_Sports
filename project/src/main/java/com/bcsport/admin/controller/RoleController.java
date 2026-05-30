package com.bcsport.admin.controller;

import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.RoleDTO;
import com.bcsport.admin.dto.RoleQueryDTO;
import com.bcsport.admin.service.RoleService;
import com.bcsport.admin.vo.RoleVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 角色管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/role")
@Api(tags = "角色管理")
public class RoleController {
    
    @Autowired
    private RoleService roleService;
    
    /**
     * 分页查询角色列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询角色列表")
    @RequiresPermissions("role:query")
    public Result<PageResult<RoleVO>> pageRoles(PageQuery pageQuery, RoleQueryDTO queryRole) {
        PageResult<RoleVO> pageResult = roleService.pageRoles(pageQuery, queryRole);
        return Result.success(pageResult);
    }
    
    /**
     * 查询所有启用角色
     */
    @GetMapping("/list")
    @ApiOperation("查询所有启用角色")
    public Result<List<RoleVO>> listEnabledRoles() {
        List<RoleVO> roles = roleService.listEnabledRolesVO();
        return Result.success(roles);
    }
    
    /**
     * 根据ID查询角色
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询角色")
    @RequiresPermissions("role:query")
    public Result<RoleVO> getRoleById(@PathVariable String id) {
        RoleVO roleVO = roleService.getRoleVOById(id);
        if (roleVO == null) {
            return Result.notFound("角色不存在");
        }
        return Result.success(roleVO);
    }
    
    /**
     * 新增角色
     */
    @PostMapping
    @ApiOperation("新增角色")
    @RequiresPermissions("role:add")
    @OperLog(module = "角色管理", operation = "新增角色")
    public Result<?> addRole(@Valid @RequestBody RoleDTO roleDTO) {
        boolean success = roleService.addRole(roleDTO);
        if (success) {
            return Result.success("新增角色成功", null);
        }
        return Result.error("新增角色失败");
    }
    
    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @ApiOperation("更新角色")
    @RequiresPermissions("role:edit")
    @OperLog(module = "角色管理", operation = "更新角色")
    public Result<?> updateRole(@PathVariable String id, @Valid @RequestBody RoleDTO roleDTO) {
        roleDTO.setId(id);
        boolean success = roleService.updateRole(roleDTO);
        if (success) {
            return Result.success("更新角色成功", null);
        }
        return Result.error("更新角色失败");
    }
    
    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除角色")
    @RequiresPermissions("role:delete")
    @OperLog(module = "角色管理", operation = "删除角色")
    public Result<?> deleteRole(@PathVariable String id) {
        boolean success = roleService.deleteRole(id);
        if (success) {
            return Result.success("删除角色成功", null);
        }
        return Result.error("删除角色失败");
    }
    
    /**
     * 获取角色权限ID列表
     */
    @GetMapping("/{id}/permissions")
    @ApiOperation("获取角色权限ID列表")
    @RequiresPermissions("role:permission")
    public Result<List<String>> getRolePermissions(@PathVariable String id) {
        List<String> permissionIds = roleService.getRolePermissionIds(id);
        return Result.success(permissionIds);
    }
    
    /**
     * 分配权限
     */
    @PutMapping("/{id}/permissions")
    @ApiOperation("分配权限")
    @RequiresPermissions("role:assignPermission")
    @OperLog(module = "角色管理", operation = "分配权限")
    public Result<?> assignPermissions(@PathVariable String id, @RequestBody List<String> permissionIds) {
        boolean success = roleService.assignPermissions(id, permissionIds);
        if (success) {
            return Result.success("分配权限成功", null);
        }
        return Result.error("分配权限失败");
    }
}
