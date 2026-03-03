package com.xujn.minimybatis.support;

/**
 * Test-only type that intentionally violates the phase 1 JavaBean contract.
 *
 * <p>Responsibility: trigger result mapping failure for acceptance coverage.
 *
 * <p>Thread-safety: immutable and thread-safe.
 */
public class NoDefaultConstructorUser {

    private final Long id;

    public NoDefaultConstructorUser(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
