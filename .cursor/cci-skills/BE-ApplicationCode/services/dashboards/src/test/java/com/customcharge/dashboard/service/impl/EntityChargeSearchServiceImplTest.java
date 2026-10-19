package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.config.DashboardEntityChargeProperties;
import com.customcharge.dashboard.dto.EntityChargeGridRowDTO;
import com.customcharge.dashboard.mapper.EntityChargeDashboardViewMapper;
import com.customcharge.dashboard.search.EntityChargeCriteriaNormalizer;
import com.customcharge.dashboard.search.EntityChargeEntityFilterResolver;
import com.customcharge.dashboard.search.EntityChargeNormalizedFilters;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.EntityChargeDashboardView;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.EntityChargeDashboardViewRepository;
import com.customcharge.persistencelayer.search.SearchCriteria;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityChargeSearchServiceImplTest {

    @Mock
    private EntityChargeDashboardViewRepository entityChargeDashboardViewRepository;
    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private EntityChargeCriteriaNormalizer criteriaNormalizer;
    @Mock
    private EntityChargeDashboardViewMapper dashboardViewMapper;
    @Mock
    private EntityChargeEntityFilterResolver entityFilterResolver;
    @Mock
    private EntityManager entityManager;
    @Mock
    private DashboardEntityChargeProperties dashboardEntityChargeProperties;

    @InjectMocks
    private EntityChargeSearchServiceImpl entityChargeSearchService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(dashboardEntityChargeProperties.getQueryTimeoutMs()).thenReturn(0);
    }

    @Test
    void search_shouldUseChargeFirstPagination_whenEntityFilterPresentWithoutAccountFilters() {
        UUID chargeId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        SearchCriteria entityFilter = new SearchCriteria();
        entityFilter.setFieldName("entity");
        entityFilter.setCondition("in");
        entityFilter.setValue(List.of("DXB"));

        EntityChargeNormalizedFilters normalized =
                new EntityChargeNormalizedFilters(List.of(entityFilter), List.of(), List.of(), List.of());
        when(criteriaNormalizer.normalizeForView(any())).thenReturn(normalized);
        when(entityFilterResolver.resolveScope(normalized.criteria()))
                .thenReturn(new EntityChargeEntityFilterResolver.EntityFilterResolution(
                        List.of(7L), true, List.of()));

        Charge charge = org.mockito.Mockito.mock(Charge.class);
        when(charge.getId()).thenReturn(chargeId);
        when(chargeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(charge), PageRequest.of(0, 10), 1L));

        EntityChargeDashboardView viewRow = new EntityChargeDashboardView();
        viewRow.setChargeId(chargeId);
        when(entityChargeDashboardViewRepository.findAllById(any(Iterable.class)))
                .thenReturn(List.of(viewRow));

        EntityChargeGridRowDTO dto = new EntityChargeGridRowDTO();
        when(dashboardViewMapper.toGridRow(viewRow)).thenReturn(dto);

        Page<EntityChargeGridRowDTO> result = entityChargeSearchService.search(List.of(entityFilter), 0, 10);

        assertThat(result.getContent()).containsExactly(dto);
        assertThat(result.getTotalElements()).isOne();
        verify(chargeRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(chargeRepository, never()).count(any(Specification.class));
        verify(entityChargeDashboardViewRepository).findAllById(any(Iterable.class));
        verify(entityChargeDashboardViewRepository, never()).count(any(Specification.class));
        verify(entityChargeDashboardViewRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_shouldUseChargeFirstPagination_whenOnlyChargeTypeFilterPresent() {
        UUID chargeId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        SearchCriteria chargeType = new SearchCriteria();
        chargeType.setFieldName("chargetype");
        chargeType.setCondition("in");
        chargeType.setValue(List.of("ECD"));

        EntityChargeNormalizedFilters normalized =
                new EntityChargeNormalizedFilters(List.of(chargeType), List.of(), List.of(), List.of());
        when(criteriaNormalizer.normalizeForView(any())).thenReturn(normalized);
        when(entityFilterResolver.resolveScope(normalized.criteria()))
                .thenReturn(new EntityChargeEntityFilterResolver.EntityFilterResolution(
                        List.of(), false, List.of(chargeType)));

        Charge charge = org.mockito.Mockito.mock(Charge.class);
        when(charge.getId()).thenReturn(chargeId);
        when(chargeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(charge), PageRequest.of(0, 10), 42L));

        EntityChargeDashboardView viewRow = new EntityChargeDashboardView();
        viewRow.setChargeId(chargeId);
        when(entityChargeDashboardViewRepository.findAllById(any(Iterable.class)))
                .thenReturn(List.of(viewRow));

        EntityChargeGridRowDTO dto = new EntityChargeGridRowDTO();
        when(dashboardViewMapper.toGridRow(viewRow)).thenReturn(dto);

        Page<EntityChargeGridRowDTO> result = entityChargeSearchService.search(List.of(chargeType), 0, 10);

        assertThat(result.getContent()).containsExactly(dto);
        assertThat(result.getTotalElements()).isEqualTo(42L);
        verify(chargeRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(chargeRepository, never()).count(any(Specification.class));
        verify(entityChargeDashboardViewRepository).findAllById(any(Iterable.class));
        verify(entityChargeDashboardViewRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_shouldFallBackToViewPagination_whenAccountNumberFilterPresent() {
        SearchCriteria entityFilter = new SearchCriteria();
        entityFilter.setFieldName("entity");
        entityFilter.setCondition("in");
        entityFilter.setValue(List.of("DXB"));

        EntityChargeNormalizedFilters normalized = new EntityChargeNormalizedFilters(
                List.of(entityFilter), List.of("12345"), List.of(), List.of());
        when(criteriaNormalizer.normalizeForView(any())).thenReturn(normalized);

        EntityChargeDashboardView viewRow = new EntityChargeDashboardView();
        when(entityChargeDashboardViewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(viewRow)));
        when(dashboardViewMapper.toGridRow(viewRow)).thenReturn(new EntityChargeGridRowDTO());

        entityChargeSearchService.search(List.of(entityFilter), 0, 10);

        verify(entityChargeDashboardViewRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(chargeRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}
