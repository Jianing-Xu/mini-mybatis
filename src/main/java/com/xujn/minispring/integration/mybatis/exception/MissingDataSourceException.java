package com.xujn.minispring.integration.mybatis.exception;

/**
 * Raised when SqlSessionFactory bootstrap cannot resolve a DataSource.
 *
 * <p>Responsibility: fail fast before XML parsing or session factory creation.
 *
 * <p>Thread-safety: exception type.
 */
public class MissingDataSourceException extends RuntimeException {

    public MissingDataSourceException(String message) {
        super(message);
    }
}
