package com.xujn.minispring.integration.mybatis.support;

import com.xujn.minimybatis.session.SqlSession;
import com.xujn.minimybatis.session.SqlSessionFactory;
import com.xujn.minispring.context.annotation.Autowired;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe facade that opens and closes SqlSession per invocation.
 *
 * <p>Responsibility: hide session lifecycle from application code and expose
 * mapper proxies safe to keep as singleton beans.
 *
 * <p>Thread-safety: thread-safe after dependency injection; one proxy instance
 * may be shared across threads because each invocation uses a fresh session.
 */
public class SqlSessionTemplate {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    private final Map<Class<?>, Object> mapperProxyCache = new ConcurrentHashMap<>();

    public <T> T selectOne(String statement) {
        return selectOne(statement, null);
    }

    public <T> T selectOne(String statement, Object parameter) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            return sqlSession.selectOne(statement, parameter);
        }
    }

    public <E> List<E> selectList(String statement) {
        return selectList(statement, null);
    }

    public <E> List<E> selectList(String statement, Object parameter) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            return sqlSession.selectList(statement, parameter);
        }
    }

    public int update(String statement, Object parameter) {
        throw new UnsupportedOperationException("mini-mybatis integration phase 1 does not support update statements");
    }

    @SuppressWarnings("unchecked")
    public <T> T getMapper(Class<T> mapperType) {
        return (T) mapperProxyCache.computeIfAbsent(mapperType, this::createTemplateMapperProxy);
    }

    private <T> T createTemplateMapperProxy(Class<T> mapperType) {
        InvocationHandler handler = new TemplateMapperInvocationHandler<>(mapperType, sqlSessionFactory);
        return mapperType.cast(Proxy.newProxyInstance(
                mapperType.getClassLoader(),
                new Class<?>[]{mapperType},
                handler));
    }

    private static final class TemplateMapperInvocationHandler<T> implements InvocationHandler {

        private final Class<T> mapperType;
        private final SqlSessionFactory sqlSessionFactory;

        private TemplateMapperInvocationHandler(Class<T> mapperType, SqlSessionFactory sqlSessionFactory) {
            this.mapperType = mapperType;
            this.sqlSessionFactory = sqlSessionFactory;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (Object.class.equals(method.getDeclaringClass())) {
                return invokeObjectMethod(proxy, method, args);
            }
            try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
                Object mapper = sqlSession.getMapper(mapperType);
                try {
                    return method.invoke(mapper, args);
                } catch (InvocationTargetException ex) {
                    throw ex.getTargetException();
                }
            }
        }

        private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "toString" -> "SqlSessionTemplateMapperProxy(" + mapperType.getName() + ")";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new IllegalStateException("Unsupported Object method: " + method.getName());
            };
        }
    }
}
