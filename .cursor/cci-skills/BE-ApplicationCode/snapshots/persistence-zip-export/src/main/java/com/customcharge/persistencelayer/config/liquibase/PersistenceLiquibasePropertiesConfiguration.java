package com.customcharge.persistencelayer.config.liquibase;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "liquibase.integration.spring.SpringLiquibase")
@EnableConfigurationProperties(PersistenceLiquibaseProperties.class)
public class PersistenceLiquibasePropertiesConfiguration {
}
