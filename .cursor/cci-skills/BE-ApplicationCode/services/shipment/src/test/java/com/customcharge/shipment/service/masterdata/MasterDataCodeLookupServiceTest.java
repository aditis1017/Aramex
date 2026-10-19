package com.customcharge.shipment.service.masterdata;

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
    void resolveCode_shouldReturnEmpty_whenInputMissing() {
        assertThat(service.resolveCode(null, "x")).isEmpty();
        assertThat(service.resolveCode("CAT", null)).isEmpty();
        assertThat(service.resolveCode(" ", "x")).isEmpty();
    }

    @Test
    void resolveCode_shouldReturnEmpty_whenCacheUnavailable() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(null);

        assertThat(service.resolveCode("CAT", "DXB")).isEmpty();
    }

    @Test
    void resolveCode_shouldMatchByCode() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.getList(any(), eq(MasterCategoryValueRow.class)))
                .thenReturn(Optional.of(List.of(
                        new MasterCategoryValueRow(null, null, "DXB", "Dubai", "Dubai Name", null, null, null, null))));

        assertThat(service.resolveCode("CAT", "dxb")).contains("DXB");
    }

    @Test
    void resolveCode_shouldMatchByLabelWhenCodeDoesNotMatch() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.getList(any(), eq(MasterCategoryValueRow.class)))
                .thenReturn(Optional.of(List.of(
                        new MasterCategoryValueRow(null, null, "DXB", "Dubai Label", null, null, null, null, null))));

        assertThat(service.resolveCode("CAT", "dubai label")).contains("DXB");
    }

    @Test
    void resolveCode_shouldMatchByNameWhenLabelDoesNotMatch() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.getList(any(), eq(MasterCategoryValueRow.class)))
                .thenReturn(Optional.of(List.of(
                        new MasterCategoryValueRow(null, null, "RUH", null, "Riyadh", null, null, null, null))));

        assertThat(service.resolveCode("CAT", "riyadh")).contains("RUH");
    }

    @Test
    void resolveCode_shouldReturnEmpty_whenCacheLookupFails() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.getList(any(), eq(MasterCategoryValueRow.class)))
                .thenThrow(new RuntimeException("redis down"));

        assertThat(service.resolveCode("CAT", "DXB")).isEmpty();
    }
}
