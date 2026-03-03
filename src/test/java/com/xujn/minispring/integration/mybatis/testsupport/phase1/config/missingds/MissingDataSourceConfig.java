package com.xujn.minispring.integration.mybatis.testsupport.phase1.config.missingds;

import com.xujn.minimybatis.session.SqlSessionFactory;
import com.xujn.minispring.context.annotation.Bean;
import com.xujn.minispring.context.annotation.Configuration;
import com.xujn.minispring.integration.mybatis.factory.SqlSessionFactoryBean;
import com.xujn.minispring.integration.mybatis.scanner.MapperScannerConfigurer;

@Configuration
public class MissingDataSourceConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory() {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setMapperLocations(new String[]{"mapper/integration-phase1-test-mapper.xml"});
        return factoryBean.buildSqlSessionFactory();
    }

    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        return new MapperScannerConfigurer("com.xujn.minispring.integration.mybatis.testsupport.phase1.common");
    }
}
