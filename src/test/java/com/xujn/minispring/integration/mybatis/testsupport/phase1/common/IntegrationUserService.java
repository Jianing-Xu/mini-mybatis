package com.xujn.minispring.integration.mybatis.testsupport.phase1.common;

import com.xujn.minispring.context.annotation.Autowired;
import com.xujn.minispring.context.annotation.Component;
import java.util.List;

@Component
public class IntegrationUserService {

    @Autowired
    private IntegrationUserMapper integrationUserMapper;

    public IntegrationUser loadById(Long id) {
        return integrationUserMapper.selectById(id);
    }

    public List<IntegrationUser> loadAll() {
        return integrationUserMapper.selectAll();
    }
}
