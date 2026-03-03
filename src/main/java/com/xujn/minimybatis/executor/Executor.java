package com.xujn.minimybatis.executor;

import com.xujn.minimybatis.mapping.MappedStatement;
import java.util.List;

/**
 * Minimal query executor contract for phase 1.
 *
 * <p>Responsibility: execute one mapped statement and return result objects
 * without exposing JDBC details to the session layer.
 *
 * <p>Thread-safety: implementations are not required to be thread-safe.
 */
public interface Executor {

    <E> List<E> query(MappedStatement mappedStatement, Object parameter);

    void close(boolean forceRollback);
}
