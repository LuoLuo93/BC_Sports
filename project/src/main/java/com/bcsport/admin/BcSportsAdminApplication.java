package com.bcsport.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * BC体育后台管理系统启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.bcsport.admin")
@MapperScan(basePackages = "com.bcsport.admin.mapper", sqlSessionFactoryRef = "sqlSessionFactory")
public class BcSportsAdminApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(BcSportsAdminApplication.class, args);
        System.out.println("=================================");
        System.out.println("BC体育后台管理系统启动成功");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("API文档: http://localhost:8080/doc.html");
        System.out.println("=================================");
    }
}
