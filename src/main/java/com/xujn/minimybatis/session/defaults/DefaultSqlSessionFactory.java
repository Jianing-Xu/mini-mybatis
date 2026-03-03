package com.xujn.minimybatis.session.defaults;

import com.xujn.minimybatis.executor.SimpleExecutor;
import com.xujn.minimybatis.session.Configuration;
import com.xujn.minimybatis.session.SqlSession;
import com.xujn.minimybatis.session.SqlSessionFactory;

/**
 * Default session factory backed by one immutable configuration.
 *
 * <p>Responsibility: create a fresh {@link DefaultSqlSession} for each caller
 * while reusing the same statement registry and data source.
 *
 * <p>Thread-safety: thread-safe after construction.
 */
public class DefaultSqlSessionFactory implements SqlSessionFactory {

    private final Configuration configuration;

    public DefaultSqlSessionFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public SqlSession openSession() {
        return new DefaultSqlSession(configuration, new SimpleExecutor(configuration));
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }
}
