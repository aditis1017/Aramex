package com.customcharge.dashboard.service.impl;

import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.enums.AccountNature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountSearchCacheServiceTest {

    @Mock
    private AccountSearchQueryService accountSearchQueryService;

    @Mock
    private CacheService cacheService;

    private AccountSearchCacheService service;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ObjectProvider<CacheService> cacheServiceProvider = mock(ObjectProvider.class);
        lenient().when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        service = new AccountSearchCacheService(accountSearchQueryService, cacheServiceProvider);
        ReflectionTestUtils.setField(service, "keyPrefix", "dashboard-api::");
    }

    @Test
    void evictApSearchesForEntity_shouldDeleteEntityScopedAndUnfilteredPatterns() {
        service.evictApSearchesForEntity(12L);

        verify(cacheService).deleteCacheUsingPattern(
                "dashboard-api::accountSearch::" + AccountNature.AP + ",12,*");
        verify(cacheService).deleteCacheUsingPattern(
                "dashboard-api::accountSearch::" + AccountNature.AP + ",ALL,*");
    }

    @Test
    void evictApSearchesForEntity_shouldSkipWhenEntityIdNull() {
        service.evictApSearchesForEntity(null);

        verify(cacheService, never()).deleteCacheUsingPattern(org.mockito.ArgumentMatchers.anyString());
    }
}
