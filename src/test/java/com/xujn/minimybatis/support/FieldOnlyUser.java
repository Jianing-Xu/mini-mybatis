package com.xujn.minimybatis.support;

/**
 * Test-only result type without setters.
 *
 * <p>Responsibility: verify that result mapping can fall back to direct field
 * access when no writable setter exists.
 *
 * <p>Thread-safety: mutable and not thread-safe.
 */
public class FieldOnlyUser {

    Long id;
    String userName;
    String email;

    public Long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }
}
