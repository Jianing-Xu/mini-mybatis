package com.xujn.minispring.integration.mybatis.scanner;

import com.xujn.minispring.beans.factory.config.BeanDefinition;
import com.xujn.minispring.beans.factory.config.BeanDefinitionRegistry;
import com.xujn.minispring.beans.factory.config.BeanFactoryPostProcessor;
import com.xujn.minispring.beans.factory.support.DefaultListableBeanFactory;
import com.xujn.minispring.integration.mybatis.exception.MapperRegistrationException;
import com.xujn.minispring.integration.mybatis.factory.MapperBeanDefinitionFactory;
import com.xujn.minispring.integration.mybatis.factory.MapperFactory;
import com.xujn.minispring.integration.mybatis.support.MapperBeanNameGenerator;
import com.xujn.minispring.integration.mybatis.support.MapperDefinitionRegistry;
import com.xujn.minispring.integration.mybatis.support.SqlSessionTemplate;
import java.util.Arrays;

/**
 * BeanFactoryPostProcessor that scans mapper interfaces and registers mapper bean definitions.
 *
 * <p>Responsibility: turn plain interfaces into container-managed mapper proxy
 * beans without changing the core BeanFactory createBean flow.
 *
 * <p>Thread-safety: bootstrap-only component.
 */
public class MapperScannerConfigurer implements BeanFactoryPostProcessor {

    public static final String MAPPER_DEFINITION_REGISTRY_BEAN_NAME = "mapperDefinitionRegistry";
    public static final String MAPPER_FACTORY_BEAN_NAME = "mapperFactory";
    public static final String SQL_SESSION_TEMPLATE_BEAN_NAME = "sqlSessionTemplate";

    private final String[] basePackages;
    private final MapperScanner mapperScanner = new ClassPathMapperScanner();

    public MapperScannerConfigurer(String... basePackages) {
        this.basePackages = basePackages == null ? new String[0] : Arrays.copyOf(basePackages, basePackages.length);
    }

    @Override
    public void postProcessBeanFactory(BeanDefinitionRegistry registry) {
        MapperDefinitionRegistry mapperDefinitionRegistry = ensureMapperDefinitionRegistry(registry);
        registerInfrastructureBeans(registry);
        for (Class<?> mapperType : mapperScanner.scan(basePackages)) {
            String beanName = MapperBeanNameGenerator.generateBeanName(mapperType);
            if (registry.containsBeanDefinition(beanName) || mapperDefinitionRegistry.containsBeanName(beanName)) {
                throw new MapperRegistrationException("Duplicate mapper bean registration detected: mapperClass="
                        + mapperType.getName() + ", beanName=" + beanName);
            }
            mapperDefinitionRegistry.register(beanName, mapperType);
            BeanDefinition beanDefinition = MapperBeanDefinitionFactory.create(
                    mapperType,
                    beanName,
                    MAPPER_FACTORY_BEAN_NAME,
                    "MapperScan:" + mapperType.getName());
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private MapperDefinitionRegistry ensureMapperDefinitionRegistry(BeanDefinitionRegistry registry) {
        if (!(registry instanceof DefaultListableBeanFactory beanFactory)) {
            throw new MapperRegistrationException("MapperScannerConfigurer requires DefaultListableBeanFactory");
        }
        if (!registry.containsBeanDefinition(MAPPER_DEFINITION_REGISTRY_BEAN_NAME)) {
            BeanDefinition beanDefinition = new BeanDefinition(
                    MapperDefinitionRegistry.class,
                    MAPPER_DEFINITION_REGISTRY_BEAN_NAME);
            beanDefinition.setSource("MapperScannerConfigurer:infrastructure");
            registry.registerBeanDefinition(MAPPER_DEFINITION_REGISTRY_BEAN_NAME, beanDefinition);
        }
        if (beanFactory.containsSingleton(MAPPER_DEFINITION_REGISTRY_BEAN_NAME)) {
            return beanFactory.getBean(MAPPER_DEFINITION_REGISTRY_BEAN_NAME, MapperDefinitionRegistry.class);
        }
        MapperDefinitionRegistry mapperDefinitionRegistry = new MapperDefinitionRegistry();
        beanFactory.registerSingleton(MAPPER_DEFINITION_REGISTRY_BEAN_NAME, mapperDefinitionRegistry);
        return mapperDefinitionRegistry;
    }

    private void registerInfrastructureBeans(BeanDefinitionRegistry registry) {
        registerInfrastructureBean(registry, SQL_SESSION_TEMPLATE_BEAN_NAME, SqlSessionTemplate.class);
        registerInfrastructureBean(registry, MAPPER_FACTORY_BEAN_NAME, MapperFactory.class);
    }

    private void registerInfrastructureBean(BeanDefinitionRegistry registry, String beanName, Class<?> beanClass) {
        if (registry.containsBeanDefinition(beanName)) {
            return;
        }
        BeanDefinition beanDefinition = new BeanDefinition(beanClass, beanName);
        beanDefinition.setSource("MapperScannerConfigurer:infrastructure");
        registry.registerBeanDefinition(beanName, beanDefinition);
    }
}
