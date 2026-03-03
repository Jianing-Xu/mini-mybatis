package com.xujn.minispring.integration.mybatis.factory;

import com.xujn.minispring.context.annotation.Autowired;
import com.xujn.minispring.integration.mybatis.support.MapperDefinitionRegistry;
import com.xujn.minispring.integration.mybatis.support.SqlSessionTemplate;

/**
 * Factory bean equivalent for mapper proxy beans under current mini-spring SPI.
 *
 * <p>Responsibility: resolve mapper metadata by bean name and delegate proxy
 * creation to SqlSessionTemplate.
 *
 * <p>Thread-safety: thread-safe after dependency injection completes.
 */
public class MapperFactory {

    @Autowired
    private MapperDefinitionRegistry mapperDefinitionRegistry;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    public Object getMapper(String beanName) {
        Class<?> mapperType = mapperDefinitionRegistry.getMapperType(beanName);
        return sqlSessionTemplate.getMapper(mapperType);
    }
}
