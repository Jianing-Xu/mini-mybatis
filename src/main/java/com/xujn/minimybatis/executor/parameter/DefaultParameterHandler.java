package com.xujn.minimybatis.executor.parameter;

import com.xujn.minimybatis.mapping.BoundSql;
import com.xujn.minimybatis.mapping.MappedStatement;
import com.xujn.minimybatis.mapping.ParameterMapping;
import com.xujn.minimybatis.reflection.MetaObject;
import com.xujn.minimybatis.support.ErrorContext;
import com.xujn.minimybatis.support.ExceptionFactory;
import com.xujn.minimybatis.support.JdbcUtils;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Default phase 2 parameter binder.
 *
 * <p>Responsibility: support simple values, named parameter maps and JavaBean
 * parameters without introducing a type-handler subsystem yet.
 *
 * <p>Thread-safety: not thread-safe; created per execution.
 */
public class DefaultParameterHandler implements ParameterHandler {

    private final MappedStatement mappedStatement;
    private final BoundSql boundSql;
    private final Object parameterObject;

    public DefaultParameterHandler(MappedStatement mappedStatement, BoundSql boundSql, Object parameterObject) {
        this.mappedStatement = mappedStatement;
        this.boundSql = boundSql;
        this.parameterObject = parameterObject;
    }

    @Override
    public Object getParameterObject() {
        return parameterObject;
    }

    @Override
    public void setParameters(PreparedStatement statement) {
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        if (mappings.isEmpty()) {
            if (parameterObject != null) {
                throw ExceptionFactory.executorException(
                        "Parameter supplied for SQL without parameter mappings",
                        context());
            }
            return;
        }

        try {
            for (int i = 0; i < mappings.size(); i++) {
                Object value = resolveValue(mappings.get(i).getProperty());
                statement.setObject(i + 1, value);
            }
        } catch (SQLException ex) {
            throw ExceptionFactory.executorException(
                    "Failed to bind JDBC parameters",
                    context(),
                    ex);
        } catch (ReflectiveOperationException ex) {
            throw ExceptionFactory.executorException(
                    "Failed to resolve parameter value",
                    context(),
                    ex);
        }
    }

    private Object resolveValue(String property) throws ReflectiveOperationException {
        if (parameterObject == null) {
            throw ExceptionFactory.executorException(
                    "Parameter value is missing for property " + property,
                    context());
        }
        if (parameterObject instanceof Map<?, ?> map) {
            if (!map.containsKey(property)) {
                throw ExceptionFactory.executorException(
                        "Parameter name not found: " + property,
                        context());
            }
            return map.get(property);
        }
        if (JdbcUtils.isSimpleType(parameterObject.getClass())) {
            return parameterObject;
        }
        MetaObject metaObject = new MetaObject(parameterObject);
        if (!metaObject.hasGetter(property)) {
            throw ExceptionFactory.executorException(
                    "Parameter name not found: " + property,
                    context());
        }
        return metaObject.getValue(property);
    }

    private ErrorContext context() {
        return ErrorContext.create()
                .statementId(mappedStatement.getStatementId())
                .resource(mappedStatement.getResource())
                .sql(boundSql.getSql())
                .parameter(parameterObject)
                .resultType(mappedStatement.getResultType());
    }
}
