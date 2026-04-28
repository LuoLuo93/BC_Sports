package com.bcsport.admin.util;

import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource;

import java.util.UUID;

/**
 * 密码工具类
 */
public class PasswordUtil {
    
    /**
     * 加密算法
     */
    private static final String ALGORITHM_NAME = "md5";
    
    /**
     * 加密次数
     */
    private static final int HASH_ITERATIONS = 2;
    
    /**
     * 生成随机盐)
     * @return 盐值字符串
     */
    public static String generateSalt() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * 加密密码
     * @param password 原始密码
     * @param salt 盐)
     * @return 加密后的密码
     */
    public static String encryptPassword(String password, String salt) {
        SimpleHash hash = new SimpleHash(ALGORITHM_NAME, password, ByteSource.Util.bytes(salt), HASH_ITERATIONS);
        return hash.toHex();
    }
    
    /**
     * 验证密码
     * @param inputPassword 输入的密)
     * @param salt 盐)
     * @param encryptedPassword 数据库中存储的加密密)
     * @return 是否匹配
     */
    public static boolean verifyPassword(String inputPassword, String salt, String encryptedPassword) {
        String encryptedInput = encryptPassword(inputPassword, salt);
        return encryptedInput.equals(encryptedPassword);
    }
}
