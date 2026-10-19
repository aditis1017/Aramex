package com.customcharge.global_settings.cache;

import com.customcharge.persistencelayer.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSettingQueryCacheServiceTest {

    @Mock
    private CacheManager cacheManager;
    @Mock
    private CacheService cacheService;

    private GlobalSettingQueryCacheService service;

    @BeforeEach
    void setUp() {
        service = new GlobalSettingQueryCacheService(cacheManager, cacheService, "global-settings-api::");
    }

    @Test
    void clearQueryCaches_shouldClearAllCachesAndDeleteRedisPattern() {
        Cache cacheOne = mock(Cache.class);
        Cache cacheTwo = mock(Cache.class);
        when(cacheManager.getCacheNames()).thenReturn(List.of("cache-one", "cache-two"));
        when(cacheManager.getCache("cache-one")).thenReturn(cacheOne);
        when(cacheManager.getCache("cache-two")).thenReturn(cacheTwo);

        service.clearQueryCaches();

        verify(cacheOne).clear();
        verify(cacheTwo).clear();
        verify(cacheService).deleteCacheUsingPattern("global-settings-api::*");
    }

    @Test
    void clearQueryCaches_shouldSkipNullCaches() {
        when(cacheManager.getCacheNames()).thenReturn(List.of("missing-cache"));
        when(cacheManager.getCache("missing-cache")).thenReturn(null);

        service.clearQueryCaches();

        verify(cacheService).deleteCacheUsingPattern("global-settings-api::*");
    }
}
