package com.xujn.minimybatis.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;

/**
 * Dynamic proxy factory that counts result set close calls.
 *
 * <p>Responsibility: capture result-set cleanup without manually implementing
 * the full ResultSet interface.
 *
 * <p>Thread-safety: proxy state is confined to one wrapped result set.
 */
public final class CountingResultSet {

    private CountingResultSet() {
    }

    public static ResultSet wrap(ResultSet delegate, CountingDataSource counters) {
        InvocationHandler handler = new CountingResultSetHandler(delegate, counters);
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                handler);
    }

    private static final class CountingResultSetHandler implements InvocationHandler {

        private final ResultSet delegate;
        private final CountingDataSource counters;
        private boolean closed;

        private CountingResultSetHandler(ResultSet delegate, CountingDataSource counters) {
            this.delegate = delegate;
            this.counters = counters;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(delegate, args);
            }
            if ("close".equals(method.getName())) {
                try {
                    return method.invoke(delegate, args);
                } finally {
                    if (!closed) {
                        counters.incrementResultSetClosed();
                        closed = true;
                    }
                }
            }
            return method.invoke(delegate, args);
        }
    }
}
