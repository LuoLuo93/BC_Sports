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
 * 企业微信模块 SQL Server 数据源配置
 * 独立数据源，连接 BC_SPORTS_QYWX 库
 */
@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = "com.bcsport.admin.qywxmapper",
        sqlSessionFactoryRef = "qywxSqlSessionFactory")
public class QywxDataSourceConfig {

    @Bean("qywxDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.qywx")
    public DataSource qywxDataSource() {
        return new DruidDataSource();
    }

    @Bean("qywxSqlSessionFactory")
    public SqlSessionFactory qywxSqlSessionFactory(@Qualifier("qywxDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        factory.setMapperLocations(new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/qywx/*.xml"));

        com.baomidou.mybatisplus.core.config.GlobalConfig globalConfig = new com.baomidou.mybatisplus.core.config.GlobalConfig();
        com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig dbConfig = new com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig();
        dbConfig.setIdType(com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID);
        globalConfig.setDbConfig(dbConfig);
        factory.setGlobalConfig(globalConfig);

        com.baomidou.mybatisplus.core.MybatisConfiguration configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);
        factory.setConfiguration(configuration);

        return factory.getObject();
    }

    @Bean("qywxSqlSessionTemplate")
    public SqlSessionTemplate qywxSqlSessionTemplate(@Qualifier("qywxSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean("qywxTransactionManager")
    public PlatformTransactionManager qywxTransactionManager(@Qualifier("qywxDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
