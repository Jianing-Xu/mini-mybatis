package com.xujn.minimybatis.binding;

import com.xujn.minimybatis.session.SqlSession;
import java.lang.reflect.Proxy;

/**
 * Creates JDK dynamic proxies for one mapper interface.
 *
 * <p>Responsibility: isolate proxy construction from registry logic so the
 * runtime session can be swapped per proxy instance.
 *
 * <p>Thread-safety: immutable and thread-safe.
 */
public class MapperProxyFactory<T> {

    private final Class<T> mapperInterface;

    public MapperProxyFactory(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    @SuppressWarnings("unchecked")
    public T newInstance(SqlSession sqlSession) {
        MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface);
        return (T) Proxy.newProxyInstance(
                mapperInterface.getClassLoader(),
                new Class<?>[] {mapperInterface},
                mapperProxy);
    }
}
