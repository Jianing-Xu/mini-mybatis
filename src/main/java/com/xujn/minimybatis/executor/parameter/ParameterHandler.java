package com.xujn.minimybatis.executor.parameter;

import java.sql.PreparedStatement;

/**
 * Binds runtime parameters to JDBC placeholders.
 *
 * <p>Responsibility: resolve parameter values from the current execution model
 * and write them to a prepared statement in mapping order.
 *
 * <p>Thread-safety: implementations are not thread-safe.
 */
public interface ParameterHandler {

    Object getParameterObject();

    void setParameters(PreparedStatement statement);
}
