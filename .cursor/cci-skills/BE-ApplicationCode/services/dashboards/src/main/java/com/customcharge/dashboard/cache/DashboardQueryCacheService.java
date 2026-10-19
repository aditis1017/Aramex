package com.customcharge.dashboard.cache;

import com.customcharge.persistencelayer.cache.CacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardQueryCacheService {

    private final List<CacheManager> cacheManagers;
    private final CacheService cacheService;
    private final String keyPrefix;

    public DashboardQueryCacheService(
            List<CacheManager> cacheManagers,
            CacheService cacheService,
            @Value("${spring.cache.redis.key-prefix:dashboard-api::}") String keyPrefix) {
        this.cacheManagers = cacheManagers;
        this.cacheService = cacheService;
        this.keyPrefix = keyPrefix;
    }

    public void clearQueryCaches() {
        for (CacheManager cacheManager : cacheManagers) {
            for (String cacheName : cacheManager.getCacheNames()) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            }
        }
        // Also remove any leftover Spring-cache keys by prefix (covers unregistered names).
        cacheService.deleteCacheUsingPattern(keyPrefix + "*");
    }
}
