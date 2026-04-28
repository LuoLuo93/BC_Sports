package com.bcsport.admin.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

/**
 * Knife4j 配置类
 */
@Configuration
@EnableSwagger2
@EnableKnife4j
@Import(BeanValidatorPluginsConfiguration.class)
public class Knife4jConfig {
    
    /**
     * 创建API文档Docket
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 扫描Controller包路径)
                .apis(RequestHandlerSelectors.basePackage("com.bcsport.admin.controller"))
                .paths(PathSelectors.any())
                .build()
                // 配置安全模式，用于Swagger界面的JWT认证
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
    }
    
    /**
     * API信息
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("BC体育后台管理系统 API文档")
                .description("BC体育后台管理系统接口文档，提供用户管理、角色管理、菜单权限等功能接口")
                .contact(new Contact("BC体育技术部", "https://www.bcsport.com", "tech@bcsport.com"))
                .version("1.0.0")
                .build();
    }
    
    /**
     * 配置安全模式（JWT)
     */
    private List<SecurityScheme> securitySchemes() {
        List<SecurityScheme> apiKeys = new ArrayList<>();
        apiKeys.add(new ApiKey("Authorization", "Authorization", "header"));
        return apiKeys;
    }
    
    /**
     * 配置安全上下文)
     */
    private List<SecurityContext> securityContexts() {
        List<SecurityContext> securityContexts = new ArrayList<>();
        securityContexts.add(SecurityContext.builder()
                .securityReferences(defaultAuth())
                .operationSelector(o -> o.requestMappingPattern().startsWith("/api/"))
                .build());
        return securityContexts;
    }
    
    /**
     * 默认的安全引用)
     */
    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        List<SecurityReference> securityReferences = new ArrayList<>();
        securityReferences.add(new SecurityReference("Authorization", authorizationScopes));
        return securityReferences;
    }
}
