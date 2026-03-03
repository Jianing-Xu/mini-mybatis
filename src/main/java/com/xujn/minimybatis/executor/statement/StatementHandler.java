package com.xujn.minimybatis.executor.statement;

import com.xujn.minimybatis.mapping.BoundSql;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * Coordinates prepared statement preparation, parameter binding and query
 * execution.
 *
 * <p>Responsibility: split JDBC statement concerns out of the executor so the
 * execution chain stays replaceable.
 *
 * <p>Thread-safety: implementations are not thread-safe.
 */
public interface StatementHandler {

    PreparedStatement prepare(Connection connection);

    void parameterize(PreparedStatement statement);

    <E> List<E> query(PreparedStatement statement);

    BoundSql getBoundSql();
}
