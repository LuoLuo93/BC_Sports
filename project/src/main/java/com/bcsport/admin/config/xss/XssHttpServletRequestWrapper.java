package com.bcsport.admin.config.xss;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * XSS 请求包装器
 *
 * 对所有请求参数和请求体进行 XSS 清洗
 * 注意：multipart/form-data 请求不缓存请求体，避免破坏文件上传
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /** 缓存的请求体（multipart 请求为 null） */
    private byte[] cachedBody;

    /** 是否为 multipart 请求 */
    private final boolean isMultipart;

    public XssHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        String contentType = request.getContentType();
        this.isMultipart = contentType != null && contentType.startsWith("multipart/");
        if (!isMultipart) {
            // 仅对非 multipart 请求缓存请求体
            this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // multipart 请求直接返回原始流，由 Spring 解析
        if (isMultipart) {
            return super.getInputStream();
        }
        // 对非 multipart 请求体进行 XSS 清洗
        String body = new String(cachedBody, StandardCharsets.UTF_8);
        String cleanedBody = XssCleaner.clean(body);

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cleanedBody.getBytes(StandardCharsets.UTF_8));

        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return value != null ? XssCleaner.clean(value) : null;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] cleanedValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleanedValues[i] = XssCleaner.clean(values[i]);
        }
        return cleanedValues;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return value != null ? XssCleaner.clean(value) : null;
    }
}
