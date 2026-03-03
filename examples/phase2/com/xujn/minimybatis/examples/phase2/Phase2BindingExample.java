package com.xujn.minimybatis.examples.phase2;

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
 * Runnable example for phase 2 parameter binding and camel-case mapping.
 *
 * <p>Responsibility: show the new execution chain with bean parameters,
 * multiple mapper arguments and underscore-to-camel-case result mapping.
 *
 * <p>Thread-safety: single-threaded demo entrypoint.
 */
public final class Phase2BindingExample {

    private Phase2BindingExample() {
    }

    public static void main(String[] args) throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:mini_mybatis_phase2;MODE=MYSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        executeSchema(dataSource);

        Configuration configuration = new Configuration();
        configuration.setDataSource(dataSource);
        configuration.setMapUnderscoreToCamelCase(true);
        try (InputStream mapperStream = resource("mapper/phase2-user-mapper.xml")) {
            new XmlMapperBuilder(configuration, mapperStream, "mapper/phase2-user-mapper.xml").parse();
        }
        configuration.addMapper(Phase2UserMapper.class);

        SqlSessionFactory sqlSessionFactory = new DefaultSqlSessionFactory(configuration);
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            Phase2UserMapper mapper = sqlSession.getMapper(Phase2UserMapper.class);
            Phase2User single = mapper.selectById(1L);
            List<Phase2User> byMultiParam = mapper.selectByUsernameAndEmail("alice", "alice@example.com");
            List<Phase2User> byBean = mapper.selectByFilter(new Phase2UserFilter(1L, "alice"));

            if (single == null || byMultiParam.size() != 1 || byBean.size() != 1 || single.getUserName() == null) {
                throw new IllegalStateException("Phase 2 example verification failed");
            }

            System.out.println("phase2 selectById -> " + single);
            System.out.println("phase2 multiParam size -> " + byMultiParam.size());
            System.out.println("phase2 beanParam size -> " + byBean.size());
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
