package com.xujn.minispring.integration.mybatis.scanner;

import com.xujn.minispring.integration.mybatis.exception.MapperRegistrationException;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * File-based classpath scanner for mapper interfaces.
 *
 * <p>Responsibility: find interfaces under configured base packages without
 * requiring mapper annotations in phase 1.
 *
 * <p>Thread-safety: bootstrap-only component.
 */
public class ClassPathMapperScanner implements MapperScanner {

    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Override
    public Set<Class<?>> scan(String... basePackages) {
        Set<Class<?>> mapperTypes = new LinkedHashSet<>();
        if (basePackages == null) {
            return mapperTypes;
        }
        for (String basePackage : basePackages) {
            if (basePackage == null || basePackage.isBlank()) {
                continue;
            }
            scanPackage(basePackage.trim(), mapperTypes);
        }
        return mapperTypes;
    }

    @Override
    public boolean isCandidateComponent(Class<?> beanClass) {
        int modifiers = beanClass.getModifiers();
        return beanClass.isInterface()
                && !beanClass.isAnnotation()
                && !beanClass.isEnum()
                && !java.lang.reflect.Modifier.isPrivate(modifiers)
                && !beanClass.isMemberClass();
    }

    private void scanPackage(String basePackage, Set<Class<?>> mapperTypes) {
        String packagePath = basePackage.replace('.', '/');
        try {
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (!"file".equals(resource.getProtocol())) {
                    continue;
                }
                File root = new File(decode(resource.getFile()));
                scanDirectory(basePackage, root, mapperTypes);
            }
        } catch (Exception ex) {
            throw new MapperRegistrationException("Failed to scan mapper package '" + basePackage + "'", ex);
        }
    }

    private void scanDirectory(String currentPackage, File directory, Set<Class<?>> mapperTypes)
            throws ClassNotFoundException {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        Arrays.sort(files, java.util.Comparator.comparing(File::getName));
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(currentPackage + "." + file.getName(), file, mapperTypes);
                continue;
            }
            if (!file.getName().endsWith(".class") || file.getName().contains("$")) {
                continue;
            }
            String simpleClassName = file.getName().substring(0, file.getName().length() - 6);
            Class<?> mapperType = Class.forName(currentPackage + "." + simpleClassName, false, classLoader);
            if (isCandidateComponent(mapperType)) {
                mapperTypes.add(mapperType);
            }
        }
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            throw new MapperRegistrationException("Failed to decode mapper resource path '" + value + "'", ex);
        }
    }
}
