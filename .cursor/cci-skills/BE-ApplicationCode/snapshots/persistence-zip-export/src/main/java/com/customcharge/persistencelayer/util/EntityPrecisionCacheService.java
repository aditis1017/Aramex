package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.cache.MasterDataCacheKeys;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.repository.EntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Service
@Slf4j
public class EntityPrecisionCacheService {

    static final Duration TTL = Duration.ofHours(6);

    private final EntityRepository entityRepository;
    private final ObjectProvider<CacheService> cacheServiceProvider;

    public EntityPrecisionCacheService(
            EntityRepository entityRepository,
            ObjectProvider<CacheService> cacheServiceProvider) {
        this.entityRepository = entityRepository;
        this.cacheServiceProvider = cacheServiceProvider;
    }

    @Transactional(readOnly = true)
    public int resolveDecimalPlaces(String entityCode) {
        if (!StringUtils.hasText(entityCode)) {
            return FixedDecimalUtil.DEFAULT_DECIMAL_PLACES;
        }

        String normalized = entityCode.strip().toUpperCase(Locale.ROOT);
        String key = MasterDataCacheKeys.entityDecimalPrecision(normalized);

        CacheService cache = cacheServiceProvider.getIfAvailable();
        if (cache != null) {
            Optional<Integer> cached = readCachedDecimalPlaces(cache, key);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        int decimalPlaces = entityRepository.findByCodeIgnoreCase(normalized)
                .filter(entity -> !Boolean.FALSE.equals(entity.getActive()))
                .map(this::parseDecimalPlaces)
                .orElse(FixedDecimalUtil.DEFAULT_DECIMAL_PLACES);

        if (cache != null) {
            cache.put(key, decimalPlaces, TTL);
        }
        return decimalPlaces;
    }

    private Optional<Integer> readCachedDecimalPlaces(CacheService cache, String key) {
        try {
            return cache.get(key, Integer.class);
        } catch (RuntimeException ex) {
            log.warn("Evicting unreadable entity precision cache entry {}: {}", key, ex.getMessage());
            cache.evict(key);
            return Optional.empty();
        }
    }

    private int parseDecimalPlaces(Entity entity) {
        Integer parsed = toNonNegativeInt(entity != null ? entity.getPrecision() : null);
        if (parsed == null) {
            log.debug(
                    "Entity {} has no valid precision; using default {}",
                    entity != null ? entity.getCode() : null,
                    FixedDecimalUtil.DEFAULT_DECIMAL_PLACES);
            return FixedDecimalUtil.DEFAULT_DECIMAL_PLACES;
        }
        return parsed;
    }

    static Integer toNonNegativeInt(Double precision) {
        if (precision == null || precision.isNaN() || precision.isInfinite() || precision < 0) {
            return null;
        }
        int places = (int) Math.floor(precision);
        return places >= 0 ? places : null;
    }
}
