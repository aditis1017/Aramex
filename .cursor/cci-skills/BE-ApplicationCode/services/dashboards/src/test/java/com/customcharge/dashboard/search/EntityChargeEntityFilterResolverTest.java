package com.customcharge.dashboard.search;

import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.search.SearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityChargeEntityFilterResolverTest {

    @Mock
    private EntityRepository entityRepository;

    @InjectMocks
    private EntityChargeEntityFilterResolver resolver;

    @Test
    void resolve_shouldMapEntityCodesToIdsAndStripEntityCriterion() {
        SearchCriteria entityFilter = new SearchCriteria();
        entityFilter.setFieldName("entity");
        entityFilter.setCondition("in");
        entityFilter.setValue(List.of("dxb"));

        SearchCriteria chargeType = new SearchCriteria();
        chargeType.setFieldName("chargetype");
        chargeType.setCondition("in");
        chargeType.setValue(List.of("ECD"));

        when(entityRepository.findIdsByCodeInIgnoreCase(List.of("DXB"))).thenReturn(List.of(42L));

        Optional<EntityChargeEntityFilterResolver.EntityFilterResolution> resolution =
                resolver.resolve(List.of(entityFilter, chargeType));

        assertThat(resolution).isPresent();
        assertThat(resolution.get().entityFilterApplied()).isTrue();
        assertThat(resolution.get().entityIds()).containsExactly(42L);
        assertThat(resolution.get().criteriaWithoutEntity()).containsExactly(chargeType);
        verify(entityRepository).findIdsByCodeInIgnoreCase(List.of("DXB"));
    }

    @Test
    void resolveScope_shouldReturnUnscopedResolution_whenEntityFilterMissing() {
        SearchCriteria chargeType = new SearchCriteria();
        chargeType.setFieldName("chargetype");
        chargeType.setCondition("in");
        chargeType.setValue(List.of("ECD"));

        EntityChargeEntityFilterResolver.EntityFilterResolution scope =
                resolver.resolveScope(List.of(chargeType));

        assertThat(scope.entityFilterApplied()).isFalse();
        assertThat(scope.entityIds()).isEmpty();
        assertThat(scope.criteriaWithoutEntity()).containsExactly(chargeType);
    }
}
