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
 * BI 数仓数据源配置 (Oracle BI_DW @ 192.168.5.177)
 */
@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = "com.bcsport.admin.bidwmapper",
        sqlSessionFactoryRef = "bidwSqlSessionFactory")
public class BidwDataSourceConfig {

    @Bean("bidwDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.bidw")
    public DataSource bidwDataSource() {
        DruidDataSource ds = new DruidDataSource();
        // 显式设置 name, 确保 Druid 监控页能识别此数据源
        ds.setName("bidwDataSource");
        return ds;
    }

    @Bean("bidwSqlSessionFactory")
    public SqlSessionFactory bidwSqlSessionFactory(
            @Qualifier("bidwDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        factory.setMapperLocations(new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/bidw/*.xml"));

        // 不设置 MetaObjectHandler（数仓表无审计字段）
        // 不设置逻辑删除（数仓表无 deleted 字段）
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

    @Bean("bidwSqlSessionTemplate")
    public SqlSessionTemplate bidwSqlSessionTemplate(@Qualifier("bidwSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean("bidwTransactionManager")
    public PlatformTransactionManager bidwTransactionManager(@Qualifier("bidwDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
