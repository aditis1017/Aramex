package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.cache.MasterDataCacheKeys;
import com.customcharge.persistencelayer.entities.Country;
import com.customcharge.persistencelayer.entities.Currency;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyCacheServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private ObjectProvider<CacheService> cacheServiceProvider;
    @Mock
    private CacheService cacheService;

    @InjectMocks
    private CurrencyCacheService currencyCacheService;

    @Test
    void setAll_shouldWriteCurrencyByCountryCode() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(currencyRepository.findByActiveTrue()).thenReturn(List.of(currency("SAR", "SA")));

        currencyCacheService.setAll();

        verify(cacheService).put(eq(MasterDataCacheKeys.currencyByCountry("SA")), any(Currency.class), any(Duration.class));
    }

    @Test
    void getByCountryCode_shouldReturnCachedCurrency_whenPresent() {
        Currency cached = currency("SAR", "SA");
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get(MasterDataCacheKeys.currencyByCountry("SA"), Currency.class))
                .thenReturn(Optional.of(cached));

        assertThat(currencyCacheService.getByCountryCode("sa")).containsSame(cached);
        verify(currencyRepository, never()).findByCountry_CodeIgnoreCaseAndActiveTrue(any());
    }

    @Test
    void getByCountryCode_shouldLoadFromDbAndCache_whenMissingInRedis() {
        Currency currency = currency("USD", "US");
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get(MasterDataCacheKeys.currencyByCountry("US"), Currency.class))
                .thenReturn(Optional.empty());
        when(currencyRepository.findByCountry_CodeIgnoreCaseAndActiveTrue("US"))
                .thenReturn(Optional.of(currency));

        assertThat(currencyCacheService.getByCountryCode("US")).containsSame(currency);
        verify(cacheService).put(eq(MasterDataCacheKeys.currencyByCountry("US")), eq(currency), any(Duration.class));
    }

    @Test
    void getByEntityCode_shouldReturnCachedCurrency_whenPresent() {
        Currency cached = currency("AED", "AE");
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get(MasterDataCacheKeys.currencyByEntity("DXB"), Currency.class))
                .thenReturn(Optional.of(cached));

        assertThat(currencyCacheService.getByEntityCode("dxb")).containsSame(cached);
        verify(currencyRepository, never()).findActiveByEntityCode(any());
    }

    @Test
    void getByEntityCode_shouldLoadFromDb_whenRedisReadFails() {
        Currency currency = currency("AED", "AE");
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get(MasterDataCacheKeys.currencyByEntity("DXB"), Currency.class))
                .thenThrow(new IllegalStateException("LettuceConnectionFactory has been STOPPED"));
        when(currencyRepository.findActiveByEntityCode("DXB")).thenReturn(Optional.of(currency));

        assertThat(currencyCacheService.getByEntityCode("DXB")).containsSame(currency);
    }

    @Test
    void getByEntityCode_shouldLoadFromDbAndCache_whenMissingInRedis() {
        Currency currency = currency("AED", "AE");
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get(MasterDataCacheKeys.currencyByEntity("DXB"), Currency.class))
                .thenReturn(Optional.empty());
        when(currencyRepository.findActiveByEntityCode("DXB")).thenReturn(Optional.of(currency));

        assertThat(currencyCacheService.getByEntityCode("DXB")).containsSame(currency);
        verify(cacheService).put(eq(MasterDataCacheKeys.currencyByEntity("DXB")), eq(currency), any(Duration.class));
    }

    private static Currency currency(String code, String countryCode) {
        Country country = new Country();
        country.setCode(countryCode);
        Currency currency = new Currency();
        currency.setCurrencyCode(code);
        currency.setCurrencyName(code);
        currency.setActive(true);
        currency.setCountry(country);
        return currency;
    }
}
