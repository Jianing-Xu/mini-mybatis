package com.xujn.minimybatis.executor;

/**
 * Selects which executor implementation a {@code SqlSession} should use.
 *
 * <p>Responsibility: keep executor strategy selection in configuration so the
 * session API does not need new overloads when execution behavior changes.
 *
 * <p>Thread-safety: enum constants are immutable and thread-safe.
 */
public enum ExecutorType {
    SIMPLE,
    REUSE
}
