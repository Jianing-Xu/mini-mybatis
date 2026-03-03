package com.xujn.minimybatis.mapping;

/**
 * Describes one parsed parameter placeholder.
 *
 * <p>Responsibility: preserve the property name that should be resolved from
 * the runtime parameter object for one JDBC placeholder.
 *
 * <p>Thread-safety: immutable and thread-safe.
 */
public final class ParameterMapping {

    private final String property;

    public ParameterMapping(String property) {
        this.property = property;
    }

    public String getProperty() {
        return property;
    }
}
