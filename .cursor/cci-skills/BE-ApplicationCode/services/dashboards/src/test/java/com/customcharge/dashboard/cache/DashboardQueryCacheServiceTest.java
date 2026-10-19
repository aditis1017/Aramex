package com.customcharge.dashboard.cache;

import com.customcharge.persistencelayer.cache.CacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardQueryCacheServiceTest {

    @Mock
    private CacheService cacheService;

    @Test
    void clearQueryCaches_shouldClearEveryRegisteredCacheManager() {
        Cache redisCache = new ConcurrentMapCache("rolesFindAll");
        Cache localCache = new ConcurrentMapCache(DashboardCacheNames.ACCOUNT_AUDIT_HISTORY);
        org.springframework.cache.CacheManager redisManager = mock(org.springframework.cache.CacheManager.class);
        org.springframework.cache.CacheManager localManager = mock(org.springframework.cache.CacheManager.class);
        when(redisManager.getCacheNames()).thenReturn(List.of("rolesFindAll"));
        when(redisManager.getCache("rolesFindAll")).thenReturn(redisCache);
        when(localManager.getCacheNames()).thenReturn(List.of(DashboardCacheNames.ACCOUNT_AUDIT_HISTORY));
        when(localManager.getCache(DashboardCacheNames.ACCOUNT_AUDIT_HISTORY)).thenReturn(localCache);

        DashboardQueryCacheService service =
                new DashboardQueryCacheService(List.of(redisManager, localManager), cacheService, "dashboard-api::");

        service.clearQueryCaches();

        assert redisCache.get("any") == null;
        assert localCache.get("any") == null;
        verify(cacheService).deleteCacheUsingPattern("dashboard-api::*");
    }
}
