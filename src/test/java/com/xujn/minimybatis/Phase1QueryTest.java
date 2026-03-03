package com.xujn.minimybatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xujn.minimybatis.builder.xml.XmlMapperBuilder;
import com.xujn.minimybatis.exceptions.ExecutorException;
import com.xujn.minimybatis.exceptions.MappingException;
import com.xujn.minimybatis.examples.phase1.User;
import com.xujn.minimybatis.examples.phase1.UserMapper;
import com.xujn.minimybatis.session.Configuration;
import com.xujn.minimybatis.session.SqlSession;
import com.xujn.minimybatis.session.SqlSessionFactory;
import com.xujn.minimybatis.session.defaults.DefaultSqlSessionFactory;
import com.xujn.minimybatis.support.CountingDataSource;
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
 * Phase 1 acceptance coverage for the core query pipeline.
 *
 * <p>Responsibility: verify XML parsing, mapper proxy routing, result mapping
 * and resource cleanup against the documented acceptance criteria.
 *
 * <p>Thread-safety: test class is stateless; each test creates its own
 * configuration and data source.
 */
class Phase1QueryTest {

    @Test
    void shouldLoadMappedStatementsFromXml() throws Exception {
        Configuration configuration = newConfiguration(baseDataSource());
        loadMapper(configuration, "mapper/user-mapper.xml");

        assertTrue(configuration.hasStatement("com.xujn.minimybatis.examples.phase1.UserMapper.selectById"));
        assertTrue(configuration.hasStatement("com.xujn.minimybatis.examples.phase1.UserMapper.selectAll"));
    }

    @Test
    void shouldQuerySingleObjectViaMapperProxy() throws Exception {
        SqlSessionFactory factory = sqlSessionFactory(baseDataSource(), "mapper/user-mapper.xml");

        try (SqlSession sqlSession = factory.openSession()) {
            UserMapper mapper = sqlSession.getMapper(UserMapper.class);
            User user = mapper.selectById(1L);

            assertNotNull(user);
            assertEquals(1L, user.getId());
            assertEquals("alice", user.getUsername());
            assertEquals("alice@example.com", user.getEmail());
        }
    }

    @Test
    void shouldQueryListViaSqlSession() throws Exception {
        SqlSessionFactory factory = sqlSessionFactory(baseDataSource(), "mapper/user-mapper.xml");

        try (SqlSession sqlSession = factory.openSession()) {
            List<User> users = sqlSession.selectList("com.xujn.minimybatis.examples.phase1.UserMapper.selectAll");

            assertEquals(3, users.size());
            assertEquals("alice", users.get(0).getUsername());
        }
    }

    @Test
    void shouldReturnSameProxyInstancePerSessionLookup() throws Exception {
        SqlSessionFactory factory = sqlSessionFactory(baseDataSource(), "mapper/user-mapper.xml");

        try (SqlSession sqlSession = factory.openSession()) {
            UserMapper first = sqlSession.getMapper(UserMapper.class);
            UserMapper second = sqlSession.getMapper(UserMapper.class);

            assertNotNull(first);
            assertNotNull(second);
            assertEquals(first.getClass(), second.getClass());
        }
    }

    @Test
    void shouldThrowWhenStatementMissing() throws Exception {
        SqlSessionFactory factory = sqlSessionFactory(baseDataSource(), "mapper/user-mapper.xml");

        try (SqlSession sqlSession = factory.openSession()) {
            MappingException ex = assertThrows(
                    MappingException.class,
                    () -> sqlSession.selectOne("missing.statement"));

            assertTrue(ex.getMessage().contains("statementId=missing.statement"));
        }
    }

    @Test
    void shouldThrowWhenDuplicateStatementId() throws Exception {
        Configuration configuration = newConfiguration(baseDataSource());
        loadMapper(configuration, "mapper/user-mapper.xml");

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> loadMapper(configuration, "mapper/duplicate-user-mapper.xml"));

        assertTrue(ex.getMessage().contains("selectAll"));
        assertTrue(ex.getMessage().contains("existingResource=mapper/user-mapper.xml"));
        assertTrue(ex.getMessage().contains("newResource=mapper/duplicate-user-mapper.xml"));
    }

    @Test
    void shouldThrowWhenResultMappingFails() throws Exception {
        SqlSessionFactory factory = sqlSessionFactory(baseDataSource(), "mapper/invalid-result-mapper.xml");

        try (SqlSession sqlSession = factory.openSession()) {
            ExecutorException ex = assertThrows(
                    ExecutorException.class,
                    () -> sqlSession.selectOne("com.xujn.minimybatis.examples.phase1.UserMapper.selectBroken"));

            assertTrue(ex.getMessage().contains("statementId=com.xujn.minimybatis.examples.phase1.UserMapper.selectBroken"));
            assertTrue(ex.getMessage().contains("resultType=com.xujn.minimybatis.support.NoDefaultConstructorUser"));
            assertTrue(ex.getMessage().contains("sql=select id, username, email from users where id = 1"));
        }
    }

    @Test
    void shouldThrowWhenMultipleParametersPassedToMapper() throws Exception {
        SqlSessionFactory factory = sqlSessionFactory(baseDataSource(), "mapper/user-mapper.xml");

        try (SqlSession sqlSession = factory.openSession()) {
            sqlSession.getConfiguration().addMapper(InvalidUserMapper.class);
            InvalidUserMapper mapper = sqlSession.getMapper(InvalidUserMapper.class);

            MappingException ex = assertThrows(
                    MappingException.class,
                    () -> mapper.find(1L, "alice@example.com"));

            assertTrue(ex.getMessage().contains("statementId=" + InvalidUserMapper.class.getName() + ".find"));
            assertTrue(ex.getMessage().contains("parameter=[1, alice@example.com] method=find"));
        }
    }

    @Test
    void shouldReturnNullWhenNoRowFound() throws Exception {
        SqlSessionFactory factory = sqlSessionFactory(baseDataSource(), "mapper/user-mapper.xml");

        try (SqlSession sqlSession = factory.openSession()) {
            UserMapper mapper = sqlSession.getMapper(UserMapper.class);
            User user = mapper.selectById(99L);

            assertNull(user);
        }
    }

    @Test
    void shouldCloseResourcesOnSuccess() throws Exception {
        CountingDataSource dataSource = new CountingDataSource(baseDataSource());
        SqlSessionFactory factory = sqlSessionFactory(dataSource, "mapper/user-mapper.xml");

        try (SqlSession sqlSession = factory.openSession()) {
            UserMapper mapper = sqlSession.getMapper(UserMapper.class);
            mapper.selectById(1L);
        }

        assertEquals(1, dataSource.getConnectionClosedCount());
        assertEquals(1, dataSource.getStatementClosedCount());
        assertEquals(1, dataSource.getResultSetClosedCount());
    }

    @Test
    void shouldCloseResourcesOnFailure() throws Exception {
        CountingDataSource dataSource = new CountingDataSource(baseDataSource());
        SqlSessionFactory factory = sqlSessionFactory(dataSource, "mapper/invalid-result-mapper.xml");

        try (SqlSession sqlSession = factory.openSession()) {
            assertThrows(
                    ExecutorException.class,
                    () -> sqlSession.selectOne("com.xujn.minimybatis.examples.phase1.UserMapper.selectBroken"));
        }

        assertEquals(1, dataSource.getConnectionClosedCount());
        assertEquals(1, dataSource.getStatementClosedCount());
        assertEquals(1, dataSource.getResultSetClosedCount());
    }

    private SqlSessionFactory sqlSessionFactory(javax.sql.DataSource dataSource, String mapperResource) throws Exception {
        Configuration configuration = newConfiguration(dataSource);
        loadMapper(configuration, mapperResource);
        configuration.addMapper(UserMapper.class);
        return new DefaultSqlSessionFactory(configuration);
    }

    private Configuration newConfiguration(javax.sql.DataSource dataSource) {
        Configuration configuration = new Configuration();
        configuration.setDataSource(dataSource);
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

    interface InvalidUserMapper {
        User find(Long id, String email);
    }
}
