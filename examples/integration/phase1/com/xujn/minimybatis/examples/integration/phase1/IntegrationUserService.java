package com.xujn.minimybatis.examples.integration.phase1;

import com.xujn.minispring.context.annotation.Autowired;
import com.xujn.minispring.context.annotation.Component;
import java.util.List;

/**
 * Example service that consumes a mapper bean from mini-spring.
 *
 * <p>Responsibility: prove that mapper proxies can be injected into ordinary
 * application components.
 *
 * <p>Thread-safety: stateless after wiring; safe for singleton use.
 */
@Component
public class IntegrationUserService {

    @Autowired
    private IntegrationUserMapper integrationUserMapper;

    public IntegrationUser load(Long id) {
        return integrationUserMapper.selectById(id);
    }

    public List<IntegrationUser> loadAll() {
        return integrationUserMapper.selectAll();
    }
}
