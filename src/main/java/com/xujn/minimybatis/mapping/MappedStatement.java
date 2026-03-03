package com.xujn.minimybatis.mapping;

/**
 * Describes a single mapped SQL statement.
 *
 * <p>Responsibility: carry the minimum metadata required by phase 1 query
 * execution, including SQL text, resource origin and result type.
 *
 * <p>Thread-safety: immutable and thread-safe.
 */
public final class MappedStatement {

    private final String id;
    private final String namespace;
    private final String statementId;
    private final SqlSource sqlSource;
    private final SqlCommandType sqlCommandType;
    private final Class<?> parameterType;
    private final Class<?> resultType;
    private final String resource;

    public MappedStatement(
            String id,
            String namespace,
            SqlSource sqlSource,
            SqlCommandType sqlCommandType,
            Class<?> parameterType,
            Class<?> resultType,
            String resource) {
        this.id = id;
        this.namespace = namespace;
        this.statementId = namespace + "." + id;
        this.sqlSource = sqlSource;
        this.sqlCommandType = sqlCommandType;
        this.parameterType = parameterType;
        this.resultType = resultType;
        this.resource = resource;
    }

    public String getId() {
        return id;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getStatementId() {
        return statementId;
    }

    public SqlSource getSqlSource() {
        return sqlSource;
    }

    public SqlCommandType getSqlCommandType() {
        return sqlCommandType;
    }

    public Class<?> getParameterType() {
        return parameterType;
    }

    public Class<?> getResultType() {
        return resultType;
    }

    public String getResource() {
        return resource;
    }

    public BoundSql getBoundSql(Object parameterObject) {
        return sqlSource.getBoundSql(parameterObject);
    }
}
