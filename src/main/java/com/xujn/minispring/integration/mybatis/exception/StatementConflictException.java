package com.xujn.minispring.integration.mybatis.exception;

/**
 * Raised when duplicate statement ids are discovered during integration bootstrap.
 *
 * <p>Responsibility: stop startup before conflicting statements enter the
 * running configuration.
 *
 * <p>Thread-safety: exception type.
 */
public class StatementConflictException extends RuntimeException {

    public StatementConflictException(String message) {
        super(message);
    }

    public StatementConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
