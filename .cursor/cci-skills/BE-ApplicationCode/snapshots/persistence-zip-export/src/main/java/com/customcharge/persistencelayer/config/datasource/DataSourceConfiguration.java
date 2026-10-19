package com.customcharge.persistencelayer.config.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(prefix = "mvp.persistence.datasource", name = "url")
@EnableConfigurationProperties(DatasourceProperties.class)
public class DataSourceConfiguration {

    @Bean(name = "writeDataSource")
    public DataSource writeDataSource(DatasourceProperties properties) {
        return buildPool(properties, properties.getWrite(), "PersistenceWritePool", false);
    }

    @Bean(name = "readDataSource")
    public DataSource readDataSource(DatasourceProperties properties) {
        DatasourceProperties.Pool read = properties.getRead();
        boolean readConfigured = read != null && StringUtils.hasText(read.getUrl());
        if (!readConfigured) {
            return writeDataSource(properties);
        }
        return buildPool(properties, read, "PersistenceReadPool", true);
    }

    @Bean(name = "routingDataSource")
    public DataSource routingDataSource(
            DatasourceProperties properties,
            @Qualifier("writeDataSource") DataSource writeDataSource,
            @Qualifier("readDataSource") DataSource readDataSource) {
        if (!properties.getRouting().isEnabled()) {
            return writeDataSource;
        }

        boolean readConfigured = isReadPoolConfigured(properties);
        RoutingDataSource routingDataSource = new RoutingDataSource(
                properties.getRouting().isFallbackToWrite(),
                readConfigured);
        Map<Object, Object> targets = new HashMap<>();
        targets.put(DataSourceRoute.WRITE, writeDataSource);
        targets.put(DataSourceRoute.READ, readDataSource);
        routingDataSource.setTargetDataSources(targets);
        routingDataSource.setDefaultTargetDataSource(writeDataSource);
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }

    @Bean
    @Primary
    public DataSource dataSource(DatasourceProperties properties) {
        DataSource routed = routingDataSource(
                properties,
                writeDataSource(properties),
                readDataSource(properties));
        return new LazyConnectionDataSourceProxy(routed);
    }

    private static boolean isReadPoolConfigured(DatasourceProperties properties) {
        DatasourceProperties.Pool read = properties.getRead();
        return read != null && StringUtils.hasText(read.getUrl());
    }

    private DataSource buildPool(
            DatasourceProperties root,
            DatasourceProperties.Pool pool,
            String defaultPoolName,
            boolean readPool) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(fallback(pool.getUrl(), root.getUrl()));
        config.setUsername(fallback(pool.getUsername(), root.getUsername()));
        config.setPassword(fallback(pool.getPassword(), root.getPassword()));
        config.setDriverClassName(fallback(pool.getDriverClassName(), root.getDriverClassName()));
        config.setMaximumPoolSize(pool.getMaximumPoolSize() != null ? pool.getMaximumPoolSize() : root.getMaximumPoolSize());
        config.setMinimumIdle(pool.getMinimumIdle() != null ? pool.getMinimumIdle() : root.getMinimumIdle());
        config.setPoolName(fallback(pool.getPoolName(), defaultPoolName));
        if (readPool) {
            config.setReadOnly(true);
        }
        return new HikariDataSource(config);
    }

    private static String fallback(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
