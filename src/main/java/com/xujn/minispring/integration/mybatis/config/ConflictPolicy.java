package com.xujn.minispring.integration.mybatis.config;

/**
 * Conflict handling strategy for integration bootstrap.
 *
 * <p>Responsibility: define how mapper or statement collisions are treated
 * during startup.
 *
 * <p>Thread-safety: enum constants are immutable and thread-safe.
 */
public enum ConflictPolicy {
    FAIL_FAST,
    OVERRIDE
}
