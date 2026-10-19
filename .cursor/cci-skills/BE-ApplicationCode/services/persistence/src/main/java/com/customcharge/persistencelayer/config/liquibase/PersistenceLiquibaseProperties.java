package com.customcharge.persistencelayer.config.liquibase;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mvp.persistence.liquibase")
public class PersistenceLiquibaseProperties {

    /**
     * Canonical default for Liquibase when {@code mvp.persistence.liquibase.enabled} is unset.
     * Consuming services should use the same literal in YAML (e.g. {@code ${LIQUIBASE_ENABLED:true}}).
     */
    public static final boolean DEFAULT_ENABLED = true;

    /**
     * Canonical default changelog path; keep in sync with service {@code LIQUIBASE_CHANGE_LOG} defaults.
     */
    public static final String DEFAULT_CHANGE_LOG = "classpath:db/changelog/db.changelog-master.yaml";

    /**
     * When false, Liquibase will not run on application startup (schema must already match).
     */
    private boolean enabled = DEFAULT_ENABLED;


    /**
     * Master changelog location on the classpath.
     */
    private String changeLog = DEFAULT_CHANGE_LOG;
}
