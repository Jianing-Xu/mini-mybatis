package com.xujn.minispring.integration.mybatis.support;

import com.xujn.minispring.integration.mybatis.exception.MapperRegistrationException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime registry for mapper bean names and mapper interface types.
 *
 * <p>Responsibility: bridge scanner-time mapper metadata to runtime mapper
 * factory creation without relying on generic property injection.
 *
 * <p>Thread-safety: safe for concurrent reads after bootstrap; registration is
 * expected during single-threaded startup only.
 */
public class MapperDefinitionRegistry {

    private final Map<String, Class<?>> mapperTypesByBeanName = new LinkedHashMap<>();
    private final Map<Class<?>, String> beanNamesByMapperType = new LinkedHashMap<>();

    public void register(String beanName, Class<?> mapperType) {
        Class<?> existingType = mapperTypesByBeanName.get(beanName);
        if (existingType != null) {
            throw new MapperRegistrationException("Duplicate mapper beanName '" + beanName +
                    "' for mapper=" + mapperType.getName() + ", existingMapper=" + existingType.getName());
        }
        String existingBeanName = beanNamesByMapperType.get(mapperType);
        if (existingBeanName != null) {
            throw new MapperRegistrationException("Duplicate mapper interface '" + mapperType.getName() +
                    "' already registered with beanName='" + existingBeanName + "'");
        }
        mapperTypesByBeanName.put(beanName, mapperType);
        beanNamesByMapperType.put(mapperType, beanName);
    }

    public Class<?> getMapperType(String beanName) {
        Class<?> mapperType = mapperTypesByBeanName.get(beanName);
        if (mapperType == null) {
            throw new MapperRegistrationException("Mapper beanName not registered: " + beanName);
        }
        return mapperType;
    }

    public boolean containsBeanName(String beanName) {
        return mapperTypesByBeanName.containsKey(beanName);
    }

    public boolean containsMapperType(Class<?> mapperType) {
        return beanNamesByMapperType.containsKey(mapperType);
    }
}
