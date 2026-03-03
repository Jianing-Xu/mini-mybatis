package com.xujn.minimybatis.reflection;

import java.lang.reflect.Constructor;

/**
 * Creates Java objects for result mapping.
 *
 * <p>Responsibility: centralize reflective construction so handler code keeps a
 * single instantiation path and error behavior.
 *
 * <p>Thread-safety: stateless and thread-safe.
 */
public class ObjectFactory {

    public <T> T create(Class<T> type) throws ReflectiveOperationException {
        Constructor<T> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
