package com.customcharge.persistencelayer.config.jpa;

import com.customcharge.persistencelayer.entity.AuditorProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@ConditionalOnProperty(prefix = "mvp.persistence.jpa", name = "enabled", havingValue = "true")
@Configuration
@AutoConfigureBefore(name = "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaConfiguration")
@ConditionalOnClass(EntityManagerFactory.class)
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(JpaProperties.class)
@EnableJpaRepositories(basePackages = "com.customcharge")
@EntityScan(basePackages = "com.customcharge")
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfiguration {

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource,
            JpaProperties properties) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.customcharge.persistencelayer.entities", "com.customcharge.persistencelayer.entity", "com.customcharge");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(buildJpaPropertyMap(properties));
        if (properties.getDefaultSchema() != null && !properties.getDefaultSchema().isBlank()) {
            factory.getJpaPropertyMap().put("hibernate.default_schema", properties.getDefaultSchema());
        }
        return factory;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean("auditorProvider")
    @ConditionalOnMissingBean(name = "auditorProvider")
    public AuditorAware<String> auditorProvider() {
        return new AuditorProvider();
    }

    private static Map<String, Object> buildJpaPropertyMap(JpaProperties jpa) {
        Map<String, Object> map = new HashMap<>();
        map.put("hibernate.show_sql", jpa.isShowSql());
        map.put("hibernate.hbm2ddl.auto", jpa.getDdlAuto());
        if (jpa.getDialect() != null && !jpa.getDialect().isBlank()) {
            map.put("hibernate.dialect", jpa.getDialect());
        }
        map.put(
                "hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
        );
        map.put("hibernate.jdbc.batch_size", jpa.getJdbcBatchSize());
        map.put("hibernate.order_inserts", jpa.isOrderInserts());
        map.put("hibernate.order_updates", jpa.isOrderUpdates());
        map.put("hibernate.jdbc.batch_versioned_data", jpa.isJdbcBatchVersionedData());
        if (jpa.getDefaultBatchFetchSize() > 0) {
            map.put("hibernate.default_batch_fetch_size", jpa.getDefaultBatchFetchSize());
        }
        return map;
    }
}
