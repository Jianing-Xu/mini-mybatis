package com.xujn.minimybatis.exceptions;

/**
 * Signals failures inside the query execution pipeline.
 *
 * <p>Responsibility: wrap JDBC, parameter model and result mapping failures
 * with statement-level context.
 *
 * <p>Thread-safety: immutable and thread-safe.
 */
public class ExecutorException extends PersistenceException {

    public ExecutorException(String message) {
        super(message);
    }

    public ExecutorException(String message, Throwable cause) {
        super(message, cause);
    }
}
