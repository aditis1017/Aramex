package com.customcharge.dashboard.search;

import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.entities.CategoryValue;
import com.customcharge.persistencelayer.search.SearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityChargeCriteriaNormalizerTest {

    @Mock
    private CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;

    private EntityChargeCriteriaNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new EntityChargeCriteriaNormalizer(categoryCodesGlobalDataCacheService);
    }

    @Test
    void normalize_shouldMapCashAccountTypeInArray() {
        SearchCriteria filter = new SearchCriteria();
        filter.setFieldName("accountType");
        filter.setCondition("in");
        filter.setValue(List.of("Cash"));

        EntityChargeNormalizedFilters result = normalizer.normalize(List.of(filter));

        assertThat(result.criteria()).isEmpty();
        assertThat(result.accountTypes()).containsExactly("cash");
    }

    @Test
    void normalize_shouldMapAccountAccountTypeInArray() {
        SearchCriteria filter = new SearchCriteria();
        filter.setFieldName("accountType");
        filter.setCondition("in");
        filter.setValue(List.of("Account"));

        EntityChargeNormalizedFilters result = normalizer.normalize(List.of(filter));

        assertThat(result.accountTypes()).containsExactly("account");
    }

    @Test
    void normalize_shouldMapBoeFilterInArray() {
        SearchCriteria filter = new SearchCriteria();
        filter.setFieldName("boe");
        filter.setCondition("in");
        filter.setValue(List.of("BOE-001", "BOE-002"));

        EntityChargeNormalizedFilters result = normalizer.normalize(List.of(filter));

        assertThat(result.criteria()).isEmpty();
        assertThat(result.boeNumbers()).containsExactly("BOE-001", "BOE-002");
    }

    @Test
    void normalize_shouldResolveBusinessTypeCodesForInArray() {
        CategoryValue categoryValue = new CategoryValue();
        categoryValue.setCode("E-Commerce_BT");
        categoryValue.setName("E-Commerce");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("E-Commerce_BT"))
                .thenReturn(categoryValue);

        SearchCriteria filter = new SearchCriteria();
        filter.setFieldName("businessType");
        filter.setCondition("in");
        filter.setValue(List.of("E-Commerce_BT"));

        EntityChargeNormalizedFilters result = normalizer.normalize(List.of(filter));

        assertThat(result.criteria()).hasSize(1);
        assertThat(result.criteria().getFirst().getFieldName()).isEqualTo("businesstype");
        assertThat(result.criteria().getFirst().getCondition()).isEqualTo("in");
        assertThat(result.criteria().getFirst().getValue()).isEqualTo(List.of("E-Commerce"));
    }
}
