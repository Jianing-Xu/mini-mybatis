package com.xujn.minimybatis.binding;

import com.xujn.minimybatis.support.ErrorContext;
import com.xujn.minimybatis.support.ExceptionFactory;
import com.xujn.minimybatis.session.SqlSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * JDK dynamic proxy handler for mapper interfaces.
 *
 * <p>Responsibility: translate mapper method calls into statement ids and
 * delegate execution to {@link SqlSession}.
 *
 * <p>Thread-safety: thread-safe only to the extent of the underlying
 * {@link SqlSession}; the proxy should not be shared across threads.
 */
public class MapperProxy<T> implements InvocationHandler {

    private final SqlSession sqlSession;
    private final Class<T> mapperInterface;

    public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface) {
        this.sqlSession = sqlSession;
        this.mapperInterface = mapperInterface;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        }
        if (method.isDefault()) {
            throw ExceptionFactory.mappingException(
                    "Default interface methods are not supported in phase 1",
                    ErrorContext.create().mapper(mapperInterface));
        }

        String statementId = mapperInterface.getName() + "." + method.getName();
        Object parameter = extractParameter(method, args, statementId);

        // Phase 1 keeps the proxy contract minimal: list-returning methods map
        // to selectList and every other method maps to selectOne.
        if (List.class.isAssignableFrom(method.getReturnType())) {
            return sqlSession.selectList(statementId, parameter);
        }
        return sqlSession.selectOne(statementId, parameter);
    }

    private Object extractParameter(Method method, Object[] args, String statementId) {
        if (args == null || args.length == 0) {
            return null;
        }
        if (args.length == 1) {
            return args[0];
        }
        throw ExceptionFactory.mappingException(
                "Mapper method accepts more than one parameter in phase 1",
                ErrorContext.create()
                        .mapper(mapperInterface)
                        .statementId(statementId)
                        .parameterSummary(Arrays.toString(args) + " method=" + method.getName()));
    }
}
