package com.xujn.minimybatis.exceptions;

/**
 * Signals configuration and XML parsing errors during startup.
 *
 * <p>Responsibility: fail fast while building mapped statements and framework
 * metadata before any query reaches JDBC.
 *
 * <p>Thread-safety: immutable and thread-safe.
 */
public class BuilderException extends PersistenceException {

    public BuilderException(String message) {
        super(message);
    }

    public BuilderException(String message, Throwable cause) {
        super(message, cause);
    }
}
