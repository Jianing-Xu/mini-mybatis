package com.xujn.minimybatis.examples.integration.phase1;

import java.util.List;

/**
 * Example mapper interface exposed as a mini-spring bean.
 *
 * <p>Responsibility: define query methods for the integration example.
 *
 * <p>Thread-safety: interface only.
 */
public interface IntegrationUserMapper {

    IntegrationUser selectById(Long id);

    List<IntegrationUser> selectAll();
}
