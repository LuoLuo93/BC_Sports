package com.bcsport.admin.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码加密工具类
 *
 * 使用 BCrypt 算法进行密码加密，安全性更高
 */
public class BCryptPasswordUtil {

    /**
     * BCrypt 加密器
     * cost factor = 12，安全性与性能的平衡
     */
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    /**
     * 加密密码
     *
     * @param rawPassword 明文密码
     * @return BCrypt 加密后的密码
     */
    public static String encrypt(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 验证密码
     *
     * @param rawPassword     明文密码
     * @param encodedPassword BCrypt 加密后的密码
     * @return true: 密码匹配, false: 密码不匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }

    /**
     * 检查密码是否是 BCrypt 格式
     *
     * BCrypt 格式以 $2a$、$2b$ 或 $2y$ 开头
     *
     * @param password 密码字符串
     * @return true: 是 BCrypt 格式, false: 不是
     */
    public static boolean isBCryptFormat(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
