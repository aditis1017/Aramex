package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.cache.DashboardCacheNames;
import com.customcharge.dashboard.dto.AccountSearchPageDTO;
import com.customcharge.dashboard.dto.AccountSearchRequestDTO;
import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.enums.AccountNature;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Redis-backed cache for identical account search requests. Stores {@link AccountSearchPageDTO}
 * (plain DTOs) rather than {@link org.springframework.data.domain.Page} to avoid serialization errors.
 */
@Service
@RequiredArgsConstructor
public class AccountSearchCacheService {

    private final AccountSearchQueryService accountSearchQueryService;
    private final ObjectProvider<CacheService> cacheServiceProvider;

    @Value("${spring.cache.redis.key-prefix:dashboard-api::}")
    private String keyPrefix;

    @Cacheable(
            cacheNames = DashboardCacheNames.ACCOUNT_SEARCH,
            key = "{#request.accountNature, #request.entityId ?: 'ALL', #request.searchField ?: 'ALL', "
                    + "#request.searchType ?: 'ALL', #request.searchText ?: '', #request.page ?: 0, "
                    + "#request.pageSize ?: 'DEFAULT'}")
    public AccountSearchPageDTO getSearch(AccountSearchRequestDTO request) {
        return accountSearchQueryService.search(request);
    }

    /**
     * Invalidates AP search pages for a specific entity and unfiltered AP searches after account create.
     */
    public void evictApSearchesForEntity(Long entityId) {
        if (entityId == null) {
            return;
        }
        CacheService cache = cacheServiceProvider.getIfAvailable();
        if (cache == null) {
            return;
        }
        String base = keyPrefix + DashboardCacheNames.ACCOUNT_SEARCH + "::" + AccountNature.AP + ",";
        cache.deleteCacheUsingPattern(base + entityId + ",*");
        cache.deleteCacheUsingPattern(base + "ALL,*");
    }
}
