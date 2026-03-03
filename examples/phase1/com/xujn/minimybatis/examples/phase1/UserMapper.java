package com.xujn.minimybatis.examples.phase1;

import java.util.List;

/**
 * Example mapper contract for phase 1.
 *
 * <p>Responsibility: drive the mapper proxy path with one single-row query and
 * one list query.
 *
 * <p>Thread-safety: interface only; implementation thread-safety follows the
 * underlying session and proxy instance.
 */
public interface UserMapper {

    User selectById(Long id);

    List<User> selectAll();
}
