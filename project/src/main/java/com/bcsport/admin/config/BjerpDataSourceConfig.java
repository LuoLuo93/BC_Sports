package com.bcsport.admin.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.annotation.DbType;
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

/**
 * 伯俊ERP Oracle 数据源配置
 */
@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = "com.bcsport.admin.erpmapper",
        sqlSessionFactoryRef = "bjerpSqlSessionFactory")
public class BjerpDataSourceConfig {

    @Bean("bjerpDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.bjerp")
    public DataSource bjerpDataSource() {
        return new DruidDataSource();
    }

    @Bean("bjerpSqlSessionFactory")
    public SqlSessionFactory bjerpSqlSessionFactory(
            @Qualifier("bjerpDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        factory.setMapperLocations(new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/erp/*.xml"));

        // 不设置 MetaObjectHandler（伯俊表没有审计字段）
        // 不设置逻辑删除（伯俊表没有 deleted 字段）
        com.baomidou.mybatisplus.core.config.GlobalConfig globalConfig = new com.baomidou.mybatisplus.core.config.GlobalConfig();
        globalConfig.setBanner(false);
        factory.setGlobalConfig(globalConfig);

        // MyBatis 配置：分页插件指定 Oracle 方言
        com.baomidou.mybatisplus.core.MybatisConfiguration configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setJdbcTypeForNull(org.apache.ibatis.type.JdbcType.VARCHAR);
        configuration.setLogImpl(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
        configuration.setDefaultStatementTimeout(300);

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.ORACLE);
        paginationInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        configuration.addInterceptor(interceptor);

        factory.setConfiguration(configuration);

        return factory.getObject();
    }

    @Bean("bjerpSqlSessionTemplate")
    public SqlSessionTemplate bjerpSqlSessionTemplate(@Qualifier("bjerpSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean("bjerpTransactionManager")
    public PlatformTransactionManager bjerpTransactionManager(@Qualifier("bjerpDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
