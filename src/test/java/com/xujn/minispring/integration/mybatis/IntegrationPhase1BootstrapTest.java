package com.xujn.minispring.integration.mybatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xujn.minimybatis.exceptions.ExecutorException;
import com.xujn.minimybatis.exceptions.MappingException;
import com.xujn.minimybatis.support.CountingDataSource;
import com.xujn.minispring.exception.BeansException;
import com.xujn.minispring.context.support.AnnotationConfigApplicationContext;
import com.xujn.minispring.integration.mybatis.exception.MapperRegistrationException;
import com.xujn.minispring.integration.mybatis.exception.MappingLoadException;
import com.xujn.minispring.integration.mybatis.exception.MissingDataSourceException;
import com.xujn.minispring.integration.mybatis.exception.StatementConflictException;
import com.xujn.minispring.integration.mybatis.support.MapperDefinitionRegistry;
import com.xujn.minispring.integration.mybatis.support.SqlSessionTemplate;
import com.xujn.minispring.integration.mybatis.testsupport.phase1.common.IntegrationUser;
import com.xujn.minispring.integration.mybatis.testsupport.phase1.common.IntegrationUserMapper;
import com.xujn.minispring.integration.mybatis.testsupport.phase1.common.IntegrationUserService;
import org.junit.jupiter.api.Test;

/**
 * Integration acceptance coverage for mini-mybatis + mini-spring phase 1.
 *
 * <p>Responsibility: verify mapper scanning, SqlSessionFactory bootstrap,
 * mapper bean creation and fail-fast startup behaviour.
 *
 * <p>Thread-safety: each test creates its own application context.
 */
class IntegrationPhase1BootstrapTest {

    @Test
    void shouldRegisterMapperBeanDefinitionsAndInfrastructureBeans() {
        AnnotationConfigApplicationContext context = successContext();

        assertTrue(context.containsBeanDefinition("integrationUserMapper"));
        assertTrue(context.containsBeanDefinition("mapperFactory"));
        assertTrue(context.containsBeanDefinition("mapperDefinitionRegistry"));
        assertTrue(context.containsBeanDefinition("sqlSessionTemplate"));

        MapperDefinitionRegistry mapperDefinitionRegistry = context.getBean(MapperDefinitionRegistry.class);
        assertEquals(IntegrationUserMapper.class, mapperDefinitionRegistry.getMapperType("integrationUserMapper"));
    }

    @Test
    void shouldBootstrapSqlSessionFactoryAndQueryViaMapperBean() {
        AnnotationConfigApplicationContext context = successContext();

        IntegrationUserService userService = context.getBean(IntegrationUserService.class);
        IntegrationUser user = userService.loadById(1L);

        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("alice", user.getUsername());
        assertEquals("alice@example.com", user.getEmail());
    }

    @Test
    void shouldReturnSameMapperBeanOnRepeatedLookup() {
        AnnotationConfigApplicationContext context = successContext();

        IntegrationUserMapper first = context.getBean(IntegrationUserMapper.class);
        IntegrationUserMapper second = context.getBean(IntegrationUserMapper.class);

        assertSame(first, second);
    }

    @Test
    void shouldExposeSqlSessionTemplateAsContainerManagedBean() {
        AnnotationConfigApplicationContext context = successContext();

        SqlSessionTemplate sqlSessionTemplate = context.getBean(SqlSessionTemplate.class);
        IntegrationUser user = sqlSessionTemplate.selectOne(
                "com.xujn.minispring.integration.mybatis.testsupport.phase1.common.IntegrationUserMapper.selectById",
                2L);

        assertNotNull(sqlSessionTemplate);
        assertEquals("bob", user.getUsername());
    }

    @Test
    void shouldCloseResourcesOnSuccessfulQuery() {
        AnnotationConfigApplicationContext context = successContext();
        CountingDataSource dataSource = context.getBean(CountingDataSource.class);
        IntegrationUserMapper mapper = context.getBean(IntegrationUserMapper.class);

        mapper.selectById(1L);

        assertEquals(1, dataSource.getConnectionClosedCount());
        assertEquals(1, dataSource.getStatementClosedCount());
        assertEquals(1, dataSource.getResultSetClosedCount());
    }

    @Test
    void shouldCloseResourcesOnMapperFailure() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                "com.xujn.minispring.integration.mybatis.testsupport.phase1.config.invalidresult",
                "com.xujn.minispring.integration.mybatis.testsupport.phase1.common");
        CountingDataSource dataSource = context.getBean(CountingDataSource.class);
        SqlSessionTemplate sqlSessionTemplate = context.getBean(SqlSessionTemplate.class);

        ExecutorException exception = assertThrows(
                ExecutorException.class,
                () -> sqlSessionTemplate.selectOne(
                        "com.xujn.minispring.integration.mybatis.testsupport.phase1.common.IntegrationUserMapper.selectBroken"));

        assertTrue(exception.getMessage().contains("statementId=com.xujn.minispring.integration.mybatis.testsupport.phase1.common.IntegrationUserMapper.selectBroken"));
        assertEquals(1, dataSource.getConnectionClosedCount());
        assertEquals(1, dataSource.getStatementClosedCount());
        assertEquals(1, dataSource.getResultSetClosedCount());
    }

    @Test
    void shouldFailWhenDataSourceMissing() {
        BeansException exception = assertThrows(BeansException.class, () ->
                new AnnotationConfigApplicationContext(
                        "com.xujn.minispring.integration.mybatis.testsupport.phase1.config.missingds",
                        "com.xujn.minispring.integration.mybatis.testsupport.phase1.common"));

        assertTrue(findRootCause(exception) instanceof MissingDataSourceException);
        assertTrue(findRootCause(exception).getMessage().contains("beanName=sqlSessionFactory"));
        assertTrue(findRootCause(exception).getMessage().contains("dependency=dataSource"));
    }

    @Test
    void shouldFailWhenMapperResourceMissing() {
        BeansException exception = assertThrows(BeansException.class, () ->
                new AnnotationConfigApplicationContext(
                        "com.xujn.minispring.integration.mybatis.testsupport.phase1.config.missingresource",
                        "com.xujn.minispring.integration.mybatis.testsupport.phase1.common"));

        assertTrue(findRootCause(exception) instanceof MappingLoadException);
        assertTrue(findRootCause(exception).getMessage().contains("resourcePath=mapper/not-exists.xml"));
    }

    @Test
    void shouldFailWhenStatementIdsConflict() {
        BeansException exception = assertThrows(BeansException.class, () ->
                new AnnotationConfigApplicationContext(
                        "com.xujn.minispring.integration.mybatis.testsupport.phase1.config.duplicate",
                        "com.xujn.minispring.integration.mybatis.testsupport.phase1.common"));

        StatementConflictException cause = findCause(exception, StatementConflictException.class);
        assertNotNull(cause);
        assertTrue(cause.getMessage().contains("selectAll"));
        assertTrue(cause.getMessage().contains("integration-phase1-duplicate-mapper.xml"));
    }

    @Test
    void shouldFailWhenDuplicateMapperRegistrationOccurs() {
        MapperRegistrationException exception = assertThrows(MapperRegistrationException.class, () ->
                new AnnotationConfigApplicationContext(
                        "com.xujn.minispring.integration.mybatis.testsupport.phase1.config.duplicatemapper",
                        "com.xujn.minispring.integration.mybatis.testsupport.phase1.common"));

        assertTrue(exception.getMessage().contains("IntegrationUserMapper"));
        assertTrue(exception.getMessage().contains("integrationUserMapper"));
    }

    private AnnotationConfigApplicationContext successContext() {
        return new AnnotationConfigApplicationContext(
                "com.xujn.minispring.integration.mybatis.testsupport.phase1.config.success",
                "com.xujn.minispring.integration.mybatis.testsupport.phase1.common");
    }

    private Throwable findRootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> expectedType) {
        Throwable current = throwable;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return expectedType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
