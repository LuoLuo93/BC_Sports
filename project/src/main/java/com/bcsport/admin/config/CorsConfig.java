package com.bcsport.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 配置（仅开发环境生效）
 * 用于 Vue SPA 前端（localhost:5173）跨域访问后端 API
 */
@Configuration
@Profile("dev")
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] devOrigins = {
            "http://localhost:5175",
            "http://localhost:5176",
            "http://127.0.0.1:5175",
            "http://127.0.0.1:5176"
        };

        registry.addMapping("/api/**")
                .allowedOrigins(devOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/doLogin")
                .allowedOrigins(devOrigins)
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

        registry.addMapping("/doLogout")
                .allowedOrigins(devOrigins)
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
