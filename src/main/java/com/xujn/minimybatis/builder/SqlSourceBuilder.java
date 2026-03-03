package com.xujn.minimybatis.builder;

import com.xujn.minimybatis.mapping.ParameterMapping;
import com.xujn.minimybatis.mapping.SqlSource;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses {@code #{...}} placeholders into JDBC SQL and parameter mappings.
 *
 * <p>Responsibility: keep placeholder parsing out of XML traversal and produce
 * deterministic parameter order for JDBC binding.
 *
 * <p>Thread-safety: stateless and thread-safe.
 */
public class SqlSourceBuilder {

    private static final Pattern PLACEHOLDER = Pattern.compile("#\\{\\s*([^}]+?)\\s*}");

    public SqlSource parse(String originalSql) {
        Matcher matcher = PLACEHOLDER.matcher(originalSql);
        StringBuilder parsedSql = new StringBuilder();
        List<ParameterMapping> mappings = new ArrayList<>();
        int lastIndex = 0;
        while (matcher.find()) {
            parsedSql.append(originalSql, lastIndex, matcher.start());
            parsedSql.append('?');
            mappings.add(new ParameterMapping(matcher.group(1).trim()));
            lastIndex = matcher.end();
        }
        parsedSql.append(originalSql.substring(lastIndex));
        return new SqlSource(originalSql, parsedSql.toString(), mappings);
    }
}
