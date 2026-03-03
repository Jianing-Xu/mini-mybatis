package com.xujn.minimybatis.builder.xml;

import com.xujn.minimybatis.mapping.MappedStatement;
import com.xujn.minimybatis.session.Configuration;
import com.xujn.minimybatis.support.ErrorContext;
import com.xujn.minimybatis.support.ExceptionFactory;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parses one mapper XML resource into mapped statements.
 *
 * <p>Responsibility: validate document-level metadata and register each select
 * statement in the shared configuration.
 *
 * <p>Thread-safety: not thread-safe; build one instance per resource.
 */
public class XmlMapperBuilder {

    private final Configuration configuration;
    private final InputStream inputStream;
    private final String resource;

    public XmlMapperBuilder(Configuration configuration, InputStream inputStream, String resource) {
        this.configuration = configuration;
        this.inputStream = inputStream;
        this.resource = resource;
    }

    public void parse() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            Element root = document.getDocumentElement();
            if (root == null || !"mapper".equals(root.getTagName())) {
                throw ExceptionFactory.builderException(
                        "Root element <mapper> is required",
                        ErrorContext.create().resource(resource));
            }
            String namespace = root.getAttribute("namespace");
            if (namespace == null || namespace.isBlank()) {
                throw ExceptionFactory.builderException(
                        "Mapper namespace is required",
                        ErrorContext.create().resource(resource));
            }
            XmlStatementParser parser = new XmlStatementParser(namespace, resource);
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element element = (Element) child;
                if (!"select".equals(element.getTagName())) {
                    throw ExceptionFactory.builderException(
                            "Unsupported XML element in phase 1: " + element.getTagName(),
                            ErrorContext.create().resource(resource).mapper(resolveClass(namespace)));
                }
                MappedStatement mappedStatement = parser.parse(element);
                configuration.addMappedStatement(mappedStatement);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ExceptionFactory.builderException(
                    "Failed to parse mapper XML",
                    ErrorContext.create().resource(resource),
                    ex);
        }
    }

    private Class<?> resolveClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }
}
