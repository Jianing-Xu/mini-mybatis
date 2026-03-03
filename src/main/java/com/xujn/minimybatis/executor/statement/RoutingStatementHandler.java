package com.xujn.minimybatis.executor.statement;

import com.xujn.minimybatis.mapping.BoundSql;
import com.xujn.minimybatis.mapping.MappedStatement;
import com.xujn.minimybatis.session.Configuration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * Stable statement-handler entry point for executor implementations.
 *
 * <p>Responsibility: centralize statement-handler selection so executors do
 * not couple themselves to one concrete handler implementation.
 *
 * <p>Thread-safety: not thread-safe; created per execution.
 */
public class RoutingStatementHandler implements StatementHandler {

    private final StatementHandler delegate;

    public RoutingStatementHandler(
            Configuration configuration,
            MappedStatement mappedStatement,
            Object parameterObject,
            BoundSql boundSql) {
        this.delegate =
                new PreparedStatementHandler(configuration, mappedStatement, parameterObject, boundSql);
    }

    @Override
    public PreparedStatement prepare(Connection connection) {
        return delegate.prepare(connection);
    }

    @Override
    public void parameterize(PreparedStatement statement) {
        delegate.parameterize(statement);
    }

    @Override
    public <E> List<E> query(PreparedStatement statement) {
        return delegate.query(statement);
    }

    @Override
    public BoundSql getBoundSql() {
        return delegate.getBoundSql();
    }
}
