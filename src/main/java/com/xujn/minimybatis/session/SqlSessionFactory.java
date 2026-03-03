package com.xujn.minimybatis.session;

/**
 * Creates {@link SqlSession} instances from a fixed configuration.
 *
 * <p>Responsibility: decouple session creation from application code so the
 * same configuration can open multiple sessions.
 *
 * <p>Thread-safety: factory implementations should be thread-safe after
 * construction.
 */
public interface SqlSessionFactory {

    SqlSession openSession();

    Configuration getConfiguration();
}
