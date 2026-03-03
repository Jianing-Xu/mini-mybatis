package com.xujn.minimybatis.support;

import java.util.Arrays;

/**
 * Carries stable diagnostic context for framework exceptions.
 *
 * <p>Responsibility: guarantee that every failure message includes the same
 * keys so acceptance checks and operators can pinpoint the failing statement.
 *
 * <p>Thread-safety: instances are mutable and request-scoped; do not share
 * them across threads.
 */
public final class ErrorContext {

    private String statementId = "n/a";
    private String resource = "n/a";
    private String sql = "n/a";
    private String parameter = "null";
    private String resultType = "n/a";
    private String mapper = "n/a";

    public static ErrorContext create() {
        return new ErrorContext();
    }

    public ErrorContext statementId(String statementId) {
        if (statementId != null && !statementId.isBlank()) {
            this.statementId = statementId;
        }
        return this;
    }

    public ErrorContext resource(String resource) {
        if (resource != null && !resource.isBlank()) {
            this.resource = resource;
        }
        return this;
    }

    public ErrorContext sql(String sql) {
        if (sql != null && !sql.isBlank()) {
            this.sql = sql;
        }
        return this;
    }

    public ErrorContext parameter(Object parameter) {
        if (parameter == null) {
            this.parameter = "null";
        } else if (parameter.getClass().isArray() && parameter instanceof Object[] objects) {
            this.parameter = parameter.getClass().getComponentType().getName() + Arrays.toString(objects);
        } else {
            this.parameter = parameter.getClass().getName() + "(" + parameter + ")";
        }
        return this;
    }

    public ErrorContext parameterSummary(String parameterSummary) {
        if (parameterSummary != null && !parameterSummary.isBlank()) {
            this.parameter = parameterSummary;
        }
        return this;
    }

    public ErrorContext resultType(Class<?> resultType) {
        if (resultType != null) {
            this.resultType = resultType.getName();
        }
        return this;
    }

    public ErrorContext mapper(Class<?> mapper) {
        if (mapper != null) {
            this.mapper = mapper.getName();
        }
        return this;
    }

    public String format() {
        return "statementId=" + statementId
                + ", resource=" + resource
                + ", sql=" + sql
                + ", parameter=" + parameter
                + ", resultType=" + resultType
                + ", mapper=" + mapper;
    }
}
