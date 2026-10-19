package com.customcharge.fileorchestration.service;

import com.customcharge.persistencelayer.entities.Currency;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityCurrencyLookupServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private EntityCurrencyLookupService entityCurrencyLookupService;

    @Test
    void findCurrencyCodeByEntityCode_shouldReturnCurrencyCode_whenEntityMapped() {
        Currency currency = new Currency();
        currency.setCurrencyCode("SAR");
        when(currencyRepository.findActiveByEntityCode("RUH")).thenReturn(Optional.of(currency));

        assertThat(entityCurrencyLookupService.findCurrencyCodeByEntityCode("RUH")).contains("SAR");
    }

    @Test
    void findCurrencyCodeByEntityCode_shouldReturnEmpty_whenEntityCodeBlank() {
        assertThat(entityCurrencyLookupService.findCurrencyCodeByEntityCode(null)).isEmpty();
        assertThat(entityCurrencyLookupService.findCurrencyCodeByEntityCode("  ")).isEmpty();
        verify(currencyRepository, never()).findActiveByEntityCode(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void findCurrencyCodeByEntityCode_shouldReturnEmpty_whenCurrencyMissing() {
        when(currencyRepository.findActiveByEntityCode("DXB")).thenReturn(Optional.empty());

        assertThat(entityCurrencyLookupService.findCurrencyCodeByEntityCode("DXB")).isEmpty();
    }
}
