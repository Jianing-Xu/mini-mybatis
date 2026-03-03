package com.xujn.minispring.integration.mybatis.testsupport.phase1.common;

import java.util.List;

public interface IntegrationUserMapper {

    IntegrationUser selectById(Long id);

    List<IntegrationUser> selectAll();
}
