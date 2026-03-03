package com.xujn.minimybatis.binding;

import com.xujn.minimybatis.support.ErrorContext;
import com.xujn.minimybatis.support.ExceptionFactory;
import com.xujn.minimybatis.support.JdbcUtils;
import com.xujn.minimybatis.session.SqlSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            Object argument = args[0];
            if (argument == null || JdbcUtils.isSimpleType(argument.getClass())) {
                return wrapNamedParameters(method, args);
            }
            return argument;
        }
        return wrapNamedParameters(method, args);
    }

    private Map<String, Object> wrapNamedParameters(Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        Map<String, Object> parameterMap = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object argument = args[i];
            parameterMap.put("param" + (i + 1), argument);
            if (parameters[i].isNamePresent()) {
                parameterMap.put(parameters[i].getName(), argument);
            }
            if (args.length == 1) {
                parameterMap.put("value", argument);
                parameterMap.put("_parameter", argument);
            }
        }
        return parameterMap;
    }
}
