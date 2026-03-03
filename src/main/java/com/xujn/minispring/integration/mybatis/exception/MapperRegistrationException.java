package com.xujn.minispring.integration.mybatis.exception;

/**
 * Raised when mapper bean registration or mapper namespace resolution fails.
 *
 * <p>Responsibility: expose mapper type, bean name and scan source conflicts.
 *
 * <p>Thread-safety: exception type.
 */
public class MapperRegistrationException extends RuntimeException {

    public MapperRegistrationException(String message) {
        super(message);
    }

    public MapperRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
