package com.xujn.minimybatis.support;

import com.xujn.minimybatis.examples.phase2.Phase2User;
import java.util.List;

/**
 * Test-only mapper for phase 2 failure and fallback scenarios.
 *
 * <p>Responsibility: keep negative-path mapper contracts out of production
 * examples while still exercising parameter and mapping edge cases.
 *
 * <p>Thread-safety: interface only; implementation thread-safety follows the
 * underlying session and proxy instance.
 */
public interface Phase2ErrorMapper {

    List<Phase2User> selectMissingParameter(Long id);

    List<FieldOnlyUser> selectFieldOnly();
}
