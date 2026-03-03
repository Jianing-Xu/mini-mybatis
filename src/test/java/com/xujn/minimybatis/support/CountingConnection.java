package com.xujn.minimybatis.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Dynamic proxy factory that counts connection close calls.
 *
 * <p>Responsibility: wrap a JDBC connection and decorate prepared statements
 * without manually implementing the full JDBC interface.
 *
 * <p>Thread-safety: proxy state is confined to one wrapped connection.
 */
public final class CountingConnection {

    private CountingConnection() {
    }

    public static Connection wrap(Connection delegate, CountingDataSource counters) {
        InvocationHandler handler = new CountingConnectionHandler(delegate, counters);
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                handler);
    }

    private static final class CountingConnectionHandler implements InvocationHandler {

        private final Connection delegate;
        private final CountingDataSource counters;
        private boolean closed;

        private CountingConnectionHandler(Connection delegate, CountingDataSource counters) {
            this.delegate = delegate;
            this.counters = counters;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(delegate, args);
            }
            if ("prepareStatement".equals(method.getName())
                    && args != null
                    && args.length > 0
                    && args[0] instanceof String) {
                PreparedStatement statement = (PreparedStatement) method.invoke(delegate, args);
                counters.incrementPreparedStatementCreated();
                return CountingPreparedStatement.wrap(statement, counters);
            }
            if ("close".equals(method.getName())) {
                try {
                    return method.invoke(delegate, args);
                } finally {
                    if (!closed) {
                        counters.incrementConnectionClosed();
                        closed = true;
                    }
                }
            }
            return method.invoke(delegate, args);
        }
    }
}
