package com.customcharge.persistencelayer.config.liquibase;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceSchemaResolverTest {

    @Test
    void resolvesFromLiquibaseDefaultSchema() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.liquibase.default-schema", "dbdesign");

        assertEquals("dbdesign", PersistenceSchemaResolver.resolve(env));
    }

    @Test
    void resolvesFromJdbcCurrentSchema() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("mvp.persistence.datasource.url",
                        "jdbc:postgresql://localhost:5432/mvp?currentSchema=dbdesign");

        assertEquals("dbdesign", PersistenceSchemaResolver.resolve(env));
    }

    @Test
    void liquibaseEnabledByDefault() {
        assertTrue(PersistenceSchemaResolver.isLiquibaseEnabled(new MockEnvironment()));
    }

    @Test
    void liquibaseDisabledWhenConfigured() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.liquibase.enabled", "false");

        assertFalse(PersistenceSchemaResolver.isLiquibaseEnabled(env));
    }
}
