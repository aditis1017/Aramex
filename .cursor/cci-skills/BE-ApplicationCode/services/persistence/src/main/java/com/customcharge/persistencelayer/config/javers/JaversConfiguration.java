package com.customcharge.persistencelayer.config.javers;


import jakarta.persistence.EntityManagerFactory;

import org.javers.core.Javers;
import org.javers.spring.auditable.AuthorProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.customcharge.persistencelayer.audit.AuditAuthorResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "mvp.persistence.javers", name = "enabled", havingValue = "true")
@ConditionalOnClass(Javers.class)
@ConditionalOnBean({DataSource.class, EntityManagerFactory.class, PlatformTransactionManager.class})
@EnableConfigurationProperties(JaversProperties.class)
public class JaversConfiguration {

    @Bean
    @Primary
    public AuthorProvider authorProvider() {
        return AuditAuthorResolver::resolve;
    }
}
