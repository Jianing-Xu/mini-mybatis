package com.xujn.minimybatis.mapping;

import java.util.List;

/**
 * Represents the executable SQL plus ordered parameter metadata.
 *
 * <p>Responsibility: carry the final JDBC SQL string and placeholder order for
 * one execution.
 *
 * <p>Thread-safety: immutable and thread-safe.
 */
public final class BoundSql {

    private final String sql;
    private final List<ParameterMapping> parameterMappings;
    private final Object parameterObject;

    public BoundSql(String sql, List<ParameterMapping> parameterMappings, Object parameterObject) {
        this.sql = sql;
        this.parameterMappings = List.copyOf(parameterMappings);
        this.parameterObject = parameterObject;
    }

    public String getSql() {
        return sql;
    }

    public List<ParameterMapping> getParameterMappings() {
        return parameterMappings;
    }

    public Object getParameterObject() {
        return parameterObject;
    }
}
