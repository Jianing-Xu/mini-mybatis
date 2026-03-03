package com.xujn.minimybatis.session;

import java.util.List;

/**
 * Main framework entry point for query execution and mapper access.
 *
 * <p>Responsibility: shield callers from executor and mapper proxy details and
 * expose the minimal phase 1 query API.
 *
 * <p>Thread-safety: session instances are not thread-safe and should be used
 * by one thread at a time.
 */
public interface SqlSession extends AutoCloseable {

    <T> T selectOne(String statement);

    <T> T selectOne(String statement, Object parameter);

    <E> List<E> selectList(String statement);

    <E> List<E> selectList(String statement, Object parameter);

    <T> T getMapper(Class<T> type);

    Configuration getConfiguration();

    @Override
    void close();
}
