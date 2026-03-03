package com.xujn.minimybatis.executor;

import com.xujn.minimybatis.mapping.MappedStatement;
import com.xujn.minimybatis.session.Configuration;
import com.xujn.minimybatis.support.ErrorContext;
import com.xujn.minimybatis.support.ExceptionFactory;
import com.xujn.minimybatis.support.JdbcUtils;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Direct JDBC executor for the phase 1 query pipeline.
 *
 * <p>Responsibility: open a connection, bind the minimal supported parameter
 * model, execute the statement and map the result set.
 *
 * <p>Thread-safety: not thread-safe; use one executor per session.
 */
public class SimpleExecutor implements Executor {

    private final Configuration configuration;
    private boolean closed;

    public SimpleExecutor(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <E> List<E> query(MappedStatement mappedStatement, Object parameter) {
        ensureOpen(mappedStatement, parameter);
        DataSource dataSource = configuration.getDataSource();
        if (dataSource == null) {
            throw ExceptionFactory.executorException(
                    "DataSource is not configured",
                    buildContext(mappedStatement, parameter));
        }

        String sql = mappedStatement.getSqlSource().getSql();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameter(statement, sql, parameter, mappedStatement);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapResults(resultSet, mappedStatement, parameter);
            }
        } catch (SQLException ex) {
            throw ExceptionFactory.persistenceException(
                    "Failed to execute query",
                    buildContext(mappedStatement, parameter),
                    ex);
        }
    }

    @Override
    public void close(boolean forceRollback) {
        closed = true;
    }

    private void ensureOpen(MappedStatement mappedStatement, Object parameter) {
        if (closed) {
            throw ExceptionFactory.executorException(
                    "Executor is already closed",
                    buildContext(mappedStatement, parameter));
        }
    }

    private void bindParameter(
            PreparedStatement statement,
            String sql,
            Object parameter,
            MappedStatement mappedStatement) throws SQLException {
        int placeholderCount = countPlaceholders(sql);
        if (placeholderCount > 1) {
            throw ExceptionFactory.executorException(
                    "SQL contains more than one placeholder in phase 1",
                    buildContext(mappedStatement, parameter));
        }
        if (placeholderCount == 0) {
            if (parameter != null) {
                throw ExceptionFactory.executorException(
                        "Parameter supplied for SQL without placeholders",
                        buildContext(mappedStatement, parameter));
            }
            return;
        }

        // Phase 1 keeps parameter binding explicit: exactly one '?' means
        // exactly one parameter object is passed through as-is.
        statement.setObject(1, parameter);
    }

    private int countPlaceholders(String sql) {
        int count = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private <E> List<E> mapResults(ResultSet resultSet, MappedStatement mappedStatement, Object parameter)
            throws SQLException {
        List<E> results = new ArrayList<>();
        Class<?> resultType = mappedStatement.getResultType();
        if (JdbcUtils.isSimpleType(resultType)) {
            while (resultSet.next()) {
                results.add((E) JdbcUtils.getColumnValue(resultSet, 1, resultType));
            }
            return results;
        }

        while (resultSet.next()) {
            results.add((E) mapBean(resultSet, mappedStatement, parameter));
        }
        return results;
    }

    private Object mapBean(ResultSet resultSet, MappedStatement mappedStatement, Object parameter) throws SQLException {
        try {
            Object target = instantiate(mappedStatement, parameter);
            Map<String, PropertyDescriptor> properties = propertyDescriptorMap(target.getClass());
            Map<String, Field> fields = fieldMap(target.getClass());
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i).toLowerCase(Locale.ROOT);
                Object value = resultSet.getObject(i);
                PropertyDescriptor descriptor = properties.get(columnName);
                if (descriptor != null && descriptor.getWriteMethod() != null) {
                    Class<?> propertyType = descriptor.getPropertyType();
                    descriptor.getWriteMethod().invoke(target, JdbcUtils.convertValue(value, propertyType));
                    continue;
                }
                Field field = fields.get(columnName);
                if (field != null) {
                    field.setAccessible(true);
                    field.set(target, JdbcUtils.convertValue(value, field.getType()));
                }
            }
            return target;
        } catch (ReflectiveOperationException | IntrospectionException ex) {
            throw ExceptionFactory.executorException(
                    "Failed to map result object",
                    buildContext(mappedStatement, parameter),
                    ex);
        }
    }

    private Object instantiate(MappedStatement mappedStatement, Object parameter)
            throws ReflectiveOperationException {
        Class<?> resultType = mappedStatement.getResultType();
        Constructor<?> constructor = resultType.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            return constructor.newInstance();
        } catch (InvocationTargetException ex) {
            throw ExceptionFactory.executorException(
                    "Failed to instantiate resultType",
                    buildContext(mappedStatement, parameter),
                    ex.getTargetException());
        }
    }

    private Map<String, PropertyDescriptor> propertyDescriptorMap(Class<?> type) throws IntrospectionException {
        Map<String, PropertyDescriptor> descriptors = new HashMap<>();
        for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
            descriptors.put(descriptor.getName().toLowerCase(Locale.ROOT), descriptor);
        }
        return descriptors;
    }

    private Map<String, Field> fieldMap(Class<?> type) {
        Map<String, Field> fields = new HashMap<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.putIfAbsent(field.getName().toLowerCase(Locale.ROOT), field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private ErrorContext buildContext(MappedStatement mappedStatement, Object parameter) {
        return ErrorContext.create()
                .statementId(mappedStatement.getStatementId())
                .resource(mappedStatement.getResource())
                .sql(mappedStatement.getSqlSource().getSql())
                .parameter(parameter)
                .resultType(mappedStatement.getResultType());
    }
}
