package com.bcsport.admin.shiro;

import com.bcsport.admin.entity.User;
import com.bcsport.admin.mapper.MenuMapper;
import com.bcsport.admin.mapper.UserRoleMapper;
import com.bcsport.admin.service.AuthCacheService;
import com.bcsport.admin.service.UserService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户Realm - 认证和授权
 */
@Component
public class UserRealm extends AuthorizingRealm {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRoleMapper userRoleMapper;
    
    @Autowired
    private MenuMapper menuMapper;

    @Autowired
    private AuthCacheService authCacheService;
    
    /**
     * 授权（获取用户角色和权限）
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String username = (String) principals.getPrimaryPrincipal();
        User user = userService.getByUsername(username);

        // 防止用户已被删除但 session 仍存在的情况
        if (user == null) {
            return new SimpleAuthorizationInfo();
        }

        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        
        // 从数据库查询角色和权限
        if ("admin".equals(username)) {
            authorizationInfo.addRole("admin");
            authorizationInfo.addStringPermission("*");
        } else {
            // 查询用户角色（优先缓存）
            List<String> roleCodes = authCacheService.getRoles(user.getId());
            if (roleCodes == null) {
                roleCodes = userRoleMapper.getRoleCodesByUserId(user.getId());
                authCacheService.putRoles(user.getId(), roleCodes);
            }
            authorizationInfo.addRoles(roleCodes);

            // 查询用户权限标识（优先缓存）
            List<String> permissions = authCacheService.getPermissions(user.getId());
            if (permissions == null) {
                permissions = menuMapper.getPermissionsByUserId(user.getId());
                authCacheService.putPermissions(user.getId(), permissions);
            }
            Set<String> permissionSet = permissions.stream()
                    .filter(p -> p != null && !p.trim().isEmpty())
                    .collect(Collectors.toSet());
            authorizationInfo.setStringPermissions(permissionSet);
        }
        
        return authorizationInfo;
    }
    
    /**
     * 认证（验证用户身份）
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) token;
        String username = usernamePasswordToken.getUsername();
        
        // 从数据库查询用户信息
        User user = userService.getByUsername(username);
        
        if (user == null) {
            throw new UnknownAccountException("用户不存在：" + username);
        }
        
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new LockedAccountException("账号已被禁用: " + username);
        }
        
        // 设置加密盐值
        ByteSource credentialsSalt = null;
        if (user.getSalt() != null && !user.getSalt().trim().isEmpty()) {
            credentialsSalt = ByteSource.Util.bytes(user.getSalt());
        }
        
        // 返回认证信息，Shiro会自动根据ShiroConfig中配置的matcher进行密码校验
        return new SimpleAuthenticationInfo(
                username, 
                user.getPassword(), 
                credentialsSalt, 
                getName()
        );
    }
}
