package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.RoleDTO;
import com.bcsport.admin.dto.RoleQueryDTO;
import com.bcsport.admin.entity.Role;
import com.bcsport.admin.vo.RoleVO;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService extends IService<Role> {
    
    /**
     * 分页查询角色列表
     */
    PageResult<RoleVO> pageRoles(PageQuery pageQuery, RoleQueryDTO queryRole);
    
    /**
     * 查询所有启用角色
     */
    List<RoleVO> listEnabledRolesVO();
    
    /**
     * 查询所有启用角色（供内部调用返回实体）
     */
    List<Role> listEnabledRoles();
    
    /**
     * 根据角色编码查询角色
     */
    Role getByRoleCode(String roleCode);
    
    /**
     * 根据ID查询角色VO
     */
    RoleVO getRoleVOById(String id);
    
    /**
     * 新增角色
     */
    boolean addRole(RoleDTO roleDTO);
    
    /**
     * 更新角色
     */
    boolean updateRole(RoleDTO roleDTO);
    
    /**
     * 删除角色
     */
    boolean deleteRole(String id);
    
    /**
     * 获取角色权限ID列表
     */
    List<String> getRolePermissionIds(String roleId);
    
    /**
     * 分配权限
     */
    boolean assignPermissions(String roleId, List<String> permissionIds);
}
