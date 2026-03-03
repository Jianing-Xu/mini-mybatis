package com.xujn.minimybatis.support;

import com.xujn.minimybatis.exceptions.BuilderException;
import com.xujn.minimybatis.exceptions.ExecutorException;
import com.xujn.minimybatis.exceptions.MappingException;
import com.xujn.minimybatis.exceptions.PersistenceException;

/**
 * Creates framework exceptions with uniform diagnostic messages.
 *
 * <p>Responsibility: centralize message formatting so each error path exposes
 * the same context keys.
 *
 * <p>Thread-safety: stateless and thread-safe.
 */
public final class ExceptionFactory {

    private ExceptionFactory() {
    }

    public static BuilderException builderException(String message, ErrorContext context) {
        return new BuilderException(message + "; " + context.format());
    }

    public static BuilderException builderException(String message, ErrorContext context, Throwable cause) {
        return new BuilderException(message + "; " + context.format(), cause);
    }

    public static MappingException mappingException(String message, ErrorContext context) {
        return new MappingException(message + "; " + context.format());
    }

    public static MappingException mappingException(String message, ErrorContext context, Throwable cause) {
        return new MappingException(message + "; " + context.format(), cause);
    }

    public static ExecutorException executorException(String message, ErrorContext context) {
        return new ExecutorException(message + "; " + context.format());
    }

    public static ExecutorException executorException(String message, ErrorContext context, Throwable cause) {
        return new ExecutorException(message + "; " + context.format(), cause);
    }

    public static PersistenceException persistenceException(String message, ErrorContext context, Throwable cause) {
        return new PersistenceException(message + "; " + context.format(), cause);
    }
}
