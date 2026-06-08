package com.bcsport.admin.shiro;

import com.bcsport.admin.util.BCryptPasswordUtil;
import com.bcsport.admin.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SaltedAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.util.ByteSource;

/**
 * BCrypt 密码匹配器
 *
 * 替代 Shiro 默认的 HashedCredentialsMatcher，使用 BCrypt 算法
 * 支持渐进式迁移：优先使用 BCrypt，回退到旧的 MD5 验证
 */
@Slf4j
public class BCryptCredentialsMatcher implements CredentialsMatcher {

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String submittedPassword = new String(upToken.getPassword());
        String storedPassword = (String) info.getCredentials();

        // 检查存储的密码是否是 BCrypt 格式
        if (BCryptPasswordUtil.isBCryptFormat(storedPassword)) {
            // 使用 BCrypt 验证
            boolean matches = BCryptPasswordUtil.matches(submittedPassword, storedPassword);
            if (!matches) {
                log.debug("BCrypt 密码验证失败");
            }
            return matches;
        }

        // 如果不是 BCrypt 格式，使用旧的 MD5 方式验证
        log.debug("检测到旧的 MD5 密码格式，使用 MD5 验证");

        // 获取盐值
        ByteSource saltBytes = null;
        if (info instanceof SaltedAuthenticationInfo saltedInfo) {
            saltBytes = saltedInfo.getCredentialsSalt();
        }
        String salt = null;
        if (saltBytes != null) {
            salt = new String(saltBytes.getBytes());
        }

        // 使用 PasswordUtil 验证 MD5 密码
        boolean matches = PasswordUtil.verifyPassword(submittedPassword, salt, storedPassword);

        if (matches) {
            log.info("MD5 密码验证通过，标记需要迁移到 BCrypt");
            // 在 AuthenticationInfo 中标记需要迁移
            // UserRealm 会在后续处理中检查这个标记
        }

        return matches;
    }
}
