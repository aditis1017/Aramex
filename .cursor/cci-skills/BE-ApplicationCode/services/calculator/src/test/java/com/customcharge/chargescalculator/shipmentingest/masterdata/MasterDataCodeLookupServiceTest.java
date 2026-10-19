package com.customcharge.chargescalculator.shipmentingest.masterdata;

import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.cache.dto.MasterCategoryValueRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterDataCodeLookupServiceTest {

    @Mock
    private ObjectProvider<CacheService> cacheServiceProvider;
    @Mock
    private CacheService cacheService;

    @InjectMocks
    private MasterDataCodeLookupService service;

    @Test
    void resolveCode_shouldReturnEmpty_whenInputBlank() {
        assertThat(service.resolveCode("PAYMENT_TYPE", " ")).isEmpty();
    }

    @Test
    void resolveCode_shouldReturnEmpty_whenCacheUnavailable() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(null);

        assertThat(service.resolveCode("PAYMENT_TYPE", "Prepaid")).isEmpty();
    }

    @Test
    void resolveCode_shouldMatchByCode() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.getList(any(), eq(MasterCategoryValueRow.class)))
                .thenReturn(Optional.of(List.of(CalculatorTestDataFactory.masterCategoryRow("P", "Prepaid", "Prepaid Label"))));

        assertThat(service.resolveCode("PAYMENT_TYPE", "p")).contains("P");
    }

    @Test
    void resolveCode_shouldMatchByLabel() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.getList(any(), eq(MasterCategoryValueRow.class)))
                .thenReturn(Optional.of(List.of(CalculatorTestDataFactory.masterCategoryRow("P", "Prepaid", "Prepaid Label"))));

        assertThat(service.resolveCode("PAYMENT_TYPE", "prepaid label")).contains("P");
    }

    @Test
    void resolveCode_shouldReturnEmpty_whenLookupFails() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.getList(any(), eq(MasterCategoryValueRow.class))).thenThrow(new RuntimeException("redis down"));

        assertThat(service.resolveCode("PAYMENT_TYPE", "Prepaid")).isEmpty();
    }

    @Test
    void resolveCode_shouldReturnEmpty_whenCategoryCodeMissing() {
        assertThat(service.resolveCode(null, "Prepaid")).isEmpty();
        assertThat(service.resolveCode(" ", "Prepaid")).isEmpty();
    }

    @Test
    void resolveCode_shouldReturnEmpty_whenInputMissing() {
        assertThat(service.resolveCode("PAYMENT_TYPE", null)).isEmpty();
    }

    @Test
    void resolveCode_shouldReturnEmpty_whenCacheRowsEmpty() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.getList(any(), eq(MasterCategoryValueRow.class))).thenReturn(Optional.of(List.of()));

        assertThat(service.resolveCode("PAYMENT_TYPE", "Prepaid")).isEmpty();
    }

    @Test
    void resolveCode_shouldMatchByName() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.getList(any(), eq(MasterCategoryValueRow.class)))
                .thenReturn(Optional.of(List.of(CalculatorTestDataFactory.masterCategoryRow("P", "Prepaid Name", "Label"))));

        assertThat(service.resolveCode("PAYMENT_TYPE", "prepaid name")).contains("P");
    }

    @Test
    void resolveCode_shouldIgnoreNullRows() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.getList(any(), eq(MasterCategoryValueRow.class)))
                .thenReturn(Optional.of(java.util.Arrays.asList(null, CalculatorTestDataFactory.masterCategoryRow("P", "Prepaid", "Prepaid"))));

        assertThat(service.resolveCode("PAYMENT_TYPE", "prepaid")).contains("P");
    }
}
