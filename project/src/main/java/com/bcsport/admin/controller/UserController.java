package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.PasswordDTO;
import com.bcsport.admin.dto.UserDTO;
import com.bcsport.admin.dto.UserQueryDTO;
import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.entity.User;
import com.bcsport.admin.util.ShiroSecurityUtils;
import com.bcsport.admin.vo.UserVO;
import com.bcsport.admin.service.UserService;
import com.bcsport.admin.service.ConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@Api(tags = "用户管理")
public class UserController {
    
    @Autowired
    private UserService userService;

    @Autowired
    private ConfigService configService;

    private String validatePassword(String password) {
        int minLen = configService.getInt("security.passwordMinLength", 6);
        if (password == null || password.length() < minLen) {
            return "密码长度不能少于 " + minLen + " 个字符";
        }
        if (configService.getBoolean("security.passwordRequireUpper", false) && !password.matches(".*[A-Z].*")) {
            return "密码必须包含至少一个大写字母";
        }
        if (configService.getBoolean("security.passwordRequireNumber", true) && !password.matches(".*\\d.*")) {
            return "密码必须包含至少一个数字";
        }
        return null;
    }
    
    /**
     * 获取当前登录用户的个人信息
     */
    @GetMapping("/profile")
    @ApiOperation("获取当前登录用户的个人信息")
    public Result<UserVO> getProfile() {
        String currentUserId = ShiroSecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error("未登录");
        }
        UserVO userVO = userService.getUserVOById(currentUserId);
        if (userVO == null) {
            return Result.notFound("用户不存在");
        }
        return Result.success(userVO);
    }

    /**
     * 更新当前登录用户的个人信息
     */
    @PutMapping("/profile")
    @ApiOperation("更新当前登录用户的个人信息")
    @OperLog(module = "用户管理", operation = "更新个人信息")
    public Result<?> updateProfile(@RequestBody UserDTO userDTO) {
        String currentUserId = ShiroSecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error("未登录");
        }
        userDTO.setId(currentUserId);
        boolean success = userService.updateUserProfile(currentUserId, userDTO);
        if (success) {
            return Result.success("更新成功", null);
        }
        return Result.error("更新失败");
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询用户列表")
    @RequiresPermissions("user:query")
    public Result<PageResult<UserVO>> pageUsers(PageQuery pageQuery, UserQueryDTO queryUser) {
        PageResult<UserVO> pageResult = userService.pageUsers(pageQuery, queryUser);
        return Result.success(pageResult);
    }
    
    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询用户")
    @RequiresPermissions("user:query")
    public Result<UserVO> getUserById(@PathVariable String id) {
        UserVO userVO = userService.getUserVOById(id);
        if (userVO == null) {
            return Result.notFound("用户不存在");
        }
        return Result.success(userVO);
    }
    
    /**
     * 新增用户
     */
    @PostMapping
    @ApiOperation("新增用户")
    @RequiresPermissions("user:add")
    @OperLog(module = "用户管理", operation = "新增用户")
    public Result<?> addUser(@Validated(User.Create.class) @RequestBody UserDTO userDTO) {
        boolean success = userService.addUserWithRoles(userDTO, userDTO.getRoleIds());
        if (success) {
            return Result.success("新增用户成功", null);
        }
        return Result.error("新增用户失败");
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @ApiOperation("更新用户")
    @RequiresPermissions("user:edit")
    @OperLog(module = "用户管理", operation = "更新用户")
    public Result<?> updateUser(@PathVariable String id, @Validated(User.Update.class) @RequestBody UserDTO userDTO) {
        userDTO.setId(id);
        boolean success = userService.updateUserWithRoles(userDTO, userDTO.getRoleIds());
        if (success) {
            return Result.success("更新用户成功", null);
        }
        return Result.error("更新用户失败");
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除用户")
    @RequiresPermissions("user:delete")
    @OperLog(module = "用户管理", operation = "删除用户")
    public Result<?> deleteUser(@PathVariable String id) {
        boolean success = userService.deleteUser(id);
        if (success) {
            return Result.success("删除用户成功", null);
        }
        return Result.error("删除用户失败");
    }
    
    /**
     * 重置密码
     */
    @PutMapping("/{id}/resetPassword")
    @ApiOperation("重置密码")
    @RequiresPermissions("user:resetPassword")
    @OperLog(module = "用户管理", operation = "重置密码")
    public Result<?> resetPassword(@PathVariable String id, @Validated @RequestBody PasswordDTO dto) {
        String error = validatePassword(dto.getNewPassword());
        if (error != null) return Result.paramError(error);
        boolean success = userService.resetPassword(id, dto.getNewPassword());
        if (success) {
            return Result.success("重置密码成功", null);
        }
        return Result.error("重置密码失败");
    }

    /**
     * 修改密码
     */
    @PutMapping("/{id}/changePassword")
    @ApiOperation("修改密码")
    @OperLog(module = "用户管理", operation = "修改密码", saveParams = false)
    public Result<?> changePassword(@PathVariable String id, @Validated @RequestBody PasswordDTO dto) {
        String error = validatePassword(dto.getNewPassword());
        if (error != null) return Result.paramError(error);
        // 检查权限：只能修改自己的密码，或者有user:resetPassword权限的管理员可以修改任意密码
        String currentUserId = ShiroSecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error("未登录");
        }
        boolean isAdmin = ShiroSecurityUtils.hasPermission("user:resetPassword");
        if (!id.equals(currentUserId) && !isAdmin) {
            return Result.error("无权修改该用户的密码");
        }
        // 管理员修改他人密码时，跳过旧密码验证，直接重置
        if (isAdmin && !id.equals(currentUserId)) {
            boolean success = userService.resetPassword(id, dto.getNewPassword());
            return success ? Result.success("修改密码成功", null) : Result.error("修改密码失败");
        }
        // 普通用户修改自己的密码，需要验证旧密码
        boolean success = userService.changePassword(id, dto.getOldPassword(), dto.getNewPassword());
        if (success) {
            return Result.success("修改密码成功", null);
        }
        return Result.error("修改密码失败");
    }
    
    /**
     * 获取用户角色ID列表
     */
    @GetMapping("/{id}/roles")
    @ApiOperation("获取用户角色ID列表")
    @RequiresPermissions("user:role")
    public Result<List<String>> getUserRoles(@PathVariable String id) {
        List<String> roleIds = userService.getUserRoleIds(id);
        return Result.success(roleIds);
    }
    
    /**
     * 分配角色
     */
    @PutMapping("/{id}/roles")
    @ApiOperation("分配角色")
    @RequiresPermissions("user:assignRole")
    @OperLog(module = "用户管理", operation = "分配角色")
    public Result<?> assignRoles(@PathVariable String id, @RequestBody List<String> roleIds) {
        boolean success = userService.assignRoles(id, roleIds);
        if (success) {
            return Result.success("分配角色成功", null);
        }
        return Result.error("分配角色失败");
    }
}
