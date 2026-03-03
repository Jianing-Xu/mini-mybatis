package com.xujn.minimybatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xujn.minimybatis.builder.xml.XmlMapperBuilder;
import com.xujn.minimybatis.exceptions.ExecutorException;
import com.xujn.minimybatis.examples.phase2.Phase2User;
import com.xujn.minimybatis.examples.phase2.Phase2UserFilter;
import com.xujn.minimybatis.examples.phase2.Phase2UserMapper;
import com.xujn.minimybatis.mapping.BoundSql;
import com.xujn.minimybatis.mapping.MappedStatement;
import com.xujn.minimybatis.session.Configuration;
import com.xujn.minimybatis.session.SqlSession;
import com.xujn.minimybatis.session.SqlSessionFactory;
import com.xujn.minimybatis.session.defaults.DefaultSqlSessionFactory;
import com.xujn.minimybatis.support.CountingDataSource;
import com.xujn.minimybatis.support.FieldOnlyUser;
import com.xujn.minimybatis.support.NoDefaultConstructorUser;
import com.xujn.minimybatis.support.Phase2ErrorMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

/**
 * Phase 2 acceptance coverage for parameter binding and result mapping.
 *
 * <p>Responsibility: verify placeholder parsing, bean and multi-parameter
 * binding, underscore-to-camel-case mapping and cleanup behavior.
 *
 * <p>Thread-safety: test class is stateless; each test creates its own
 * configuration and data source.
 */
class Phase2BindingMappingTest {

    @Test
    void shouldParseHashPlaceholdersIntoBoundSql() throws Exception {
        Configuration configuration = newConfiguration(baseDataSource(), true);
        loadMapper(configuration, "mapper/phase2-user-mapper.xml");

        MappedStatement statement = configuration.getMappedStatement(
                "com.xujn.minimybatis.examples.phase2.Phase2UserMapper.selectByUsernameAndEmail");
        BoundSql boundSql = statement.getBoundSql(null);

        assertEquals("select id, username as user_name, email from users where username = ? and email = ?",
                boundSql.getSql());
        assertEquals(2, boundSql.getParameterMappings().size());
        assertEquals("username", boundSql.getParameterMappings().get(0).getProperty());
        assertEquals("email", boundSql.getParameterMappings().get(1).getProperty());
    }

    @Test
    void shouldQueryWithSingleSimpleParameter() throws Exception {
        SqlSessionFactory factory = phase2Factory(baseDataSource(), true);

        try (SqlSession sqlSession = factory.openSession()) {
            Phase2UserMapper mapper = sqlSession.getMapper(Phase2UserMapper.class);
            Phase2User user = mapper.selectById(1L);

            assertNotNull(user);
            assertEquals(1L, user.getId());
            assertEquals("alice", user.getUserName());
        }
    }

    @Test
    void shouldQueryWithBeanParameter() throws Exception {
        SqlSessionFactory factory = phase2Factory(baseDataSource(), true);

        try (SqlSession sqlSession = factory.openSession()) {
            Phase2UserMapper mapper = sqlSession.getMapper(Phase2UserMapper.class);
            List<Phase2User> users = mapper.selectByFilter(new Phase2UserFilter(1L, "alice"));

            assertEquals(1, users.size());
            assertEquals("alice", users.get(0).getUserName());
        }
    }

    @Test
    void shouldQueryWithMultipleParameters() throws Exception {
        SqlSessionFactory factory = phase2Factory(baseDataSource(), true);

        try (SqlSession sqlSession = factory.openSession()) {
            Phase2UserMapper mapper = sqlSession.getMapper(Phase2UserMapper.class);
            List<Phase2User> users = mapper.selectByUsernameAndEmail("alice", "alice@example.com");

            assertEquals(1, users.size());
            assertEquals("alice@example.com", users.get(0).getEmail());
        }
    }

    @Test
    void shouldMapUnderscoreToCamelCaseWhenEnabled() throws Exception {
        SqlSessionFactory factory = phase2Factory(baseDataSource(), true);

        try (SqlSession sqlSession = factory.openSession()) {
            Phase2UserMapper mapper = sqlSession.getMapper(Phase2UserMapper.class);
            List<Phase2User> users = mapper.selectAllCamelCase();

            assertEquals(3, users.size());
            assertEquals("alice", users.get(0).getUserName());
        }
    }

    @Test
    void shouldLeaveCamelCasePropertyUnsetWhenDisabled() throws Exception {
        SqlSessionFactory factory = phase2Factory(baseDataSource(), false);

        try (SqlSession sqlSession = factory.openSession()) {
            Phase2UserMapper mapper = sqlSession.getMapper(Phase2UserMapper.class);
            List<Phase2User> users = mapper.selectAllCamelCase();

            assertEquals(3, users.size());
            assertNull(users.get(0).getUserName());
        }
    }

    @Test
    void shouldFallbackToFieldWriteWhenNoSetterExists() throws Exception {
        Configuration configuration = newConfiguration(baseDataSource(), true);
        loadMapper(configuration, "mapper/phase2-error-mapper.xml");
        configuration.addMapper(Phase2ErrorMapper.class);
        SqlSessionFactory factory = new DefaultSqlSessionFactory(configuration);

        try (SqlSession sqlSession = factory.openSession()) {
            Phase2ErrorMapper mapper = sqlSession.getMapper(Phase2ErrorMapper.class);
            List<FieldOnlyUser> users = mapper.selectFieldOnly();

            assertEquals(3, users.size());
            assertEquals("alice", users.get(0).getUserName());
        }
    }

    @Test
    void shouldThrowWhenParameterNameMissing() throws Exception {
        Configuration configuration = newConfiguration(baseDataSource(), true);
        loadMapper(configuration, "mapper/phase2-error-mapper.xml");
        configuration.addMapper(Phase2ErrorMapper.class);
        SqlSessionFactory factory = new DefaultSqlSessionFactory(configuration);

        try (SqlSession sqlSession = factory.openSession()) {
            Phase2ErrorMapper mapper = sqlSession.getMapper(Phase2ErrorMapper.class);
            ExecutorException ex = assertThrows(ExecutorException.class, () -> mapper.selectMissingParameter(1L));

            assertTrue(ex.getMessage().contains("statementId=com.xujn.minimybatis.support.Phase2ErrorMapper.selectMissingParameter"));
            assertTrue(ex.getMessage().contains("Parameter name not found: missingId"));
        }
    }

    @Test
    void shouldThrowWhenResultMappingFails() throws Exception {
        Configuration configuration = newConfiguration(baseDataSource(), true);
        loadMapper(configuration, "mapper/invalid-result-mapper.xml");
        configuration.addMapper(com.xujn.minimybatis.examples.phase1.UserMapper.class);
        SqlSessionFactory factory = new DefaultSqlSessionFactory(configuration);

        try (SqlSession sqlSession = factory.openSession()) {
            ExecutorException ex = assertThrows(
                    ExecutorException.class,
                    () -> sqlSession.selectOne("com.xujn.minimybatis.examples.phase1.UserMapper.selectBroken"));

            assertTrue(ex.getMessage().contains("resultType=" + NoDefaultConstructorUser.class.getName()));
            assertTrue(ex.getMessage().contains("sql=select id, username, email from users where id = 1"));
        }
    }

    @Test
    void shouldCloseResourcesOnBindingFailure() throws Exception {
        CountingDataSource dataSource = new CountingDataSource(baseDataSource());
        Configuration configuration = newConfiguration(dataSource, true);
        loadMapper(configuration, "mapper/phase2-error-mapper.xml");
        configuration.addMapper(Phase2ErrorMapper.class);
        SqlSessionFactory factory = new DefaultSqlSessionFactory(configuration);

        try (SqlSession sqlSession = factory.openSession()) {
            Phase2ErrorMapper mapper = sqlSession.getMapper(Phase2ErrorMapper.class);
            assertThrows(ExecutorException.class, () -> mapper.selectMissingParameter(1L));
        }

        assertEquals(1, dataSource.getConnectionClosedCount());
        assertEquals(1, dataSource.getStatementClosedCount());
        assertEquals(0, dataSource.getResultSetClosedCount());
    }

    @Test
    void shouldCloseResourcesOnSuccess() throws Exception {
        CountingDataSource dataSource = new CountingDataSource(baseDataSource());
        SqlSessionFactory factory = phase2Factory(dataSource, true);

        try (SqlSession sqlSession = factory.openSession()) {
            Phase2UserMapper mapper = sqlSession.getMapper(Phase2UserMapper.class);
            mapper.selectByUsernameAndEmail("alice", "alice@example.com");
        }

        assertEquals(1, dataSource.getConnectionClosedCount());
        assertEquals(1, dataSource.getStatementClosedCount());
        assertEquals(1, dataSource.getResultSetClosedCount());
    }

    private SqlSessionFactory phase2Factory(javax.sql.DataSource dataSource, boolean underscoreToCamelCase)
            throws Exception {
        Configuration configuration = newConfiguration(dataSource, underscoreToCamelCase);
        loadMapper(configuration, "mapper/phase2-user-mapper.xml");
        configuration.addMapper(Phase2UserMapper.class);
        return new DefaultSqlSessionFactory(configuration);
    }

    private Configuration newConfiguration(javax.sql.DataSource dataSource, boolean underscoreToCamelCase) {
        Configuration configuration = new Configuration();
        configuration.setDataSource(dataSource);
        configuration.setMapUnderscoreToCamelCase(underscoreToCamelCase);
        return configuration;
    }

    private void loadMapper(Configuration configuration, String mapperResource) throws Exception {
        try (InputStream inputStream = resource(mapperResource)) {
            new XmlMapperBuilder(configuration, inputStream, mapperResource).parse();
        }
    }

    private JdbcDataSource baseDataSource() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + System.nanoTime() + ";MODE=MYSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        executeSchema(dataSource);
        return dataSource;
    }

    private void executeSchema(JdbcDataSource dataSource) throws Exception {
        String sqlScript = readResource("schema.sql");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : sqlScript.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        }
    }

    private InputStream resource(String path) {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            throw new IllegalStateException("Resource not found: " + path);
        }
        return inputStream;
    }

    private String readResource(String path) throws IOException {
        try (InputStream inputStream = resource(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
