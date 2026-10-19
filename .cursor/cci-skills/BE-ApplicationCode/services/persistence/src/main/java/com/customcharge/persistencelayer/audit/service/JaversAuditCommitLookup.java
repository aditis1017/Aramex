package com.customcharge.persistencelayer.audit.service;

import com.customcharge.persistencelayer.audit.model.AuditQueryRequest;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Paginates Javers audit commits via SQL so snapshot queries only load the current page.
 */
@Component
class JaversAuditCommitLookup {

    private final JdbcTemplate jdbcTemplate;
    private final String commitTable;
    private final String commitPropertyTable;
    private final String snapshotTable;
    private final String globalIdTable;

    JaversAuditCommitLookup(JdbcTemplate jdbcTemplate, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        String schema = firstNonBlank(
                environment.getProperty("javers.sqlSchema"),
                environment.getProperty("spring.liquibase.default-schema"),
                environment.getProperty("DB_SCHEMA"),
                environment.getProperty("mvp.persistence.jpa.default-schema"),
                environment.getProperty("mvp.persistence.jpa.default_schema"),
                currentSchemaFromJdbcUrl(environment.getProperty("mvp.persistence.datasource.url")),
                "public");
        this.commitTable = qualified(schema, "jv_commit");
        this.commitPropertyTable = qualified(schema, "jv_commit_property");
        this.snapshotTable = qualified(schema, "jv_snapshot");
        this.globalIdTable = qualified(schema, "jv_global_id");
    }

    long countMatchingCommits(AuditQueryRequest request) {
        FilterSql filter = buildFilterSql(request);
        String sql = "SELECT COUNT(*) FROM " + commitTable + " c WHERE " + filter.whereClause();
        Long count = jdbcTemplate.queryForObject(sql, Long.class, filter.params().toArray());
        return count == null ? 0L : count;
    }

    List<BigDecimal> findCommitIds(AuditQueryRequest request, long offset, int limit) {
        FilterSql filter = buildFilterSql(request);
        List<Object> params = new ArrayList<>(filter.params());
        params.add(offset);
        params.add(limit);
        String sql = "SELECT c.commit_id FROM " + commitTable + " c WHERE " + filter.whereClause()
                + " ORDER BY c.commit_date DESC NULLS LAST, c.commit_pk DESC OFFSET ? LIMIT ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getBigDecimal("commit_id"), params.toArray());
    }

    long countMatchingSnapshots(AuditQueryRequest request) {
        FilterSql filter = buildFilterSql(request);
        String sql = "SELECT COUNT(*) FROM " + snapshotTable + " s"
                + " INNER JOIN " + commitTable + " c ON s.commit_fk = c.commit_pk"
                + " WHERE " + filter.whereClause();
        Long count = jdbcTemplate.queryForObject(sql, Long.class, filter.params().toArray());
        return count == null ? 0L : count;
    }

    private FilterSql buildFilterSql(AuditQueryRequest request) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("c.commit_date >= ? AND c.commit_date <= ?");
        params.add(Timestamp.from(request.getFromDate()));
        params.add(Timestamp.from(request.getToDate()));

        String groupName = Optional.ofNullable(request.getGroupName())
                .map(String::trim)
                .map(s -> s.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Group Name must not be null or blank"));
        appendCommitPropertyExists(where, params, AuditCommitProperties.GROUP_NAME, groupName);

        Map<String, String> filters = request.getCommitPropertyFilters();
        if (filters != null) {
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String value = entry.getValue();
                if (value == null || value.isBlank()) {
                    continue;
                }
                appendCommitPropertyExists(where, params, entry.getKey(), value.trim());
            }
        }

        Optional<String> managedTypeFqcn = request.getManagedType()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim);
        if (managedTypeFqcn.isPresent()) {
            where.append(" AND EXISTS (SELECT 1 FROM ").append(snapshotTable).append(" s")
                    .append(" INNER JOIN ").append(globalIdTable).append(" g ON g.global_id_pk = s.global_id_fk")
                    .append(" WHERE s.commit_fk = c.commit_pk AND g.type_name = ?)");
            params.add(managedTypeFqcn.get());
        }

        return new FilterSql(where.toString(), params);
    }

    private void appendCommitPropertyExists(
            StringBuilder where, List<Object> params, String propertyName, String propertyValue) {
        where.append(" AND EXISTS (SELECT 1 FROM ").append(commitPropertyTable).append(" cp")
                .append(" WHERE cp.commit_fk = c.commit_pk AND cp.property_name = ? AND cp.property_value = ?)");
        params.add(propertyName);
        params.add(propertyValue);
    }

    private static String qualified(String schema, String table) {
        return schema.trim() + "." + table;
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private static String currentSchemaFromJdbcUrl(String jdbcUrl) {
        if (!StringUtils.hasText(jdbcUrl)) {
            return null;
        }
        String marker = "currentSchema=";
        int start = jdbcUrl.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = jdbcUrl.indexOf('&', start);
        return end < 0 ? jdbcUrl.substring(start).trim() : jdbcUrl.substring(start, end).trim();
    }

    private record FilterSql(String whereClause, List<Object> params) {
    }
}
