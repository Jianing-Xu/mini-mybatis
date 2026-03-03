package com.xujn.minispring.integration.mybatis.scanner;

import java.util.Set;

/**
 * Scans classpath packages for mapper interfaces.
 *
 * <p>Responsibility: isolate mapper candidate discovery from BeanDefinition
 * registration details.
 *
 * <p>Thread-safety: implementations are intended for bootstrap-time use.
 */
public interface MapperScanner {

    Set<Class<?>> scan(String... basePackages);

    boolean isCandidateComponent(Class<?> beanClass);
}
