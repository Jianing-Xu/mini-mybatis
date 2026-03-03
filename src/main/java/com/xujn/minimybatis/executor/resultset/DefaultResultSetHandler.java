package com.xujn.minimybatis.executor.resultset;

import com.xujn.minimybatis.mapping.BoundSql;
import com.xujn.minimybatis.mapping.MappedStatement;
import com.xujn.minimybatis.reflection.MetaObject;
import com.xujn.minimybatis.reflection.ObjectFactory;
import com.xujn.minimybatis.session.Configuration;
import com.xujn.minimybatis.support.ErrorContext;
import com.xujn.minimybatis.support.ExceptionFactory;
import com.xujn.minimybatis.support.JdbcUtils;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Default result mapping implementation for phase 2.
 *
 * <p>Responsibility: map simple scalar types and JavaBeans, optionally applying
 * underscore-to-camel-case conversion before property lookup.
 *
 * <p>Thread-safety: not thread-safe; created per execution.
 */
public class DefaultResultSetHandler implements ResultSetHandler {

    private final Configuration configuration;
    private final MappedStatement mappedStatement;
    private final BoundSql boundSql;
    private final ObjectFactory objectFactory;

    public DefaultResultSetHandler(
            Configuration configuration,
            MappedStatement mappedStatement,
            BoundSql boundSql,
            ObjectFactory objectFactory) {
        this.configuration = configuration;
        this.mappedStatement = mappedStatement;
        this.boundSql = boundSql;
        this.objectFactory = objectFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> List<E> handleResultSets(PreparedStatement statement) {
        List<E> results = new ArrayList<>();
        Class<?> resultType = mappedStatement.getResultType();
        try (ResultSet resultSet = statement.executeQuery()) {
            if (JdbcUtils.isSimpleType(resultType)) {
                while (resultSet.next()) {
                    results.add((E) JdbcUtils.getColumnValue(resultSet, 1, resultType));
                }
                return results;
            }

            while (resultSet.next()) {
                results.add((E) mapBean(resultSet, resultType));
            }
            return results;
        } catch (SQLException ex) {
            throw ExceptionFactory.executorException(
                    "Failed to read result set",
                    context(),
                    ex);
        } catch (ReflectiveOperationException ex) {
            throw ExceptionFactory.executorException(
                    "Failed to map result object",
                    context(),
                    ex);
        }
    }

    private Object mapBean(ResultSet resultSet, Class<?> resultType) throws ReflectiveOperationException, SQLException {
        Object target = objectFactory.create(resultType);
        MetaObject metaObject = new MetaObject(target);
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String propertyName = resolvePropertyName(metaData.getColumnLabel(i));
            if (!metaObject.hasSetter(propertyName)) {
                continue;
            }
            Class<?> propertyType = metaObject.getSetterType(propertyName);
            Object value = JdbcUtils.convertValue(resultSet.getObject(i), propertyType);
            metaObject.setValue(propertyName, value);
        }
        return target;
    }

    private String resolvePropertyName(String columnLabel) {
        if (!configuration.isMapUnderscoreToCamelCase()) {
            return columnLabel;
        }
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (char ch : columnLabel.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            builder.append(upperNext ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
            upperNext = false;
        }
        return builder.toString();
    }

    private ErrorContext context() {
        return ErrorContext.create()
                .statementId(mappedStatement.getStatementId())
                .resource(mappedStatement.getResource())
                .sql(boundSql.getSql())
                .parameter(boundSql.getParameterObject())
                .resultType(mappedStatement.getResultType());
    }
}
