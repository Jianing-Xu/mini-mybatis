package com.xujn.minimybatis.support;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * DataSource wrapper that counts close calls on JDBC resources.
 *
 * <p>Responsibility: prove that the executor closes connection, statement and
 * result set on both success and failure paths.
 *
 * <p>Thread-safety: counters are thread-safe; intended for test usage only.
 */
public class CountingDataSource implements DataSource {

    private final DataSource delegate;
    private final AtomicInteger connectionClosedCount = new AtomicInteger();
    private final AtomicInteger statementClosedCount = new AtomicInteger();
    private final AtomicInteger resultSetClosedCount = new AtomicInteger();

    public CountingDataSource(DataSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return CountingConnection.wrap(delegate.getConnection(), this);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return CountingConnection.wrap(delegate.getConnection(username, password), this);
    }

    void incrementConnectionClosed() {
        connectionClosedCount.incrementAndGet();
    }

    void incrementStatementClosed() {
        statementClosedCount.incrementAndGet();
    }

    void incrementResultSetClosed() {
        resultSetClosedCount.incrementAndGet();
    }

    public int getConnectionClosedCount() {
        return connectionClosedCount.get();
    }

    public int getStatementClosedCount() {
        return statementClosedCount.get();
    }

    public int getResultSetClosedCount() {
        return resultSetClosedCount.get();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }
}
