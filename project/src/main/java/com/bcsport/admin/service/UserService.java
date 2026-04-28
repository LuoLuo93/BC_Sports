package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.UserDTO;
import com.bcsport.admin.dto.UserQueryDTO;
import com.bcsport.admin.entity.User;
import com.bcsport.admin.vo.UserVO;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {
    
    /**
     * 分页查询用户列表
     */
    PageResult<UserVO> pageUsers(PageQuery pageQuery, UserQueryDTO queryUser);
    
    /**
     * 根据ID查询用户并转换为VO
     */
    UserVO getUserVOById(String id);
    
    /**
     * 根据用户名查询用)
     */
    User getByUsername(String username);
    
    /**
     * 新增用户
     */
    boolean addUser(User user);
    
    /**
     * 更新用户
     */
    boolean updateUser(User user);
    
    /**
     * 删除用户（逻辑删除完
     */
    boolean deleteUser(String id);
    
    /**
     * 重置密码
     */
    boolean resetPassword(String id, String newPassword);
    
    /**
     * 修改密码
     */
    boolean changePassword(String id, String oldPassword, String newPassword);
    
    /**
     * 获取用户角色ID列表
     */
    List<String> getUserRoleIds(String userId);
    
    /**
     * 分配角色
     */
    boolean assignRoles(String userId, List<String> roleIds);
    
    /**
     * 新增用户并分配角)
     */
    boolean addUserWithRoles(UserDTO userDTO, List<String> roleIds);
    
    /**
     * 更新用户并分配角)
     */
    boolean updateUserWithRoles(UserDTO userDTO, List<String> roleIds);
    
    /**
     * 测试用户角色分配功能
     */
    void testUserRoleAssignment();
}
