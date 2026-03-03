package com.xujn.minimybatis.mapping;

import java.util.List;

/**
 * Holds the parsed SQL text and placeholder metadata.
 *
 * <p>Responsibility: preserve the executable SQL string and the ordered
 * parameter mappings produced from XML parsing.
 *
 * <p>Thread-safety: immutable and thread-safe.
 */
public final class SqlSource {

    private final String originalSql;
    private final String sql;
    private final List<ParameterMapping> parameterMappings;

    public SqlSource(String sql) {
        this(sql, sql, List.of());
    }

    public SqlSource(String originalSql, String sql, List<ParameterMapping> parameterMappings) {
        this.originalSql = originalSql;
        this.sql = sql;
        this.parameterMappings = List.copyOf(parameterMappings);
    }

    public BoundSql getBoundSql(Object parameterObject) {
        return new BoundSql(sql, parameterMappings, parameterObject);
    }

    public String getOriginalSql() {
        return originalSql;
    }

    public String getSql() {
        return sql;
    }

    public List<ParameterMapping> getParameterMappings() {
        return parameterMappings;
    }
}
