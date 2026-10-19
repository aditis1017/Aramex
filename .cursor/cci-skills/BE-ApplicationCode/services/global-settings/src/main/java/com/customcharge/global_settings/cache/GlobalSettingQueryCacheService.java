package com.customcharge.global_settings.cache;

import com.customcharge.persistencelayer.cache.CacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class GlobalSettingQueryCacheService {

    private final CacheManager cacheManager;
    private final CacheService cacheService;
    private final String keyPrefix;

    public GlobalSettingQueryCacheService(
            CacheManager cacheManager,
            CacheService cacheService,
            @Value("${spring.cache.redis.key-prefix:global-settings-api::}") String keyPrefix) {
        this.cacheManager = cacheManager;
        this.cacheService = cacheService;
        this.keyPrefix = keyPrefix;
    }

    public void clearQueryCaches() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
        cacheService.deleteCacheUsingPattern(keyPrefix + "*");
    }
}
