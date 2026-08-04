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
 * BC_SPORTS 业务表数据源配置 (Oracle BC_SPORTS @ 192.168.5.177)
 */
@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = "com.bcsport.admin.bcspmapper",
        sqlSessionFactoryRef = "bcspSqlSessionFactory")
public class BcSportsDataSourceConfig {

    @Bean("bcspDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.bcsp")
    public DataSource bcspDataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setName("bcspDataSource");
        return ds;
    }

    @Bean("bcspSqlSessionFactory")
    public SqlSessionFactory bcspSqlSessionFactory(
            @Qualifier("bcspDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        factory.setMapperLocations(new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/bcsp/*.xml"));

        // 不设置 MetaObjectHandler（业务表如有审计字段，后续按需开启）
        // 不设置逻辑删除（按表实际结构再定）
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

    @Bean("bcspSqlSessionTemplate")
    public SqlSessionTemplate bcspSqlSessionTemplate(@Qualifier("bcspSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean("bcspTransactionManager")
    public PlatformTransactionManager bcspTransactionManager(@Qualifier("bcspDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
