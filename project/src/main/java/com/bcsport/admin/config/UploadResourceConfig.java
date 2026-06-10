package com.bcsport.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 上传文件资源映射配置
 * 将 /images/** 映射到外部上传目录，使上传的图片可通过 HTTP 访问
 */
@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {

    @Value("${bc.upload.path:E:/work/BC_Sport/BcSportsDataManageSystem/project/src/main/resources/static/images}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /images/** 映射到外部上传目录（注意末尾的 / 不能省略）
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
