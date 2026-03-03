package com.xujn.minimybatis.executor.statement;

import com.xujn.minimybatis.executor.parameter.DefaultParameterHandler;
import com.xujn.minimybatis.executor.parameter.ParameterHandler;
import com.xujn.minimybatis.executor.resultset.DefaultResultSetHandler;
import com.xujn.minimybatis.executor.resultset.ResultSetHandler;
import com.xujn.minimybatis.mapping.BoundSql;
import com.xujn.minimybatis.mapping.MappedStatement;
import com.xujn.minimybatis.reflection.ObjectFactory;
import com.xujn.minimybatis.session.Configuration;
import com.xujn.minimybatis.support.ErrorContext;
import com.xujn.minimybatis.support.ExceptionFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Statement handler backed by JDBC {@link PreparedStatement}.
 *
 * <p>Responsibility: wire the phase 2 parameter and result handlers around one
 * prepared statement lifecycle.
 *
 * <p>Thread-safety: not thread-safe; created per execution.
 */
public class PreparedStatementHandler implements StatementHandler {

    private final MappedStatement mappedStatement;
    private final BoundSql boundSql;
    private final ParameterHandler parameterHandler;
    private final ResultSetHandler resultSetHandler;

    public PreparedStatementHandler(
            Configuration configuration,
            MappedStatement mappedStatement,
            Object parameterObject,
            BoundSql boundSql) {
        this.mappedStatement = mappedStatement;
        this.boundSql = boundSql;
        this.parameterHandler = new DefaultParameterHandler(mappedStatement, boundSql, parameterObject);
        this.resultSetHandler =
                new DefaultResultSetHandler(configuration, mappedStatement, boundSql, new ObjectFactory());
    }

    @Override
    public PreparedStatement prepare(Connection connection) {
        try {
            return connection.prepareStatement(boundSql.getSql());
        } catch (SQLException ex) {
            throw ExceptionFactory.executorException(
                    "Failed to prepare statement",
                    context(),
                    ex);
        }
    }

    @Override
    public void parameterize(PreparedStatement statement) {
        parameterHandler.setParameters(statement);
    }

    @Override
    public <E> List<E> query(PreparedStatement statement) {
        return resultSetHandler.handleResultSets(statement);
    }

    @Override
    public BoundSql getBoundSql() {
        return boundSql;
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
