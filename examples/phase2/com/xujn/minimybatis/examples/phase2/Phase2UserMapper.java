package com.xujn.minimybatis.examples.phase2;

import java.util.List;

/**
 * Example mapper contract for phase 2 binding and mapping features.
 *
 * <p>Responsibility: drive simple parameter binding, bean parameter binding,
 * multi-parameter binding and camel-case result mapping.
 *
 * <p>Thread-safety: interface only; implementation thread-safety follows the
 * underlying session and proxy instance.
 */
public interface Phase2UserMapper {

    Phase2User selectById(Long id);

    List<Phase2User> selectByUsernameAndEmail(String username, String email);

    List<Phase2User> selectByFilter(Phase2UserFilter filter);

    List<Phase2User> selectAllCamelCase();
}
