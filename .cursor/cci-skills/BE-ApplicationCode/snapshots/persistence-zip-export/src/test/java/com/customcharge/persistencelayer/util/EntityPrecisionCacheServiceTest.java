package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.cache.MasterDataCacheKeys;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.repository.EntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityPrecisionCacheServiceTest {

    @Mock
    private EntityRepository entityRepository;
    @Mock
    private ObjectProvider<CacheService> cacheServiceProvider;
    @Mock
    private CacheService cacheService;

    @InjectMocks
    private EntityPrecisionCacheService entityPrecisionCacheService;

    @Test
    void resolveDecimalPlaces_shouldReturnCachedValue_whenPresentInRedis() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get(MasterDataCacheKeys.entityDecimalPrecision("DXB"), Integer.class))
                .thenReturn(Optional.of(4));

        assertThat(entityPrecisionCacheService.resolveDecimalPlaces("dxb")).isEqualTo(4);
        verify(entityRepository, never()).findByCodeIgnoreCase(any());
    }

    @Test
    void resolveDecimalPlaces_shouldLoadFromDbAndCache_whenMissingInRedis() {
        Entity entity = entity("DXB", 3.0);
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get(MasterDataCacheKeys.entityDecimalPrecision("DXB"), Integer.class))
                .thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(entity));

        assertThat(entityPrecisionCacheService.resolveDecimalPlaces("DXB")).isEqualTo(3);
        verify(cacheService).put(
                eq(MasterDataCacheKeys.entityDecimalPrecision("DXB")),
                eq(3),
                eq(EntityPrecisionCacheService.TTL));
    }

    @Test
    void resolveDecimalPlaces_shouldUseDefault_whenEntityMissing() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get(MasterDataCacheKeys.entityDecimalPrecision("DXB"), Integer.class))
                .thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.empty());

        assertThat(entityPrecisionCacheService.resolveDecimalPlaces("DXB"))
                .isEqualTo(FixedDecimalUtil.DEFAULT_DECIMAL_PLACES);
        verify(cacheService).put(
                eq(MasterDataCacheKeys.entityDecimalPrecision("DXB")),
                eq(FixedDecimalUtil.DEFAULT_DECIMAL_PLACES),
                eq(EntityPrecisionCacheService.TTL));
    }

    @Test
    void resolveDecimalPlaces_shouldUseDefault_whenPrecisionInvalid() {
        Entity entity = entity("DXB", -1.0);
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get(MasterDataCacheKeys.entityDecimalPrecision("DXB"), Integer.class))
                .thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(entity));

        assertThat(entityPrecisionCacheService.resolveDecimalPlaces("DXB"))
                .isEqualTo(FixedDecimalUtil.DEFAULT_DECIMAL_PLACES);
    }

    @Test
    void resolveDecimalPlaces_shouldUseDefault_whenEntityCodeBlank() {
        assertThat(entityPrecisionCacheService.resolveDecimalPlaces(null))
                .isEqualTo(FixedDecimalUtil.DEFAULT_DECIMAL_PLACES);
        assertThat(entityPrecisionCacheService.resolveDecimalPlaces("  "))
                .isEqualTo(FixedDecimalUtil.DEFAULT_DECIMAL_PLACES);
        verify(entityRepository, never()).findByCodeIgnoreCase(any());
    }

    @Test
    void resolveDecimalPlaces_shouldLoadFromDb_whenCacheUnavailable() {
        Entity entity = entity("RUH", 2.0);
        when(cacheServiceProvider.getIfAvailable()).thenReturn(null);
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(entity));

        assertThat(entityPrecisionCacheService.resolveDecimalPlaces("RUH")).isEqualTo(2);
    }

    @Test
    void resolveDecimalPlaces_shouldEvictAndLoadFromDb_whenCacheEntryUnreadable() {
        Entity entity = entity("RUH", 3.0);
        String key = MasterDataCacheKeys.entityDecimalPrecision("RUH");
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get(key, Integer.class))
                .thenThrow(new IllegalArgumentException("Cannot deserialize value of type Integer from Object"));
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(entity));

        assertThat(entityPrecisionCacheService.resolveDecimalPlaces("RUH")).isEqualTo(3);
        verify(cacheService).evict(key);
        verify(cacheService).put(eq(key), eq(3), eq(EntityPrecisionCacheService.TTL));
    }

    @Test
    void entityDecimalPrecision_shouldNotUseCategoryValueKey() {
        assertThat(MasterDataCacheKeys.entityDecimalPrecision("RUH"))
                .isEqualTo("entity:precision:RUH")
                .doesNotContain("category_value");
    }

    @Test
    void toNonNegativeInt_shouldParseWholeNumberPrecision() {
        assertThat(EntityPrecisionCacheService.toNonNegativeInt(3.0)).isEqualTo(3);
        assertThat(EntityPrecisionCacheService.toNonNegativeInt(null)).isNull();
        assertThat(EntityPrecisionCacheService.toNonNegativeInt(Double.NaN)).isNull();
    }

    private static Entity entity(String code, Double precision) {
        Entity entity = new Entity();
        entity.setCode(code);
        entity.setPrecision(precision);
        entity.setActive(true);
        return entity;
    }
}
