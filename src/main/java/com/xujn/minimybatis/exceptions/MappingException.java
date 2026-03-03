package com.xujn.minimybatis.exceptions;

/**
 * Signals Mapper registration and statement routing failures.
 *
 * <p>Responsibility: surface invalid mapper definitions, missing statements
 * and unsupported mapper method signatures.
 *
 * <p>Thread-safety: immutable and thread-safe.
 */
public class MappingException extends PersistenceException {

    public MappingException(String message) {
        super(message);
    }

    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
