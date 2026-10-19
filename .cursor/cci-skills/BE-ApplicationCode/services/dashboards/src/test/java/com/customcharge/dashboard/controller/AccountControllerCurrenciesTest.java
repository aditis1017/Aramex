package com.customcharge.dashboard.controller;

import com.customcharge.dashboard.service.AccountService;
import com.customcharge.persistencelayer.dto.ActiveCurrencyDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerCurrenciesTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController controller;

    @Test
    void getActiveCurrencies_shouldReturnAll_whenCountryOmitted() {
        List<ActiveCurrencyDto> currencies = List.of(
                new ActiveCurrencyDto(1L, "SAR", "Saudi Riyal", "SA", "Saudi Arabia"));
        when(accountService.getActiveCurrencies(null)).thenReturn(currencies);

        assertThat(controller.getActiveCurrencies(null)).isEqualTo(currencies);
        verify(accountService).getActiveCurrencies(null);
    }

    @Test
    void getActiveCurrencies_shouldReturnCountryCurrency_whenCountryProvided() {
        List<ActiveCurrencyDto> currencies = List.of(
                new ActiveCurrencyDto(2L, "USD", "US Dollar", "US", "United States"));
        when(accountService.getActiveCurrencies("US")).thenReturn(currencies);

        assertThat(controller.getActiveCurrencies("US")).isEqualTo(currencies);
        verify(accountService).getActiveCurrencies("US");
    }
}
