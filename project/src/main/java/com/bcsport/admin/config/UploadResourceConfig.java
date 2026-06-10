package com.bcsport.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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

        // SPA 路由转发：非 API、非静态资源的请求都转发到 index.html
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    public Resource getResource(String location, Resource resource) throws IOException {
                        Resource candidate = super.getResource(location, resource);
                        // 如果找到静态文件（.js/.css/.html 等）直接返回
                        if (candidate != null && candidate.exists() && candidate.isReadable()) {
                            return candidate;
                        }
                        // 否则转发到 index.html，由 Vue Router 处理前端路由
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
