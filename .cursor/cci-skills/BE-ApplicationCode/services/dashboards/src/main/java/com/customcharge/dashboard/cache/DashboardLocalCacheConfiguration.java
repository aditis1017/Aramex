package com.customcharge.dashboard.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-process {@code @Cacheable} manager for response shapes that are unsafe in Redis JSON cache
 * (e.g. {@link com.fasterxml.jackson.databind.JsonNode} in audit snapshots). Not shared across pods;
 * cleared on restart.
 */
@Configuration
public class DashboardLocalCacheConfiguration {

    public static final String LOCAL_CACHE_MANAGER = "dashboardLocalCacheManager";

    @Bean(LOCAL_CACHE_MANAGER)
    public CacheManager dashboardLocalCacheManager() {
        return new ConcurrentMapCacheManager(DashboardCacheNames.ACCOUNT_AUDIT_HISTORY);
    }
}
