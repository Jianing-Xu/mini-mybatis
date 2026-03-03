package com.xujn.minimybatis.binding;

import com.xujn.minimybatis.session.Configuration;
import com.xujn.minimybatis.session.SqlSession;
import com.xujn.minimybatis.support.ErrorContext;
import com.xujn.minimybatis.support.ExceptionFactory;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of mapper interfaces to proxy factories.
 *
 * <p>Responsibility: validate mapper types once during configuration and reuse
 * the same proxy factory for each session lookup.
 *
 * <p>Thread-safety: safe for concurrent reads after startup. Runtime mutation
 * is not supported.
 */
public class MapperRegistry {

    private final Configuration configuration;
    private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new LinkedHashMap<>();

    public MapperRegistry(Configuration configuration) {
        this.configuration = configuration;
    }

    public <T> void addMapper(Class<T> type) {
        if (!type.isInterface()) {
            throw ExceptionFactory.mappingException(
                    "Only interfaces can be registered as mappers",
                    ErrorContext.create().mapper(type));
        }
        if (knownMappers.containsKey(type)) {
            throw ExceptionFactory.mappingException(
                    "Mapper already registered",
                    ErrorContext.create().mapper(type));
        }
        knownMappers.put(type, new MapperProxyFactory<>(type));
    }

    @SuppressWarnings("unchecked")
    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        MapperProxyFactory<T> factory = (MapperProxyFactory<T>) knownMappers.get(type);
        if (factory == null) {
            throw ExceptionFactory.mappingException(
                    "Mapper not registered",
                    ErrorContext.create().mapper(type));
        }
        return factory.newInstance(sqlSession);
    }

    public boolean hasMapper(Class<?> type) {
        return knownMappers.containsKey(type);
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
