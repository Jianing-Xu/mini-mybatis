package com.xujn.minispring.integration.mybatis.support;

import com.xujn.minispring.integration.mybatis.exception.MapperRegistrationException;

/**
 * Generates stable bean names for mapper interfaces.
 *
 * <p>Responsibility: keep mapper bean naming deterministic across scanner runs.
 *
 * <p>Thread-safety: stateless and thread-safe.
 */
public final class MapperBeanNameGenerator {

    private MapperBeanNameGenerator() {
    }

    public static String generateBeanName(Class<?> mapperType) {
        String simpleName = mapperType.getSimpleName();
        if (simpleName.isEmpty()) {
            throw new MapperRegistrationException("Cannot derive mapper bean name from " + mapperType.getName());
        }
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
}
