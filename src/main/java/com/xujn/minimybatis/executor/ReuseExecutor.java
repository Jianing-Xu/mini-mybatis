package com.xujn.minimybatis.executor;

import com.xujn.minimybatis.executor.statement.RoutingStatementHandler;
import com.xujn.minimybatis.executor.statement.StatementHandler;
import com.xujn.minimybatis.executor.support.StatementCacheKey;
import com.xujn.minimybatis.mapping.BoundSql;
import com.xujn.minimybatis.mapping.MappedStatement;
import com.xujn.minimybatis.session.Configuration;
import com.xujn.minimybatis.support.ErrorContext;
import com.xujn.minimybatis.support.ExceptionFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Executor that reuses prepared statements within one {@code SqlSession}.
 *
 * <p>Responsibility: hold one JDBC connection for the session lifetime and
 * cache prepared statements by SQL so repeated queries avoid re-preparing the
 * same SQL text.
 *
 * <p>Thread-safety: not thread-safe; one executor belongs to one session.
 */
public class ReuseExecutor implements Executor {

    private final Configuration configuration;
    private final Map<StatementCacheKey, PreparedStatement> statementCache = new LinkedHashMap<>();
    private final Map<StatementCacheKey, ErrorContext> statementContexts = new LinkedHashMap<>();
    private Connection connection;
    private boolean closed;

    public ReuseExecutor(Configuration configuration) {
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

        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        StatementHandler statementHandler =
                new RoutingStatementHandler(configuration, mappedStatement, parameter, boundSql);
        StatementCacheKey cacheKey = new StatementCacheKey(boundSql.getSql());
        ErrorContext context = buildContext(mappedStatement, parameter);

        try {
            PreparedStatement statement =
                    getOrCreateStatement(dataSource, cacheKey, statementHandler, mappedStatement, boundSql);
            statementHandler.parameterize(statement);
            return statementHandler.query(statement);
        } catch (RuntimeException ex) {
            // A failed reused statement is conservatively discarded so the next
            // query does not keep retrying a potentially broken JDBC object.
            discardCachedStatement(cacheKey, ex);
            throw ex;
        } catch (SQLException ex) {
            throw ExceptionFactory.persistenceException(
                    "Failed to execute query",
                    context,
                    ex);
        }
    }

    @Override
    public void close(boolean forceRollback) {
        if (closed) {
            return;
        }
        SQLException firstError = null;
        ErrorContext firstContext = null;

        for (Map.Entry<StatementCacheKey, PreparedStatement> entry : statementCache.entrySet()) {
            PreparedStatement statement = entry.getValue();
            try {
                if (statement != null && !statement.isClosed()) {
                    statement.close();
                }
            } catch (SQLException ex) {
                if (firstError == null) {
                    firstError = ex;
                    firstContext = statementContexts.get(entry.getKey());
                }
            }
        }
        statementCache.clear();
        statementContexts.clear();

        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ex) {
                if (firstError == null) {
                    firstError = ex;
                    firstContext = ErrorContext.create().executorType(ExecutorType.REUSE);
                }
            } finally {
                connection = null;
            }
        }
        closed = true;

        if (firstError != null) {
            throw ExceptionFactory.executorException(
                    "Failed to close reused JDBC resources",
                    firstContext != null ? firstContext : ErrorContext.create().executorType(ExecutorType.REUSE),
                    firstError);
        }
    }

    private PreparedStatement getOrCreateStatement(
            DataSource dataSource,
            StatementCacheKey cacheKey,
            StatementHandler statementHandler,
            MappedStatement mappedStatement,
            BoundSql boundSql) throws SQLException {
        PreparedStatement cached = statementCache.get(cacheKey);
        if (cached != null) {
            if (cached.isClosed()) {
                removeCachedStatement(cacheKey);
            } else {
                return cached;
            }
        }

        Connection activeConnection = getOrCreateConnection(dataSource);
        PreparedStatement statement = statementHandler.prepare(activeConnection);
        statementCache.put(cacheKey, statement);
        statementContexts.put(cacheKey, statementContext(mappedStatement, boundSql));
        return statement;
    }

    private Connection getOrCreateConnection(DataSource dataSource) throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = dataSource.getConnection();
        }
        return connection;
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
                .resultType(mappedStatement.getResultType())
                .executorType(ExecutorType.REUSE);
    }

    private ErrorContext statementContext(MappedStatement mappedStatement, BoundSql boundSql) {
        return ErrorContext.create()
                .statementId(mappedStatement.getStatementId())
                .resource(mappedStatement.getResource())
                .sql(boundSql.getSql())
                .resultType(mappedStatement.getResultType())
                .executorType(ExecutorType.REUSE);
    }

    private void discardCachedStatement(StatementCacheKey cacheKey, RuntimeException failure) {
        PreparedStatement cached = statementCache.get(cacheKey);
        removeCachedStatement(cacheKey);
        if (cached == null) {
            return;
        }
        try {
            if (!cached.isClosed()) {
                cached.close();
            }
        } catch (SQLException closeEx) {
            failure.addSuppressed(closeEx);
        }
    }

    private void removeCachedStatement(StatementCacheKey cacheKey) {
        statementCache.remove(cacheKey);
        statementContexts.remove(cacheKey);
    }
}
