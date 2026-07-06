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
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * 旧版HK ERP 数据源配置 (SQL Server，连接 HKERP 库)
 * <p>
 * 源项目 interfaceForHK 的直写链路：直接读写 HKERP 库的 Bas_Personnel / Bas_Shop / Bas_DepartMent
 * 等表。与伯俊 ERP 的 HTTP API 链路独立并存，互不影响。
 */
@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = {"com.bcsport.admin.hkerpmapper"},
        sqlSessionFactoryRef = "hkerpSqlSessionFactory")
public class HkErpDataSourceConfig {

    @Bean("hkerpDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.hkerp")
    public DataSource hkerpDataSource() {
        return new DruidDataSource();
    }

    @Bean("hkerpSqlSessionFactory")
    public SqlSessionFactory hkerpSqlSessionFactory(
            @Qualifier("hkerpDataSource") DataSource dataSource,
            MetaObjectHandler metaObjectHandler) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        // 加载 hkerp 的 mapper xml
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/hkerp/*.xml"));

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
        configuration.setJdbcTypeForNull(org.apache.ibatis.type.JdbcType.VARCHAR);
        configuration.setLogImpl(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
        configuration.setDefaultStatementTimeout(300);

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.SQL_SERVER);
        paginationInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        configuration.addInterceptor(interceptor);

        factory.setConfiguration(configuration);

        return factory.getObject();
    }

    @Bean("hkerpSqlSessionTemplate")
    public SqlSessionTemplate hkerpSqlSessionTemplate(@Qualifier("hkerpSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean("hkerpTransactionManager")
    public PlatformTransactionManager hkerpTransactionManager(@Qualifier("hkerpDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
