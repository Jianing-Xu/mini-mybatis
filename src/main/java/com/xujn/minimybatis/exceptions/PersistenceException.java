package com.xujn.minimybatis.exceptions;

/**
 * Base runtime exception for all mini-mybatis failures.
 *
 * <p>Responsibility: provides a single unchecked root type for builder,
 * mapping and executor failures so callers can catch framework errors in one
 * place.
 *
 * <p>Thread-safety: immutable and thread-safe.
 */
public class PersistenceException extends RuntimeException {

    public PersistenceException(String message) {
        super(message);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
