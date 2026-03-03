package com.xujn.minimybatis.executor;

import com.xujn.minimybatis.executor.statement.PreparedStatementHandler;
import com.xujn.minimybatis.executor.statement.StatementHandler;
import com.xujn.minimybatis.mapping.BoundSql;
import com.xujn.minimybatis.mapping.MappedStatement;
import com.xujn.minimybatis.session.Configuration;
import com.xujn.minimybatis.support.ErrorContext;
import com.xujn.minimybatis.support.ExceptionFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

/**
 * Direct JDBC executor for the phase 1 query pipeline.
 *
 * <p>Responsibility: open a connection, delegate statement preparation and keep
 * connection-level resource handling in one place.
 *
 * <p>Thread-safety: not thread-safe; use one executor per session.
 */
public class SimpleExecutor implements Executor {

    private final Configuration configuration;
    private boolean closed;

    public SimpleExecutor(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <E> List<E> query(MappedStatement mappedStatement, Object parameter) {
        ensureOpen(mappedStatement, parameter);
        DataSource dataSource = configuration.getDataSource();
        if (dataSource == null) {
            throw ExceptionFactory.executorException(
                    "DataSource is not configured",
                    buildContext(mappedStatement, parameter));
        }

        String sql = mappedStatement.getSqlSource().getSql();
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        StatementHandler statementHandler =
                new PreparedStatementHandler(configuration, mappedStatement, parameter, boundSql);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = statementHandler.prepare(connection)) {
            statementHandler.parameterize(statement);
            return statementHandler.query(statement);
        } catch (SQLException ex) {
            throw ExceptionFactory.persistenceException(
                    "Failed to execute query",
                    buildContext(mappedStatement, parameter),
                    ex);
        }
    }

    @Override
    public void close(boolean forceRollback) {
        closed = true;
    }

    private void ensureOpen(MappedStatement mappedStatement, Object parameter) {
        if (closed) {
            throw ExceptionFactory.executorException(
                    "Executor is already closed",
                    buildContext(mappedStatement, parameter));
        }
    }

    private ErrorContext buildContext(MappedStatement mappedStatement, Object parameter) {
        return ErrorContext.create()
                .statementId(mappedStatement.getStatementId())
                .resource(mappedStatement.getResource())
                .sql(mappedStatement.getBoundSql(parameter).getSql())
                .parameter(parameter)
                .resultType(mappedStatement.getResultType());
    }
}
