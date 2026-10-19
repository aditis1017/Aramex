package com.customcharge.dashboard.keywordrules.service;

import com.customcharge.dashboard.constants.KeywordRuleConstants;
import com.customcharge.persistencelayer.config.jpa.JpaProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class KeywordRuleSqlFunctionInitializer {

    private static final Logger log = LoggerFactory.getLogger(KeywordRuleSqlFunctionInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final String schema;

    public KeywordRuleSqlFunctionInitializer(JdbcTemplate jdbcTemplate, JpaProperties jpaProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.schema = resolveSchema(jpaProperties.getDefaultSchema());
    }

    @PostConstruct
    public void initializeFunction() {
        try {
            createPostgresFunction();
            log.info("SQL function {}.{} initialized.", schema, KeywordRuleConstants.FUNCTION_NAME);
        } catch (Exception exception) {
            log.error("Failed to initialize SQL function {}.{}.", schema, KeywordRuleConstants.FUNCTION_NAME, exception);
        }
    }

    private void createPostgresFunction() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
        String sql = """
                CREATE OR REPLACE FUNCTION %s.%s(input_text TEXT)
                RETURNS INTEGER
                LANGUAGE plpgsql
                AS $$
                BEGIN
                    IF input_text IS NULL OR BTRIM(input_text) = '' THEN
                        RETURN 0;
                    END IF;

                    IF EXISTS (
                        SELECT 1
                        FROM %s.%s kr
                        WHERE kr.is_active = TRUE
                          AND (
                              (kr.match_type = '%s' AND POSITION(LOWER(kr.keyword) IN LOWER(input_text)) > 0)
                              OR (kr.match_type = '%s' AND (' ' || LOWER(input_text) || ' ') LIKE ('%% ' || LOWER(kr.keyword) || ' %%'))
                              OR (kr.match_type = '%s' AND LOWER(input_text) LIKE (LOWER(kr.keyword) || '%%'))
                              OR (kr.match_type = '%s' AND LOWER(input_text) LIKE ('%%' || LOWER(kr.keyword)))
                          )
                    ) THEN
                        RETURN 1;
                    END IF;

                    RETURN 0;
                END;
                $$;
                """.formatted(
                schema,
                KeywordRuleConstants.FUNCTION_NAME,
                schema,
                KeywordRuleConstants.TABLE_NAME,
                KeywordRuleConstants.MATCH_TYPE_CONTAINS,
                KeywordRuleConstants.MATCH_TYPE_EXACT,
                KeywordRuleConstants.MATCH_TYPE_STARTS_WITH,
                KeywordRuleConstants.MATCH_TYPE_ENDS_WITH);
        jdbcTemplate.execute(sql);
    }

    private static String resolveSchema(String configuredSchema) {
        if (configuredSchema == null || configuredSchema.isBlank()) {
            return KeywordRuleConstants.FALLBACK_SCHEMA;
        }
        String schema = configuredSchema.trim();
        if (!KeywordRuleConstants.SAFE_SCHEMA_PATTERN.matcher(schema).matches()) {
            throw new IllegalArgumentException(
                    KeywordRuleConstants.INVALID_SCHEMA_MESSAGE + configuredSchema);
        }
        return schema;
    }
}
