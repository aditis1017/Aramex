package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.constant.DomEntityConstants;
import com.customcharge.persistencelayer.entities.CategoryValue;
import com.customcharge.persistencelayer.repository.CategoryValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomEntityConfigResolverTest {

    private static final String CONSIGNEE_COUNTRY_CODE = "SA";
    private static final String DOM_ENTITY_VALUE_CODE = "DOM_ENTITY_SA";

    @Mock
    private ObjectProvider<CategoryCodesGlobalDataCacheService> categoryCodesCacheService;
    @Mock
    private CategoryValueRepository categoryValueRepository;
    @Mock
    private CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;

    private DomEntityConfigResolver domEntityConfigResolver;

    @BeforeEach
    void setUp() {
        domEntityConfigResolver = new DomEntityConfigResolver(categoryCodesCacheService, categoryValueRepository);
    }

    @Test
    void resolveDomEntityCode_shouldBuildKeyFromConsigneeCountryCodeAndReadFromCache() {
        when(categoryCodesCacheService.getIfAvailable()).thenReturn(categoryCodesGlobalDataCacheService);
        CategoryValue categoryValue = new CategoryValue();
        categoryValue.setName("RUH");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode(DOM_ENTITY_VALUE_CODE))
                .thenReturn(categoryValue);

        assertThat(domEntityConfigResolver.resolveDomEntityCode(CONSIGNEE_COUNTRY_CODE)).isEqualTo("RUH");
    }

    @Test
    void resolveDomEntityCode_shouldFallbackToDatabaseWhenCacheUnavailable() {
        when(categoryCodesCacheService.getIfAvailable()).thenReturn(null);
        CategoryValue categoryValue = new CategoryValue();
        categoryValue.setName(" JED ");
        categoryValue.setIsActive(true);
        when(categoryValueRepository.findByCode(DOM_ENTITY_VALUE_CODE))
                .thenReturn(Optional.of(categoryValue));

        assertThat(domEntityConfigResolver.resolveDomEntityCode(CONSIGNEE_COUNTRY_CODE)).isEqualTo("JED");
    }

    @Test
    void resolveDomEntityCode_shouldReturnDefaultWhenConsigneeCountryMissing() {
        assertThat(domEntityConfigResolver.resolveDomEntityCode(null))
                .isEqualTo(DomEntityConstants.DEFAULT_DOM_ENTITY_CODE);
        assertThat(domEntityConfigResolver.resolveDomEntityCode(" "))
                .isEqualTo(DomEntityConstants.DEFAULT_DOM_ENTITY_CODE);
    }

    @Test
    void formatDomEntityValidationMessage_shouldUseConfiguredEntity() {
        when(categoryCodesCacheService.getIfAvailable()).thenReturn(categoryCodesGlobalDataCacheService);
        CategoryValue categoryValue = new CategoryValue();
        categoryValue.setName("RUH");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode(DOM_ENTITY_VALUE_CODE))
                .thenReturn(categoryValue);

        assertThat(domEntityConfigResolver.formatDomEntityValidationMessage(CONSIGNEE_COUNTRY_CODE))
                .isEqualTo(DomEntityConstants.formatDomEntityValidationMessage("RUH"));
    }
}
