package com.xujn.minispring.integration.mybatis.support;

import com.xujn.minispring.integration.mybatis.exception.MappingLoadException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves integration mapper resources from the application classpath.
 *
 * <p>Responsibility: provide stable resource names and bytes for XML parsing.
 *
 * <p>Thread-safety: stateless and thread-safe.
 */
public class ResourcePatternResolver {

    public List<ResolvedResource> resolve(String... locations) {
        List<ResolvedResource> resources = new ArrayList<>();
        if (locations == null) {
            return resources;
        }
        for (String location : locations) {
            if (location == null || location.isBlank()) {
                continue;
            }
            String trimmed = location.trim();
            try (InputStream inputStream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(trimmed)) {
                if (inputStream == null) {
                    throw new MappingLoadException("Mapper resource not found: resourcePath=" + trimmed);
                }
                resources.add(new ResolvedResource(trimmed, inputStream.readAllBytes()));
            } catch (IOException ex) {
                throw new MappingLoadException("Failed to read mapper resource: resourcePath=" + trimmed, ex);
            }
        }
        return resources;
    }

    public record ResolvedResource(String location, byte[] bytes) {
    }
}
