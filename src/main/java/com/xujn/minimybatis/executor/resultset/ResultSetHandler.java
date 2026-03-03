package com.xujn.minimybatis.executor.resultset;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Maps JDBC result sets into framework return values.
 *
 * <p>Responsibility: translate result-set rows into simple values or JavaBean
 * instances without leaking JDBC details to the executor.
 *
 * <p>Thread-safety: implementations are not thread-safe.
 */
public interface ResultSetHandler {

    <E> List<E> handleResultSets(PreparedStatement statement);
}
