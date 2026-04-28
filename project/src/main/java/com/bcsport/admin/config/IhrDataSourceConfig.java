package com.bcsport.admin.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * IHR模块 SQL Server 数据源配置
 * 独立数据源，连接 BC_SPORTS_IHR 库 */
@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = "com.bcsport.admin.ihrmapper",
        sqlSessionFactoryRef = "ihrSqlSessionFactory")
public class IhrDataSourceConfig {

    @Bean("ihrDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.ihr")
    public DataSource ihrDataSource() {
        return new DruidDataSource();
    }

    @Bean("ihrSqlSessionFactory")
    public SqlSessionFactory ihrSqlSessionFactory(@Qualifier("ihrDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        
        // 1. 设置 Mapper 位置
        factory.setMapperLocations(new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/ihr/*.xml"));

        // 2. MyBatis-Plus 全局配置
        com.baomidou.mybatisplus.core.config.GlobalConfig globalConfig = new com.baomidou.mybatisplus.core.config.GlobalConfig();
        com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig dbConfig = new com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig();
        dbConfig.setIdType(com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID);
        // SQL Server 不需要像 Oracle 那样配置特殊的 DbType，但可以明确指定
        globalConfig.setDbConfig(dbConfig);
        factory.setGlobalConfig(globalConfig);

        // 3. MyBatis 配置
        com.baomidou.mybatisplus.core.MybatisConfiguration configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);
        factory.setConfiguration(configuration);

        return factory.getObject();
    }

    @Bean("ihrSqlSessionTemplate")
    public SqlSessionTemplate ihrSqlSessionTemplate(@Qualifier("ihrSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean("ihrTransactionManager")
    public PlatformTransactionManager ihrTransactionManager(@Qualifier("ihrDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
