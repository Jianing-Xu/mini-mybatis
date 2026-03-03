package com.xujn.minimybatis.examples.phase1;

import com.xujn.minimybatis.builder.xml.XmlMapperBuilder;
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
 * Runnable example that exercises the complete phase 1 query pipeline.
 *
 * <p>Responsibility: provide a manual acceptance entrypoint that bootstraps
 * H2, loads XML statements, opens a session and prints deterministic results.
 *
 * <p>Thread-safety: single-threaded demo entrypoint.
 */
public final class Phase1QueryExample {

    private Phase1QueryExample() {
    }

    public static void main(String[] args) throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:mini_mybatis_phase1;MODE=MYSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        executeSchema(dataSource);

        Configuration configuration = new Configuration();
        configuration.setDataSource(dataSource);
        try (InputStream mapperStream = resource("mapper/user-mapper.xml")) {
            new XmlMapperBuilder(configuration, mapperStream, "mapper/user-mapper.xml").parse();
        }
        configuration.addMapper(UserMapper.class);

        SqlSessionFactory sqlSessionFactory = new DefaultSqlSessionFactory(configuration);
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            User user = userMapper.selectById(1L);
            List<User> users = userMapper.selectAll();

            if (user == null || users.size() != 3) {
                throw new IllegalStateException("Example verification failed");
            }

            System.out.println("selectById -> " + user);
            System.out.println("selectAll size -> " + users.size());
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
