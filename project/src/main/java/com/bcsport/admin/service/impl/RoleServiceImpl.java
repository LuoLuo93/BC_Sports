package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.exception.BusinessException;
import com.bcsport.admin.dto.RoleDTO;
import com.bcsport.admin.dto.RoleQueryDTO;
import com.bcsport.admin.entity.Role;
import com.bcsport.admin.entity.RoleMenu;
import com.bcsport.admin.mapper.RoleMapper;
import com.bcsport.admin.mapper.RoleMenuMapper;
import com.bcsport.admin.service.AuthCacheService;
import com.bcsport.admin.service.RoleService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.util.ShiroSecurityUtils;
import com.bcsport.admin.vo.RoleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 角色服务实现类 */
@Slf4j
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
    
    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Autowired
    private AuthCacheService authCacheService;
    
    @Override
    public PageResult<RoleVO> pageRoles(PageQuery pageQuery, RoleQueryDTO queryRole) {
        Page<Role> page = pageQuery.toPage();
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        
        if (queryRole != null) {
            if (StringUtils.hasText(queryRole.getRoleName())) {
                queryWrapper.like(Role::getRoleName, queryRole.getRoleName());
            }
            if (StringUtils.hasText(queryRole.getRoleCode())) {
                queryWrapper.like(Role::getRoleCode, queryRole.getRoleCode());
            }
            if (queryRole.getStatus() != null) {
                queryWrapper.eq(Role::getStatus, queryRole.getStatus());
            }
        }
        
        queryWrapper.orderByAsc(Role::getSort);
        
        Page<Role> rolePage = baseMapper.selectPage(page, queryWrapper);
        return BeanCopyUtils.copyPage(PageResult.of(rolePage), RoleVO.class);
    }
    
    @Override
    public List<RoleVO> listEnabledRolesVO() {
        List<Role> roles = listEnabledRoles();
        return BeanCopyUtils.copyList(roles, RoleVO.class);
    }
    
    @Override
    public List<Role> listEnabledRoles() {
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Role::getStatus, 1);
        queryWrapper.orderByAsc(Role::getSort);
        return baseMapper.selectList(queryWrapper);
    }
    
    @Override
    public Role getByRoleCode(String roleCode) {
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Role::getRoleCode, roleCode);
        return baseMapper.selectOne(queryWrapper);
    }
    
    @Override
    public RoleVO getRoleVOById(String id) {
        Role role = getById(id);
        return BeanCopyUtils.copy(role, RoleVO.class);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addRole(RoleDTO roleDTO) {
        // 检查角色名称是否已存在
        LambdaQueryWrapper<Role> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(Role::getRoleName, roleDTO.getRoleName());
        if (baseMapper.selectCount(nameWrapper) > 0) {
            throw new BusinessException("角色名称已存在，请更换！");
        }
        
        // 检查角色编码是否已存在
        Role existingRole = getByRoleCode(roleDTO.getRoleCode());
        if (existingRole != null) {
            throw new BusinessException("角色编码已存在");
        }
        
        Role role = BeanCopyUtils.copy(roleDTO, Role.class);
        role.setStatus(1); // 默认启用
        return baseMapper.insert(role) > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(RoleDTO roleDTO) {
        // 检查角色名称是否与其他角色重复
        LambdaQueryWrapper<Role> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(Role::getRoleName, roleDTO.getRoleName())
                   .ne(Role::getId, roleDTO.getId());
        if (baseMapper.selectCount(nameWrapper) > 0) {
            throw new BusinessException("角色名称已存在，请更换其他名称！");
        }

        Role role = BeanCopyUtils.copy(roleDTO, Role.class);
        boolean result = baseMapper.updateById(role) > 0;
        if (result) {
            authCacheService.evictAll();
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRole(String id) {
        boolean result = baseMapper.deleteById(id) > 0;
        if (result) {
            authCacheService.evictAll();
        }
        return result;
    }
    
    @Override
    public List<String> getRolePermissionIds(String roleId) {
        LambdaQueryWrapper<RoleMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RoleMenu::getRoleId, roleId);
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(queryWrapper);
        return roleMenus.stream()
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignPermissions(String roleId, List<String> permissionIds) {
        // 删除原有权限
        LambdaQueryWrapper<RoleMenu> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(RoleMenu::getRoleId, roleId);
        roleMenuMapper.delete(deleteWrapper);

        // 批量插入新权限（避免循环插入的N+1问题）
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<RoleMenu> roleMenuList = permissionIds.stream()
                    .map(menuId -> {
                        RoleMenu roleMenu = new RoleMenu();
                        roleMenu.setId(UUID.randomUUID().toString().replace("-", ""));
                        roleMenu.setRoleId(roleId);
                        roleMenu.setMenuId(menuId);
                        roleMenu.setCreateTime(LocalDateTime.now());
                        roleMenu.setCreateBy(ShiroSecurityUtils.getCurrentUserId());
                        return roleMenu;
                    })
                    .collect(Collectors.toList());

            roleMenuMapper.batchInsert(roleMenuList);
        }
        authCacheService.evictAll();
        return true;
    }
}
