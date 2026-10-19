package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.cache.MasterDataCacheKeys;
import com.customcharge.persistencelayer.entities.Currency;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Service
public class CurrencyCacheService {

    private static final Duration TTL = Duration.ofHours(25);

    private final CurrencyRepository currencyRepository;
    private final ObjectProvider<CacheService> cacheServiceProvider;

    public CurrencyCacheService(
            CurrencyRepository currencyRepository,
            ObjectProvider<CacheService> cacheServiceProvider) {
        this.currencyRepository = currencyRepository;
        this.cacheServiceProvider = cacheServiceProvider;
    }

    @Transactional(readOnly = true)
    public void setAll() {
        CacheService cache = cacheServiceProvider.getIfAvailable();
        if (cache == null) {
            return;
        }
        for (Currency currency : currencyRepository.findByActiveTrue()) {
            String countryCode = countryCode(currency);
            if (StringUtils.hasText(countryCode)) {
                cache.put(MasterDataCacheKeys.currencyByCountry(countryCode), currency, TTL);
            }
        }
    }

    @Transactional(readOnly = true)
    public Optional<Currency> getByCountryCode(String countryCode) {
        if (!StringUtils.hasText(countryCode)) {
            return Optional.empty();
        }
        String normalized = countryCode.trim();
        String key = MasterDataCacheKeys.currencyByCountry(normalized);
        return getCachedOrLoad(key, () -> currencyRepository.findByCountry_CodeIgnoreCaseAndActiveTrue(normalized));
    }

    @Transactional(readOnly = true)
    public Optional<Currency> getByEntityCode(String entityCode) {
        if (!StringUtils.hasText(entityCode)) {
            return Optional.empty();
        }
        String normalized = entityCode.strip().toUpperCase(Locale.ROOT);
        String key = MasterDataCacheKeys.currencyByEntity(normalized);
        return getCachedOrLoad(key, () -> currencyRepository.findActiveByEntityCode(normalized));
    }

    private Optional<Currency> getCachedOrLoad(String key, java.util.function.Supplier<Optional<Currency>> loader) {
        CacheService cache = cacheServiceProvider.getIfAvailable();
        if (cache != null) {
            Optional<Currency> cached = readFromCache(cache, key);
            if (cached.isPresent()) {
                return cached;
            }
        }
        return loader.get().map(currency -> cacheAndReturn(key, currency));
    }

    private static Optional<Currency> readFromCache(CacheService cache, String key) {
        try {
            return cache.get(key, Currency.class);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Currency cacheAndReturn(String key, Currency currency) {
        CacheService cache = cacheServiceProvider.getIfAvailable();
        if (cache != null) {
            cache.put(key, currency, TTL);
        }
        return currency;
    }

    private static String countryCode(Currency currency) {
        return currency.getCountry() != null ? currency.getCountry().getCode() : null;
    }
}
