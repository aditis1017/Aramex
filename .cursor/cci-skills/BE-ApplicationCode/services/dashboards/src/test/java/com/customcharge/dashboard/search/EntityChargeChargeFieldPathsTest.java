package com.customcharge.dashboard.search;

import com.customcharge.persistencelayer.search.SearchCriteria;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntityChargeChargeFieldPathsTest {

    @Test
    void remapToChargePath_shouldMapViewFieldsToChargePaths() {
        SearchCriteria source = new SearchCriteria();
        source.setFieldName("chargetype");
        source.setCondition("in");
        source.setValue(List.of("ECD"));

        SearchCriteria remapped = EntityChargeChargeFieldPaths.remapToChargePath(source);

        assertThat(remapped.getFieldName()).isEqualTo("chargeCode");
        assertThat(remapped.getCondition()).isEqualTo("in");
        assertThat(remapped.getValue()).isEqualTo(List.of("ECD"));
    }

    @Test
    void remapToChargePath_shouldReturnNullForEntityField() {
        SearchCriteria source = new SearchCriteria();
        source.setFieldName("entity");
        source.setCondition("in");
        source.setValue(List.of("DXB"));

        assertThat(EntityChargeChargeFieldPaths.remapToChargePath(source)).isNull();
    }
}
