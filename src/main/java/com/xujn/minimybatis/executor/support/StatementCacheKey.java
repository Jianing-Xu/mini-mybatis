package com.xujn.minimybatis.executor.support;

import java.util.Objects;

/**
 * Cache key for reusable prepared statements inside one executor instance.
 *
 * <p>Responsibility: identify statements by their executable SQL text so one
 * session can reuse prepared statements for repeated queries.
 *
 * <p>Thread-safety: immutable and thread-safe.
 */
public final class StatementCacheKey {

    private final String sql;

    public StatementCacheKey(String sql) {
        this.sql = Objects.requireNonNull(sql, "sql");
    }

    public String getSql() {
        return sql;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StatementCacheKey that)) {
            return false;
        }
        return sql.equals(that.sql);
    }

    @Override
    public int hashCode() {
        return sql.hashCode();
    }

    @Override
    public String toString() {
        return sql;
    }
}
