package com.xujn.minispring.integration.mybatis.factory;

import com.xujn.minimybatis.builder.xml.XmlMapperBuilder;
import com.xujn.minimybatis.exceptions.BuilderException;
import com.xujn.minimybatis.session.Configuration;
import com.xujn.minimybatis.session.SqlSessionFactory;
import com.xujn.minimybatis.session.defaults.DefaultSqlSessionFactory;
import com.xujn.minispring.integration.mybatis.config.ConflictPolicy;
import com.xujn.minispring.integration.mybatis.exception.MapperRegistrationException;
import com.xujn.minispring.integration.mybatis.exception.MappingLoadException;
import com.xujn.minispring.integration.mybatis.exception.MissingDataSourceException;
import com.xujn.minispring.integration.mybatis.exception.StatementConflictException;
import com.xujn.minispring.integration.mybatis.support.ResourcePatternResolver;
import com.xujn.minispring.integration.mybatis.support.ResourcePatternResolver.ResolvedResource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Bootstrap helper that assembles Configuration and SqlSessionFactory.
 *
 * <p>Responsibility: validate integration inputs, load mapper XML resources and
 * return a ready-to-use SqlSessionFactory.
 *
 * <p>Thread-safety: configure during bootstrap, then discard or reuse as an
 * immutable helper.
 */
public class SqlSessionFactoryBean {

    private final ResourcePatternResolver resourcePatternResolver = new ResourcePatternResolver();

    private DataSource dataSource;
    private String configLocation;
    private String[] mapperLocations;
    private String[] typeAliasesPackages;
    private Properties configurationProperties;
    private boolean failFast = true;
    private boolean mapUnderscoreToCamelCase;
    private ConflictPolicy conflictPolicy = ConflictPolicy.FAIL_FAST;

    public SqlSessionFactory buildSqlSessionFactory() {
        if (dataSource == null) {
            throw new MissingDataSourceException("DataSource is required: beanName=sqlSessionFactory, dependency=dataSource");
        }
        if (conflictPolicy == ConflictPolicy.OVERRIDE) {
            throw new UnsupportedOperationException("ConflictPolicy.OVERRIDE is not supported in integration phase 1");
        }
        Configuration configuration = new Configuration();
        configuration.setDataSource(dataSource);
        configuration.setMapUnderscoreToCamelCase(mapUnderscoreToCamelCase);
        loadMapperResources(configuration);
        return new DefaultSqlSessionFactory(configuration);
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setConfigLocation(String configLocation) {
        this.configLocation = configLocation;
    }

    public void setMapperLocations(String[] mapperLocations) {
        this.mapperLocations = mapperLocations;
    }

    public void setTypeAliasesPackages(String[] typeAliasesPackages) {
        this.typeAliasesPackages = typeAliasesPackages;
    }

    public void setConfigurationProperties(Properties configurationProperties) {
        this.configurationProperties = configurationProperties;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
        this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
    }

    public void setConflictPolicy(ConflictPolicy conflictPolicy) {
        if (conflictPolicy != null) {
            this.conflictPolicy = conflictPolicy;
        }
    }

    private void loadMapperResources(Configuration configuration) {
        Map<String, String> namespaceResources = new LinkedHashMap<>();
        for (ResolvedResource resource : resourcePatternResolver.resolve(mapperLocations)) {
            String namespace = extractNamespace(resource.location(), resource.bytes());
            try (InputStream inputStream = new ByteArrayInputStream(resource.bytes())) {
                new XmlMapperBuilder(configuration, inputStream, resource.location()).parse();
            } catch (StatementConflictException | MapperRegistrationException ex) {
                throw ex;
            } catch (BuilderException ex) {
                if (ex.getMessage().contains("Duplicate statementId detected")) {
                    throw new StatementConflictException("Failed to register statements: resourcePath=" + resource.location()
                            + ", cause=" + ex.getMessage(), ex);
                }
                throw new MappingLoadException("Failed to parse mapper XML: resourcePath=" + resource.location()
                        + ", mapper=" + namespace, ex);
            } catch (java.io.IOException ex) {
                throw new MappingLoadException("Failed to close mapper resource stream: resourcePath="
                        + resource.location() + ", mapper=" + namespace, ex);
            } catch (RuntimeException ex) {
                throw new MappingLoadException("Failed to load mapper XML: resourcePath=" + resource.location()
                        + ", mapper=" + namespace, ex);
            }
            String existingResource = namespaceResources.putIfAbsent(namespace, resource.location());
            if (existingResource != null) {
                throw new MapperRegistrationException("Duplicate mapper namespace detected: mapperClass=" + namespace
                        + ", existingResource=" + existingResource + ", newResource=" + resource.location());
            }
            registerMapperInterface(configuration, namespace, resource.location());
        }
    }

    private void registerMapperInterface(Configuration configuration, String namespace, String resourcePath) {
        try {
            Class<?> mapperType = Class.forName(namespace);
            if (!mapperType.isInterface()) {
                throw new MapperRegistrationException("Mapper namespace must resolve to interface: mapperClass="
                        + namespace + ", resourcePath=" + resourcePath);
            }
            if (!configuration.getMapperRegistry().hasMapper(mapperType)) {
                configuration.addMapper(mapperType);
            }
        } catch (ClassNotFoundException ex) {
            throw new MapperRegistrationException("Mapper namespace class not found: mapperClass="
                    + namespace + ", resourcePath=" + resourcePath, ex);
        }
    }

    private String extractNamespace(String resourcePath, byte[] bytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(bytes));
            Element root = document.getDocumentElement();
            if (root == null || !"mapper".equals(root.getTagName())) {
                throw new MappingLoadException("Root element <mapper> is required: resourcePath=" + resourcePath);
            }
            String namespace = root.getAttribute("namespace");
            if (namespace == null || namespace.isBlank()) {
                throw new MappingLoadException("Mapper namespace is required: resourcePath=" + resourcePath);
            }
            return namespace.trim();
        } catch (MappingLoadException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MappingLoadException("Failed to inspect mapper namespace: resourcePath=" + resourcePath, ex);
        }
    }
}
