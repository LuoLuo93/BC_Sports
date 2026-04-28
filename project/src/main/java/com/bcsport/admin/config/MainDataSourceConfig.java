package com.bcsport.admin.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 主数据源配置 (Oracle)
 */
@Configuration
public class MainDataSourceConfig {

    @Autowired
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    @Autowired
    private MetaObjectHandler mybatisPlusAutoFillHandler;

    @Primary
    @Bean("dataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return new DruidDataSource();
    }

    @Primary
    @Bean("sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        
        // 设置 Mapper 位置
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/*.xml"));

        // MyBatis-Plus 全局配置
        GlobalConfig globalConfig = new GlobalConfig();
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setIdType(com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID);
        dbConfig.setLogicDeleteField("deleted");
        dbConfig.setLogicDeleteValue("1");
        dbConfig.setLogicNotDeleteValue("0");
        globalConfig.setDbConfig(dbConfig);
        globalConfig.setBanner(false);
        globalConfig.setMetaObjectHandler(mybatisPlusAutoFillHandler);
        factory.setGlobalConfig(globalConfig);

        // MyBatis 配置
        com.baomidou.mybatisplus.core.MybatisConfiguration configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);
        // 添加分页插件
        configuration.addInterceptor(mybatisPlusInterceptor);
        factory.setConfiguration(configuration);

        return factory.getObject();
    }

    @Primary
    @Bean("sqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Primary
    @Bean("transactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
