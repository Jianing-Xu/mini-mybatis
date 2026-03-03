package com.xujn.minispring.integration.mybatis.exception;

/**
 * Raised when mapper XML resources cannot be loaded or parsed.
 *
 * <p>Responsibility: expose resource path and namespace level diagnostics.
 *
 * <p>Thread-safety: exception type.
 */
public class MappingLoadException extends RuntimeException {

    public MappingLoadException(String message) {
        super(message);
    }

    public MappingLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
