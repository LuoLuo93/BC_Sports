package com.bcsport.admin.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

/**
 * QZ Tray 签名服务
 * 提供证书和签名 API，配合前端 qz-tray.js 实现静默打印
 */
@Slf4j
@RestController
@RequestMapping("/api/qz")
public class QzSignController {

    private static final String KEYSTORE_PATH = "certs/qz-private.p12";
    private static final String CERT_PATH = "certs/override.crt";
    private static final String KEYSTORE_PASSWORD = "changeit";
    private static final String KEY_ALIAS = "qz";

    private PrivateKey privateKey;
    private String certificatePem;

    /** 启动时加载私钥和证书，避免每次请求重复读取 */
    @PostConstruct
    public void init() {
        try {
            // 加载私钥
            ClassPathResource ksResource = new ClassPathResource(KEYSTORE_PATH);
            try (InputStream is = ksResource.getInputStream()) {
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(is, KEYSTORE_PASSWORD.toCharArray());
                privateKey = (PrivateKey) ks.getKey(KEY_ALIAS, KEYSTORE_PASSWORD.toCharArray());
            }
            // 加载证书
            ClassPathResource certResource = new ClassPathResource(CERT_PATH);
            try (InputStream is = certResource.getInputStream()) {
                certificatePem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            log.info("QZ Tray 签名服务初始化成功");
        } catch (Exception e) {
            log.error("QZ Tray 签名服务初始化失败", e);
        }
    }

    /**
     * 获取证书内容（PEM 格式）
     * 前端 qz.security.setCertificatePromise() 调用此接口
     */
    @GetMapping(value = "/certificate", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getCertificate() {
        return certificatePem;
    }

    /**
     * 对数据进行签名
     * 前端 qz.security.setSignaturePromise() 调用此接口
     * @param data 待签名的数据（Base64 编码的 SHA-256 哈希）
     * @return Base64 编码的签名
     */
    @PostMapping(value = "/sign", produces = MediaType.TEXT_PLAIN_VALUE)
    public String sign(@RequestBody String data) {
        if (privateKey == null) {
            log.error("QZ Tray 私钥未加载，无法签名");
            return "";
        }
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            byte[] dataBytes = Base64.getDecoder().decode(data.trim());
            signer.update(dataBytes);
            byte[] signature = signer.sign();
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            log.error("QZ Tray 签名失败", e);
            return "";
        }
    }
}
