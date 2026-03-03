package com.xujn.minispring.integration.mybatis.factory;

import com.xujn.minispring.beans.factory.config.BeanDefinition;

/**
 * Builds BeanDefinitions for mapper proxy beans backed by MapperFactory.
 *
 * <p>Responsibility: keep mapper BeanDefinition generation consistent across
 * scanner executions.
 *
 * <p>Thread-safety: stateless and thread-safe.
 */
public final class MapperBeanDefinitionFactory {

    private MapperBeanDefinitionFactory() {
    }

    public static BeanDefinition create(Class<?> mapperType, String beanName, String mapperFactoryBeanName, String source) {
        BeanDefinition beanDefinition = new BeanDefinition(mapperType, beanName);
        beanDefinition.setFactoryBeanName(mapperFactoryBeanName);
        beanDefinition.setFactoryMethodName("getMapper");
        beanDefinition.setFactoryMethodParameterTypes(new Class<?>[]{String.class});
        beanDefinition.setFactoryMethodArguments(new Object[]{beanName});
        beanDefinition.setSource(source);
        return beanDefinition;
    }
}
