package com.xujn.minimybatis.session;

import com.xujn.minimybatis.binding.MapperRegistry;
import com.xujn.minimybatis.exceptions.MappingException;
import com.xujn.minimybatis.mapping.MappedStatement;
import com.xujn.minimybatis.support.ErrorContext;
import com.xujn.minimybatis.support.ExceptionFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Central registry for runtime configuration and mapped statements.
 *
 * <p>Responsibility: keep framework metadata in one place so sessions,
 * executors and mappers all resolve the same statement definitions.
 *
 * <p>Thread-safety: configuration is safe for concurrent reads after startup.
 * Mutating it at runtime is not supported.
 */
public class Configuration {

    private final Map<String, MappedStatement> mappedStatements = new LinkedHashMap<>();
    private final MapperRegistry mapperRegistry = new MapperRegistry(this);
    private DataSource dataSource;
    private boolean mapUnderscoreToCamelCase;

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void addMappedStatement(MappedStatement mappedStatement) {
        MappedStatement existing = mappedStatements.get(mappedStatement.getStatementId());
        if (existing != null) {
            throw ExceptionFactory.builderException(
                    "Duplicate statementId detected; existingResource=" + existing.getResource()
                            + ", newResource=" + mappedStatement.getResource(),
                    ErrorContext.create()
                            .statementId(mappedStatement.getStatementId())
                            .resource(mappedStatement.getResource())
                            .sql(mappedStatement.getSqlSource().getSql())
                            .resultType(mappedStatement.getResultType()));
        }
        mappedStatements.put(mappedStatement.getStatementId(), mappedStatement);
    }

    public boolean hasStatement(String statementId) {
        return mappedStatements.containsKey(statementId);
    }

    public MappedStatement getMappedStatement(String statementId) {
        MappedStatement mappedStatement = mappedStatements.get(statementId);
        if (mappedStatement == null) {
            throw ExceptionFactory.mappingException(
                    "MappedStatement not found",
                    ErrorContext.create().statementId(statementId));
        }
        return mappedStatement;
    }

    public Map<String, MappedStatement> getMappedStatements() {
        return mappedStatements;
    }

    public <T> void addMapper(Class<T> type) {
        mapperRegistry.addMapper(type);
    }

    public <T> T getMapper(Class<T> type, SqlSession sqlSession) throws MappingException {
        return mapperRegistry.getMapper(type, sqlSession);
    }

    public MapperRegistry getMapperRegistry() {
        return mapperRegistry;
    }

    public boolean isMapUnderscoreToCamelCase() {
        return mapUnderscoreToCamelCase;
    }

    public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
        this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
    }
}
