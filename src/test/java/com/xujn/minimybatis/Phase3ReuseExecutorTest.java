package com.xujn.minimybatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xujn.minimybatis.builder.xml.XmlMapperBuilder;
import com.xujn.minimybatis.exceptions.ExecutorException;
import com.xujn.minimybatis.examples.phase3.Phase3User;
import com.xujn.minimybatis.examples.phase3.Phase3UserMapper;
import com.xujn.minimybatis.executor.Executor;
import com.xujn.minimybatis.executor.ExecutorType;
import com.xujn.minimybatis.executor.ReuseExecutor;
import com.xujn.minimybatis.session.Configuration;
import com.xujn.minimybatis.session.SqlSession;
import com.xujn.minimybatis.session.SqlSessionFactory;
import com.xujn.minimybatis.session.defaults.DefaultSqlSessionFactory;
import com.xujn.minimybatis.support.CountingDataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

/**
 * Phase 3 acceptance coverage for executor extension and statement reuse.
 *
 * <p>Responsibility: verify default compatibility, statement reuse boundaries
 * and resource cleanup when {@code ReuseExecutor} is enabled.
 *
 * <p>Thread-safety: test class is stateless; each test creates its own
 * configuration and data source.
 */
class Phase3ReuseExecutorTest {

    @Test
    void shouldKeepSimpleExecutorAsDefault() throws Exception {
        SqlSessionFactory factory = reuseFactory(new CountingDataSource(baseDataSource()), ExecutorType.SIMPLE);

        try (SqlSession sqlSession = factory.openSession()) {
            Phase3UserMapper mapper = sqlSession.getMapper(Phase3UserMapper.class);
            Phase3User first = mapper.selectById(1L);
            Phase3User second = mapper.selectById(1L);

            assertNotNull(first);
            assertNotNull(second);
            assertEquals("alice", first.getUsername());
        }
    }

    @Test
    void shouldReusePreparedStatementWithinSameSession() throws Exception {
        CountingDataSource dataSource = new CountingDataSource(baseDataSource());
        SqlSessionFactory factory = reuseFactory(dataSource, ExecutorType.REUSE);

        try (SqlSession sqlSession = factory.openSession()) {
            Phase3UserMapper mapper = sqlSession.getMapper(Phase3UserMapper.class);
            mapper.selectById(1L);
            mapper.selectById(1L);
        }

        assertEquals(1, dataSource.getPreparedStatementCreatedCount());
        assertEquals(1, dataSource.getStatementClosedCount());
        assertEquals(2, dataSource.getResultSetClosedCount());
        assertEquals(1, dataSource.getConnectionClosedCount());
    }

    @Test
    void shouldCreateDifferentStatementsForDifferentSqlInSameSession() throws Exception {
        CountingDataSource dataSource = new CountingDataSource(baseDataSource());
        SqlSessionFactory factory = reuseFactory(dataSource, ExecutorType.REUSE);

        try (SqlSession sqlSession = factory.openSession()) {
            Phase3UserMapper mapper = sqlSession.getMapper(Phase3UserMapper.class);
            mapper.selectById(1L);
            mapper.selectAll();
        }

        assertEquals(2, dataSource.getPreparedStatementCreatedCount());
        assertEquals(2, dataSource.getStatementClosedCount());
        assertEquals(2, dataSource.getResultSetClosedCount());
    }

    @Test
    void shouldNotReusePreparedStatementAcrossSessions() throws Exception {
        CountingDataSource dataSource = new CountingDataSource(baseDataSource());
        SqlSessionFactory factory = reuseFactory(dataSource, ExecutorType.REUSE);

        try (SqlSession first = factory.openSession()) {
            first.getMapper(Phase3UserMapper.class).selectById(1L);
        }
        try (SqlSession second = factory.openSession()) {
            second.getMapper(Phase3UserMapper.class).selectById(1L);
        }

        assertEquals(2, dataSource.getPreparedStatementCreatedCount());
        assertEquals(2, dataSource.getStatementClosedCount());
        assertEquals(2, dataSource.getConnectionClosedCount());
    }

    @Test
    void shouldRecreateClosedCachedStatement() throws Exception {
        CountingDataSource dataSource = new CountingDataSource(baseDataSource());
        SqlSessionFactory factory = reuseFactory(dataSource, ExecutorType.REUSE);

        try (SqlSession sqlSession = factory.openSession()) {
            Phase3UserMapper mapper = sqlSession.getMapper(Phase3UserMapper.class);
            mapper.selectById(1L);

            ReuseExecutor executor = (ReuseExecutor) extractExecutor(sqlSession);
            PreparedStatement cachedStatement = extractSingleCachedStatement(executor);
            cachedStatement.close();

            mapper.selectById(1L);
        }

        assertEquals(2, dataSource.getPreparedStatementCreatedCount());
        assertEquals(2, dataSource.getStatementClosedCount());
        assertEquals(2, dataSource.getResultSetClosedCount());
    }

    @Test
    void shouldEvictFailedCachedStatementAndRecreateNextQuery() throws Exception {
        CountingDataSource dataSource = new CountingDataSource(baseDataSource());
        SqlSessionFactory factory = reuseFactory(dataSource, ExecutorType.REUSE);

        try (SqlSession sqlSession = factory.openSession()) {
            Phase3UserMapper mapper = sqlSession.getMapper(Phase3UserMapper.class);
            mapper.selectById(1L);

            ReuseExecutor executor = (ReuseExecutor) extractExecutor(sqlSession);
            replaceCachedStatement(executor, failingExecuteQueryStatement(extractSingleCachedStatement(executor)));

            assertThrows(ExecutorException.class, () -> mapper.selectById(1L));

            Phase3User recovered = mapper.selectById(1L);
            assertNotNull(recovered);
            assertEquals("alice", recovered.getUsername());
        }

        assertEquals(2, dataSource.getPreparedStatementCreatedCount());
        assertEquals(2, dataSource.getStatementClosedCount());
        assertEquals(2, dataSource.getResultSetClosedCount());
    }

    @Test
    void shouldIncludeStatementContextWhenCloseFails() throws Exception {
        SqlSessionFactory factory = reuseFactory(baseDataSource(), ExecutorType.REUSE);
        SqlSession sqlSession = factory.openSession();
        Phase3UserMapper mapper = sqlSession.getMapper(Phase3UserMapper.class);
        mapper.selectById(1L);

        ReuseExecutor executor = (ReuseExecutor) extractExecutor(sqlSession);
        replaceCachedStatement(executor, failingCloseStatement(extractSingleCachedStatement(executor)));

        ExecutorException ex = assertThrows(ExecutorException.class, sqlSession::close);

        assertTrue(ex.getMessage().contains("statementId=com.xujn.minimybatis.examples.phase3.Phase3UserMapper.selectById"));
        assertTrue(ex.getMessage().contains("resource=mapper/phase3-reuse-mapper.xml"));
        assertTrue(ex.getMessage().contains("sql=select id, username, email from users where id = ?"));
        assertTrue(ex.getMessage().contains("executorType=REUSE"));
    }

    private SqlSessionFactory reuseFactory(javax.sql.DataSource dataSource, ExecutorType executorType)
            throws Exception {
        Configuration configuration = newConfiguration(dataSource, executorType);
        loadMapper(configuration, "mapper/phase3-reuse-mapper.xml");
        configuration.addMapper(Phase3UserMapper.class);
        return new DefaultSqlSessionFactory(configuration);
    }

    private Configuration newConfiguration(javax.sql.DataSource dataSource, ExecutorType executorType) {
        Configuration configuration = new Configuration();
        configuration.setDataSource(dataSource);
        configuration.setDefaultExecutorType(executorType);
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

    private Executor extractExecutor(SqlSession sqlSession) throws Exception {
        Field field = sqlSession.getClass().getDeclaredField("executor");
        field.setAccessible(true);
        return (Executor) field.get(sqlSession);
    }

    @SuppressWarnings("unchecked")
    private PreparedStatement extractSingleCachedStatement(ReuseExecutor executor) throws Exception {
        Field field = ReuseExecutor.class.getDeclaredField("statementCache");
        field.setAccessible(true);
        Map<Object, PreparedStatement> cache = (Map<Object, PreparedStatement>) field.get(executor);
        assertEquals(1, cache.size());
        return cache.values().iterator().next();
    }

    @SuppressWarnings("unchecked")
    private void replaceCachedStatement(ReuseExecutor executor, PreparedStatement replacement) throws Exception {
        Field field = ReuseExecutor.class.getDeclaredField("statementCache");
        field.setAccessible(true);
        Map<Object, PreparedStatement> cache = (Map<Object, PreparedStatement>) field.get(executor);
        assertEquals(1, cache.size());
        Object cacheKey = cache.keySet().iterator().next();
        cache.put(cacheKey, replacement);
    }

    private PreparedStatement failingExecuteQueryStatement(PreparedStatement delegate) {
        InvocationHandler handler = new FaultyPreparedStatementHandler(delegate, true, false);
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[] {PreparedStatement.class},
                handler);
    }

    private PreparedStatement failingCloseStatement(PreparedStatement delegate) {
        InvocationHandler handler = new FaultyPreparedStatementHandler(delegate, false, true);
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[] {PreparedStatement.class},
                handler);
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

    private static final class FaultyPreparedStatementHandler implements InvocationHandler {

        private final PreparedStatement delegate;
        private final boolean failExecuteQuery;
        private final boolean failClose;

        private FaultyPreparedStatementHandler(
                PreparedStatement delegate,
                boolean failExecuteQuery,
                boolean failClose) {
            this.delegate = delegate;
            this.failExecuteQuery = failExecuteQuery;
            this.failClose = failClose;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(delegate, args);
            }
            if (failExecuteQuery && "executeQuery".equals(method.getName()) && (args == null || args.length == 0)) {
                throw new SQLException("Injected executeQuery failure");
            }
            if (failClose && "close".equals(method.getName())) {
                throw new SQLException("Injected close failure");
            }
            return method.invoke(delegate, args);
        }
    }
}
