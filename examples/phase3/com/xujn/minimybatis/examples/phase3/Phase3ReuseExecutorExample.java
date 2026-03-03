package com.xujn.minimybatis.examples.phase3;

import com.xujn.minimybatis.builder.xml.XmlMapperBuilder;
import com.xujn.minimybatis.executor.ExecutorType;
import com.xujn.minimybatis.session.Configuration;
import com.xujn.minimybatis.session.SqlSession;
import com.xujn.minimybatis.session.SqlSessionFactory;
import com.xujn.minimybatis.session.defaults.DefaultSqlSessionFactory;
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

/**
 * Runnable example for phase 3 executor reuse behavior.
 *
 * <p>Responsibility: show that enabling {@code REUSE} keeps mapper APIs and
 * query results unchanged while allowing repeated same-session queries.
 *
 * <p>Thread-safety: single-threaded demo entrypoint.
 */
public final class Phase3ReuseExecutorExample {

    private Phase3ReuseExecutorExample() {
    }

    public static void main(String[] args) throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:mini_mybatis_phase3;MODE=MYSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        executeSchema(dataSource);

        Configuration configuration = new Configuration();
        configuration.setDataSource(dataSource);
        configuration.setDefaultExecutorType(ExecutorType.REUSE);
        try (InputStream mapperStream = resource("mapper/phase3-user-mapper.xml")) {
            new XmlMapperBuilder(configuration, mapperStream, "mapper/phase3-user-mapper.xml").parse();
        }
        configuration.addMapper(Phase3UserMapper.class);

        SqlSessionFactory sqlSessionFactory = new DefaultSqlSessionFactory(configuration);
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            Phase3UserMapper mapper = sqlSession.getMapper(Phase3UserMapper.class);
            Phase3User first = mapper.selectById(1L);
            Phase3User second = mapper.selectById(1L);
            List<Phase3User> all = mapper.selectAll();

            if (first == null || second == null || all.size() != 3) {
                throw new IllegalStateException("Phase 3 example verification failed");
            }

            System.out.println("phase3 executorType -> " + configuration.getDefaultExecutorType());
            System.out.println("phase3 first query -> " + first);
            System.out.println("phase3 second query -> " + second);
            System.out.println("phase3 selectAll size -> " + all.size());
        }
    }

    private static void executeSchema(JdbcDataSource dataSource) throws Exception {
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

    private static InputStream resource(String path) {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            throw new IllegalStateException("Resource not found: " + path);
        }
        return inputStream;
    }

    private static String readResource(String path) throws IOException {
        try (InputStream inputStream = resource(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
