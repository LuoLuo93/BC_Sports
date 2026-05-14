package com.bcsport.admin.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * IHR模块 + 云盯客流 SQL Server 数据源配置
 * 统一数据源，连接 BC_SPORTS_IHR 库 */
@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = {"com.bcsport.admin.ihrmapper", "com.bcsport.admin.ydklmapper"},
        sqlSessionFactoryRef = "ihrSqlSessionFactory")
public class IhrDataSourceConfig {

    @Bean("ihrDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.ihr")
    public DataSource ihrDataSource() {
        return new DruidDataSource();
    }

    @Bean("ihrSqlSessionFactory")
    public SqlSessionFactory ihrSqlSessionFactory(
            @Qualifier("ihrDataSource") DataSource dataSource,
            MetaObjectHandler metaObjectHandler) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        // 合并 ihr 和 ydkl 的 mapper xml
        List<org.springframework.core.io.Resource> mapperResources = new ArrayList<>();
        mapperResources.addAll(Arrays.asList(
                new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                        .getResources("classpath*:mapper/ihr/*.xml")));
        mapperResources.addAll(Arrays.asList(
                new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                        .getResources("classpath*:mapper/ydkl/*.xml")));
        factory.setMapperLocations(mapperResources.toArray(new org.springframework.core.io.Resource[0]));

        // 全局配置：逻辑删除 + ID策略 + 审计字段自动填充
        com.baomidou.mybatisplus.core.config.GlobalConfig globalConfig = new com.baomidou.mybatisplus.core.config.GlobalConfig();
        com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig dbConfig = new com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig();
        dbConfig.setIdType(com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID);
        dbConfig.setLogicDeleteField("deleted");
        dbConfig.setLogicDeleteValue("1");
        dbConfig.setLogicNotDeleteValue("0");
        globalConfig.setDbConfig(dbConfig);
        globalConfig.setMetaObjectHandler(metaObjectHandler);
        factory.setGlobalConfig(globalConfig);

        // MyBatis 配置：分页插件指定 SQL Server 方言
        com.baomidou.mybatisplus.core.MybatisConfiguration configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
        configuration.setDefaultStatementTimeout(300);

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQL_SERVER));
        configuration.addInterceptor(interceptor);

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
