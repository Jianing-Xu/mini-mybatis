package com.xujn.minispring.integration.mybatis.testsupport.phase1.config.missingmapping;

import com.xujn.minimybatis.session.SqlSessionFactory;
import com.xujn.minimybatis.support.CountingDataSource;
import com.xujn.minispring.context.annotation.Bean;
import com.xujn.minispring.context.annotation.Configuration;
import com.xujn.minispring.integration.mybatis.factory.SqlSessionFactoryBean;
import com.xujn.minispring.integration.mybatis.scanner.MapperScannerConfigurer;
import com.xujn.minispring.integration.mybatis.testsupport.phase1.common.IntegrationTestDataSupport;

@Configuration
public class MissingMappingConfig {

    @Bean
    public CountingDataSource dataSource() throws Exception {
        return IntegrationTestDataSupport.countingDataSource("mini_mybatis_integration_missing_mapping");
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(CountingDataSource dataSource) {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new String[]{"mapper/integration-phase1-missing-mapping.xml"});
        return factoryBean.buildSqlSessionFactory();
    }

    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        return new MapperScannerConfigurer("com.xujn.minispring.integration.mybatis.testsupport.phase1.common");
    }
}
