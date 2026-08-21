package com.bcsport.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * 上传文件资源映射配置 + SPA 路由转发
 */
@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {

    @Value("${bc.upload.path:E:/work/BC_Sport/uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /images/** 映射到外部上传目录
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadPath + "/");

        // SPA 路由转发：仅"无扩展名的前端路由"（如 /login、/sticker/print）兜底到 index.html。
        // 带扩展名的静态资源（.js/.css/.png...）不存在时必须返回 404：
        // 部署后 hash 变化，浏览器缓存的旧 index.html 会请求已删除的旧 chunk，
        // 若兜底返回 200+HTML，浏览器把 HTML 当 JS 加载直接语法报错、应用假死
        // （表现为"第一次登录点不动，刷新后再登才进"）。
        // api 路径同样不兜底，避免未知接口返回 HTML 干扰前端错误处理。
        // 统一 no-cache（协商缓存，304）：index.html 每次都重验证，杜绝旧入口配新资源。
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.contains(".") || resourcePath.startsWith("api/")) {
                            return null; // 静态资源/接口路径缺失 → 404
                        }
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
