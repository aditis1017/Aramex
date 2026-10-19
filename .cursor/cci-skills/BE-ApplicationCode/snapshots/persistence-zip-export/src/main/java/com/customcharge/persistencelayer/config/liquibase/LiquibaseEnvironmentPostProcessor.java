package com.customcharge.persistencelayer.config.liquibase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Supplies library defaults for Liquibase, Javers, and Hibernate DDL when consuming applications
 * do not define them explicitly. Schema name is derived from Liquibase/JPA/JDBC settings so
 * services do not need {@code javers.sqlSchema} in their own YAML.
 */
public class LiquibaseEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "persistenceLiquibaseDefaults";

    static final String CHANGELOG = PersistenceLiquibaseProperties.DEFAULT_CHANGE_LOG;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new LinkedHashMap<>();

        if (!environment.containsProperty("spring.liquibase.change-log")) {
            defaults.put("spring.liquibase.change-log", CHANGELOG);
        }
        if (!environment.containsProperty("spring.liquibase.enabled")) {
            String enabled = environment.getProperty(
                    "mvp.persistence.liquibase.enabled",
                    Boolean.toString(PersistenceLiquibaseProperties.DEFAULT_ENABLED));
            defaults.put("spring.liquibase.enabled", enabled);
        }
        if (!environment.containsProperty("mvp.persistence.liquibase.enabled")) {
            defaults.put(
                    "mvp.persistence.liquibase.enabled",
                    Boolean.toString(PersistenceLiquibaseProperties.DEFAULT_ENABLED));
        }
        if (!environment.containsProperty("mvp.persistence.jpa.ddl-auto")) {
            defaults.put("mvp.persistence.jpa.ddl-auto", "validate");
        }

        String schema = PersistenceSchemaResolver.resolve(environment);
        if (StringUtils.hasText(schema)) {
            if (!environment.containsProperty("spring.liquibase.default-schema")) {
                defaults.put("spring.liquibase.default-schema", schema);
            }
            if (!environment.containsProperty("spring.liquibase.liquibase-schema")) {
                defaults.put("spring.liquibase.liquibase-schema", schema);
            }
            if (!environment.containsProperty("javers.sqlSchema")) {
                defaults.put("javers.sqlSchema", schema);
            }
        }

        if (PersistenceSchemaResolver.isLiquibaseEnabled(environment)
                && !environment.containsProperty("javers.sqlSchemaManagementEnabled")) {
            defaults.put("javers.sqlSchemaManagementEnabled", false);
        }

        if (PersistenceSchemaResolver.isLiquibaseEnabled(environment)) {
            if (!environment.containsProperty("spring.sql.init.mode")) {
                defaults.put("spring.sql.init.mode", "never");
            }
        }

        if (!defaults.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
