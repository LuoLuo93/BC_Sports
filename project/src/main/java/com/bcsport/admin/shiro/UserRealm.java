package com.bcsport.admin.shiro;

import com.bcsport.admin.entity.User;
import com.bcsport.admin.mapper.MenuMapper;
import com.bcsport.admin.mapper.UserRoleMapper;
import com.bcsport.admin.service.AuthCacheService;
import com.bcsport.admin.service.ConfigService;
import com.bcsport.admin.service.UserService;
import com.bcsport.admin.util.BCryptPasswordUtil;
import com.bcsport.admin.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.lang.util.ByteSource;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户Realm - 认证和授权
 */
@Slf4j
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

    @Autowired
    private ConfigService configService;

    /**
     * 权限/角色变更时由 UserServiceImpl 调用：同时失效 Shiro 授权缓存与 Redis 权限缓存，
     * 下次鉴权重新查库。
     */
    public void evictAuthorizationCache(String username) {
        // Shiro 授权缓存（F32 开启后生效）
        getAuthorizationCache().remove(username);
    }

    /**
     * 清空整个授权缓存（角色批量变更/不确定 username 时用）。
     * 角色分配是低频管理操作，全量清空可接受。
     */
    public void clearAllAuthorizationCache() {
        var cache = getAuthorizationCache();
        if (cache != null) {
            cache.clear();
        }
    }

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
     *
     * 支持渐进式迁移：
     * 1. 优先使用 passwordNew 字段（BCrypt）
     * 2. 如果 passwordNew 为空，使用旧的 password 字段（MD5）
     * 3. BCryptCredentialsMatcher 会自动处理两种格式的验证
     * 4. 登录成功后，在 AuthController 中处理密码迁移
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String username = upToken.getUsername();

        // 从数据库查询用户信息
        User user = userService.getByUsername(username);

        if (user == null) {
            throw new UnknownAccountException("用户不存在：" + username);
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new LockedAccountException("账号已被禁用: " + username);
        }

        // 优先使用 BCrypt 密码
        String passwordNew = user.getPasswordNew();
        if (passwordNew != null && !passwordNew.isEmpty() && BCryptPasswordUtil.isBCryptFormat(passwordNew)) {
            // BCrypt 格式，返回给 BCryptCredentialsMatcher 验证
            return new SimpleAuthenticationInfo(username, passwordNew, getName());
        }

        // F34: MD5 限期拒绝——配置 security.md5RejectEnabled=true 后，
        // MD5-only 账号不允许登录（迁移已有 AuthController 登录成功时自动执行，
        // 此开关用于"最后通牒"：尚未登录过的遗留账号在截止后被拒，联系管理员重置）
        if (configService.getBoolean("security.md5RejectEnabled", false)) {
            throw new LockedAccountException("密码策略已升级，请由管理员重置密码后再登录");
        }

        // 回退到旧的 MD5 格式
        String oldPassword = user.getPassword();
        if (oldPassword == null || oldPassword.isEmpty()) {
            throw new IncorrectCredentialsException("用户名或密码错误");
        }

        // 设置盐值，返回给 BCryptCredentialsMatcher 验证
        // Shiro 3.0 ByteSource 从 shiro.util 移到了 shiro.lang.util，用法不变
        ByteSource credentialsSalt = null;
        if (user.getSalt() != null && !user.getSalt().trim().isEmpty()) {
            credentialsSalt = ByteSource.Util.bytes(user.getSalt());
        }

        // 返回 MD5 凭证信息，BCryptCredentialsMatcher 会用 MD5 方式验证
        // 登录成功后，AuthController 会通过检查 passwordNew 字段判断是否需要迁移
        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(
                username,
                oldPassword,
                credentialsSalt,
                getName()
        );

        return info;
    }
}
