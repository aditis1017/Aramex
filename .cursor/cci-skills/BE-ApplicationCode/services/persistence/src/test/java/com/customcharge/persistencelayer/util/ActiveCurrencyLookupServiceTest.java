package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.dto.ActiveCurrencyDto;
import com.customcharge.persistencelayer.entities.Country;
import com.customcharge.persistencelayer.entities.Currency;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveCurrencyLookupServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private CurrencyCacheService currencyCacheService;

    @InjectMocks
    private ActiveCurrencyLookupService activeCurrencyLookupService;

    @Test
    void listActiveCurrencyCodes_shouldReturnDistinctCodesFromRepository() {
        when(currencyRepository.findDistinctActiveCurrencyCodes()).thenReturn(List.of("SAR", "USD"));

        assertThat(activeCurrencyLookupService.listActiveCurrencyCodes()).containsExactly("SAR", "USD");
    }

    @Test
    void requireActiveCurrencyCodeReturning_shouldNormalizeAndReturn_whenActive() {
        when(currencyRepository.findDistinctActiveCurrencyCodes()).thenReturn(List.of("USD", "SAR"));

        assertThat(activeCurrencyLookupService.requireActiveCurrencyCodeReturning(" usd ", "not found"))
                .isEqualTo("USD");
        verify(currencyRepository).findDistinctActiveCurrencyCodes();
    }

    @Test
    void requireActiveCurrencyCode_shouldThrow_whenInactiveOrMissing() {
        when(currencyRepository.findDistinctActiveCurrencyCodes()).thenReturn(List.of("USD", "SAR"));

        assertThatThrownBy(() -> activeCurrencyLookupService.requireActiveCurrencyCode("XYZ", "Currency not found"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Currency not found");
    }

    @Test
    void listActiveCurrencies_shouldMapActiveRowsFromRepository() {
        when(currencyRepository.findAllActiveWithCountry()).thenReturn(List.of(currency("SAR", "SA", "Saudi Arabia")));

        List<ActiveCurrencyDto> result = activeCurrencyLookupService.listActiveCurrencies();

        assertThat(result).containsExactly(
                new ActiveCurrencyDto(1L, "SAR", "Saudi Riyal", "SA", "Saudi Arabia"));
    }

    @Test
    void findActiveCurrencyByCountry_shouldUseCurrencyCacheService() {
        Currency currency = currency("USD", "US", "United States");
        when(currencyCacheService.getByCountryCode("US")).thenReturn(Optional.of(currency));

        assertThat(activeCurrencyLookupService.findActiveCurrencyByCountry("US"))
                .contains(new ActiveCurrencyDto(1L, "USD", "US Dollar", "US", "United States"));
        verify(currencyCacheService).getByCountryCode("US");
    }

    @Test
    void findActiveCurrencyByCountry_shouldReturnEmpty_whenCountryBlank() {
        assertThat(activeCurrencyLookupService.findActiveCurrencyByCountry("  ")).isEmpty();
    }

    private static Currency currency(String code, String countryCode, String countryName) {
        Country country = new Country();
        country.setCode(countryCode);
        country.setName(countryName);
        Currency currency = new Currency();
        currency.setId(1L);
        currency.setCurrencyCode(code);
        currency.setCurrencyName(code.equals("SAR") ? "Saudi Riyal" : "US Dollar");
        currency.setActive(true);
        currency.setCountry(country);
        return currency;
    }
}
