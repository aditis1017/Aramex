package com.customcharge.persistencelayer.config.jpa;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "mvp.persistence.jpa")
public class JpaProperties {
    private String dialect;
    private boolean showSql = false;
    private String ddlAuto = "validate";
    private String defaultSchema;
    private boolean enabled = true;
    private int jdbcBatchSize = 50;
    private boolean orderInserts = true;
    private boolean orderUpdates = true;
    private boolean jdbcBatchVersionedData = true;
    private int defaultBatchFetchSize = 32;
}
