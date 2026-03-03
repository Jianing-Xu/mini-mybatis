package com.xujn.minimybatis.builder.xml;

import com.xujn.minimybatis.mapping.MappedStatement;
import com.xujn.minimybatis.mapping.SqlCommandType;
import com.xujn.minimybatis.mapping.SqlSource;
import com.xujn.minimybatis.support.ErrorContext;
import com.xujn.minimybatis.support.ExceptionFactory;
import org.w3c.dom.Element;

/**
 * Parses a single {@code <select>} element into a mapped statement.
 *
 * <p>Responsibility: validate the element-level required attributes and keep
 * the XML builder focused on document traversal.
 *
 * <p>Thread-safety: immutable and thread-safe.
 */
public class XmlStatementParser {

    private final String namespace;
    private final String resource;

    public XmlStatementParser(String namespace, String resource) {
        this.namespace = namespace;
        this.resource = resource;
    }

    public MappedStatement parse(Element selectElement) {
        String id = selectElement.getAttribute("id");
        String resultTypeName = selectElement.getAttribute("resultType");
        if (id == null || id.isBlank()) {
            throw ExceptionFactory.builderException(
                    "Select id is required",
                    ErrorContext.create().resource(resource).mapper(resolveNamespaceClass()));
        }
        if (resultTypeName == null || resultTypeName.isBlank()) {
            throw ExceptionFactory.builderException(
                    "Select resultType is required",
                    ErrorContext.create().resource(resource).statementId(namespace + "." + id));
        }
        String sql = compressSql(selectElement.getTextContent());
        if (sql.isBlank()) {
            throw ExceptionFactory.builderException(
                    "Select SQL body is required",
                    ErrorContext.create().resource(resource).statementId(namespace + "." + id));
        }
        return new MappedStatement(
                id,
                namespace,
                new SqlSource(sql),
                SqlCommandType.SELECT,
                null,
                resolveClass(resultTypeName),
                resource);
    }

    private String compressSql(String textContent) {
        return textContent == null ? "" : textContent.replaceAll("\\s+", " ").trim();
    }

    private Class<?> resolveClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw ExceptionFactory.builderException(
                    "Failed to resolve resultType",
                    ErrorContext.create().resource(resource).resultType(Object.class).parameterSummary(className),
                    ex);
        }
    }

    private Class<?> resolveNamespaceClass() {
        try {
            return Class.forName(namespace);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }
}
