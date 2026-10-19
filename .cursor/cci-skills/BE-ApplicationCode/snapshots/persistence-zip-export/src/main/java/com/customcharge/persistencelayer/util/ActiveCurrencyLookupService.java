package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.dto.ActiveCurrencyDto;
import com.customcharge.persistencelayer.entities.Country;
import com.customcharge.persistencelayer.entities.Currency;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves active currency codes from the {@code currency} table (not master category values).
 */
@Service
public class ActiveCurrencyLookupService {

    private static final long ACTIVE_CODES_CACHE_TTL_MS = 60_000L;

    private final CurrencyRepository currencyRepository;
    private final CurrencyCacheService currencyCacheService;

    private volatile Set<String> activeCurrencyCodesCache = Set.of();
    private volatile long activeCurrencyCodesCacheLoadedAt;

    public ActiveCurrencyLookupService(
            CurrencyRepository currencyRepository,
            CurrencyCacheService currencyCacheService) {
        this.currencyRepository = currencyRepository;
        this.currencyCacheService = currencyCacheService;
    }

    @Transactional(readOnly = true)
    public List<String> listActiveCurrencyCodes() {
        return currencyRepository.findDistinctActiveCurrencyCodes();
    }

    @Transactional(readOnly = true)
    public List<ActiveCurrencyDto> listActiveCurrencies() {
        return currencyRepository.findAllActiveWithCountry().stream()
                .map(ActiveCurrencyLookupService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ActiveCurrencyDto> findActiveCurrencyByCountry(String countryCode) {
        if (!StringUtils.hasText(countryCode)) {
            return Optional.empty();
        }
        return currencyCacheService.getByCountryCode(countryCode.strip())
                .map(ActiveCurrencyLookupService::toDto);
    }

    @Transactional(readOnly = true)
    public void requireActiveCurrencyCode(String currencyCode, String message) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new ResourceNotFoundException(message);
        }
        String normalized = ExchangeRateService.normalizeCurrency(currencyCode);
        if (!cachedActiveCurrencyCodes().contains(normalized)) {
            throw new ResourceNotFoundException(message);
        }
    }

    private Set<String> cachedActiveCurrencyCodes() {
        long now = System.currentTimeMillis();
        Set<String> snapshot = activeCurrencyCodesCache;
        if (!snapshot.isEmpty() && (now - activeCurrencyCodesCacheLoadedAt) < ACTIVE_CODES_CACHE_TTL_MS) {
            return snapshot;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (!activeCurrencyCodesCache.isEmpty()
                    && (now - activeCurrencyCodesCacheLoadedAt) < ACTIVE_CODES_CACHE_TTL_MS) {
                return activeCurrencyCodesCache;
            }
            Set<String> loaded = currencyRepository.findDistinctActiveCurrencyCodes().stream()
                    .map(ExchangeRateService::normalizeCurrency)
                    .collect(Collectors.toUnmodifiableSet());
            activeCurrencyCodesCache = loaded;
            activeCurrencyCodesCacheLoadedAt = now;
            return loaded;
        }
    }

    @Transactional(readOnly = true)
    public String requireActiveCurrencyCodeReturning(String currencyCode, String message) {
        requireActiveCurrencyCode(currencyCode, message);
        return ExchangeRateService.normalizeCurrency(currencyCode);
    }

    private static ActiveCurrencyDto toDto(Currency currency) {
        Country country = currency.getCountry();
        return new ActiveCurrencyDto(
                currency.getId(),
                ExchangeRateService.normalizeCurrency(currency.getCurrencyCode()),
                currency.getCurrencyName(),
                country != null ? country.getCode() : null,
                country != null ? country.getName() : null);
    }
}
