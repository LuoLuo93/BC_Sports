package com.bcsport.admin.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * 密码工具类（旧 MD5 密码回退验证专用，新密码一律用 BCryptPasswordUtil）
 * Shiro 3.0 移除了 ByteSource/Md5Hash/SimpleHash，改用标准 Java MessageDigest 实现，
 * 功能完全等价：MD5 迭代哈希 + 盐值拼接。
 */
public class PasswordUtil {

    private static final int HASH_ITERATIONS = 2;

    public static String generateSalt() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * MD5 迭代哈希：Shiro 3.0 移除了 SimpleHash，用标准 MessageDigest 等价实现。
     * Shiro 的 SimpleHash 默认先将 salt 拼到密码末尾再做 MD5。
     */
    public static String encryptPassword(String password, String salt) {
        String input = password + salt;
        byte[] digest;
        try {
            digest = input.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("MD5");
            for (int i = 0; i < HASH_ITERATIONS; i++) {
                digest = md.digest(digest);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 不可用", e);
        }
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static boolean verifyPassword(String inputPassword, String salt, String encryptedPassword) {
        String encryptedInput = encryptPassword(inputPassword, salt);
        return encryptedInput.equals(encryptedPassword);
    }
}
