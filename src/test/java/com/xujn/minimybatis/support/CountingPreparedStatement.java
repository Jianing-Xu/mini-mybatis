package com.xujn.minimybatis.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Dynamic proxy factory that counts prepared statement close calls.
 *
 * <p>Responsibility: decorate executeQuery and close so resource verification
 * stays lightweight in tests.
 *
 * <p>Thread-safety: proxy state is confined to one wrapped statement.
 */
public final class CountingPreparedStatement {

    private CountingPreparedStatement() {
    }

    public static PreparedStatement wrap(PreparedStatement delegate, CountingDataSource counters) {
        InvocationHandler handler = new CountingPreparedStatementHandler(delegate, counters);
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[] {PreparedStatement.class},
                handler);
    }

    private static final class CountingPreparedStatementHandler implements InvocationHandler {

        private final PreparedStatement delegate;
        private final CountingDataSource counters;
        private boolean closed;

        private CountingPreparedStatementHandler(PreparedStatement delegate, CountingDataSource counters) {
            this.delegate = delegate;
            this.counters = counters;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(delegate, args);
            }
            if ("executeQuery".equals(method.getName()) && (args == null || args.length == 0)) {
                ResultSet resultSet = (ResultSet) method.invoke(delegate, args);
                return CountingResultSet.wrap(resultSet, counters);
            }
            if ("close".equals(method.getName())) {
                try {
                    return method.invoke(delegate, args);
                } finally {
                    if (!closed) {
                        counters.incrementStatementClosed();
                        closed = true;
                    }
                }
            }
            return method.invoke(delegate, args);
        }
    }
}
