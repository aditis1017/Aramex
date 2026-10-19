package com.customcharge.persistencelayer.config.liquibase;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the PostgreSQL schema used by Liquibase, JPA, and Javers from shared persistence settings.
 */
final class PersistenceSchemaResolver {

    private static final Pattern CURRENT_SCHEMA = Pattern.compile("currentSchema=([^&]+)", Pattern.CASE_INSENSITIVE);

    private PersistenceSchemaResolver() {
    }

    static String resolve(Environment environment) {
        String schema = firstNonBlank(
                environment.getProperty("spring.liquibase.default-schema"),
                environment.getProperty("DB_SCHEMA"),
                environment.getProperty("mvp.persistence.jpa.default-schema"),
                environment.getProperty("mvp.persistence.jpa.default_schema"));
        if (StringUtils.hasText(schema)) {
            return schema.trim();
        }
        return currentSchemaFromJdbcUrl(environment.getProperty("mvp.persistence.datasource.url"));
    }

    static boolean isLiquibaseEnabled(Environment environment) {
        if (environment.containsProperty("spring.liquibase.enabled")) {
            return environment.getProperty(
                    "spring.liquibase.enabled",
                    Boolean.class,
                    PersistenceLiquibaseProperties.DEFAULT_ENABLED);
        }
        return environment.getProperty(
                "mvp.persistence.liquibase.enabled",
                Boolean.class,
                PersistenceLiquibaseProperties.DEFAULT_ENABLED);
    }

    private static String currentSchemaFromJdbcUrl(String jdbcUrl) {
        if (!StringUtils.hasText(jdbcUrl)) {
            return null;
        }
        Matcher matcher = CURRENT_SCHEMA.matcher(jdbcUrl);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
