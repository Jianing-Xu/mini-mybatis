package com.xujn.minimybatis.session.defaults;

import com.xujn.minimybatis.executor.Executor;
import com.xujn.minimybatis.support.ErrorContext;
import com.xujn.minimybatis.support.ExceptionFactory;
import com.xujn.minimybatis.session.Configuration;
import com.xujn.minimybatis.session.SqlSession;
import java.util.List;

/**
 * Default phase 1 session implementation.
 *
 * <p>Responsibility: resolve mapped statements, dispatch to the executor and
 * expose mapper proxies through the configuration registry.
 *
 * <p>Thread-safety: not thread-safe; one session should stay confined to one
 * thread and one usage scope.
 */
public class DefaultSqlSession implements SqlSession {

    private final Configuration configuration;
    private final Executor executor;
    private boolean closed;

    public DefaultSqlSession(Configuration configuration, Executor executor) {
        this.configuration = configuration;
        this.executor = executor;
    }

    @Override
    public <T> T selectOne(String statement) {
        return selectOne(statement, null);
    }

    @Override
    public <T> T selectOne(String statement, Object parameter) {
        List<T> results = selectList(statement, parameter);
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw ExceptionFactory.executorException(
                    "Expected one result but found " + results.size(),
                    ErrorContext.create().statementId(statement).parameter(parameter));
        }
        return results.get(0);
    }

    @Override
    public <E> List<E> selectList(String statement) {
        return selectList(statement, null);
    }

    @Override
    public <E> List<E> selectList(String statement, Object parameter) {
        ensureOpen();
        if (!configuration.hasStatement(statement)) {
            throw ExceptionFactory.mappingException(
                    "MappedStatement not found",
                    ErrorContext.create().statementId(statement).parameter(parameter));
        }
        return executor.query(configuration.getMappedStatement(statement), parameter);
    }

    @Override
    public <T> T getMapper(Class<T> type) {
        ensureOpen();
        return configuration.getMapper(type, this);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void close() {
        if (!closed) {
            executor.close(false);
            closed = true;
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw ExceptionFactory.executorException(
                    "SqlSession is already closed",
                    ErrorContext.create());
        }
    }
}
