package com.customcharge.dashboard.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(DashboardLocalCacheConfiguration.class)
class DashboardLocalCacheConfigurationTest {

    @Autowired
    @Qualifier(DashboardLocalCacheConfiguration.LOCAL_CACHE_MANAGER)
    private CacheManager localCacheManager;

    @Test
    void localCacheManager_shouldRegisterAccountAuditHistoryCache() {
        assertThat(localCacheManager.getCacheNames())
                .contains(DashboardCacheNames.ACCOUNT_AUDIT_HISTORY);
        assertThat(localCacheManager.getCache(DashboardCacheNames.ACCOUNT_AUDIT_HISTORY))
                .isNotNull();
    }

    @Test
    void localCacheManager_shouldStoreAndRetrieveValuesInMemory() {
        var cache = localCacheManager.getCache(DashboardCacheNames.ACCOUNT_AUDIT_HISTORY);
        cache.put("probe-key", "probe-value");
        assertThat(cache.get("probe-key").get()).isEqualTo("probe-value");
    }
}
