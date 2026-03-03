package com.xujn.minimybatis.mapping;

/**
 * Holds the raw executable SQL text.
 *
 * <p>Responsibility: preserve the already-parsed SQL string without adding any
 * dynamic SQL or placeholder expansion semantics.
 *
 * <p>Thread-safety: immutable and thread-safe.
 */
public final class SqlSource {

    private final String sql;

    public SqlSource(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }
}
