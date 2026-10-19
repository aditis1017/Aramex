package com.customcharge.dashboard.cache;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DashboardSpringCacheConfigurationTest {

    private final DashboardSpringCacheConfiguration configuration = new DashboardSpringCacheConfiguration();

    @Test
    void cacheManager_shouldCreateRedisCacheManager() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        CacheManager cacheManager = configuration.cacheManager(
                connectionFactory,
                Duration.ofMinutes(10),
                "dashboard-api::",
                false);

        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.getCacheNames()).isEmpty();
    }

    @Test
    void cacheManager_shouldAllowNullValueCaching_whenEnabled() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        CacheManager cacheManager = configuration.cacheManager(
                connectionFactory,
                Duration.ofMinutes(5),
                "prefix::",
                true);

        assertThat(cacheManager).isNotNull();
    }
}
