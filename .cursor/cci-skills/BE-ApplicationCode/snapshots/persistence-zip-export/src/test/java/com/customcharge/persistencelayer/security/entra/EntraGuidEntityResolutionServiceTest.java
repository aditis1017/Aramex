package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.entities.EntraGuidEntityMap;
import com.customcharge.persistencelayer.repository.EntraGuidEntityMapRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntraGuidEntityResolutionServiceTest {

    @Mock
    private EntraGuidEntityMapRepository entraGuidEntityMapRepository;

    @Test
    void resolveForPersistence_mapsGuidToEntityCode() {
        EntraGuidEntityMap row = new EntraGuidEntityMap();
        row.setGuid("c4813d11-92bb-4c93-bcc6-596989b6613d");
        row.setEntityCode("NIC");
        when(entraGuidEntityMapRepository.findAll()).thenReturn(List.of(row));

        EntraGuidEntityResolutionService service = new EntraGuidEntityResolutionService(entraGuidEntityMapRepository);
        service.reloadCache();

        assertThat(service.resolveForPersistence("C4813D11-92BB-4C93-BCC6-596989B6613D")).isEqualTo("NIC");
    }

    @Test
    void resolveForPersistence_leavesUnknownGuidUnchanged() {
        when(entraGuidEntityMapRepository.findAll()).thenReturn(List.of());
        EntraGuidEntityResolutionService service = new EntraGuidEntityResolutionService(entraGuidEntityMapRepository);
        service.reloadCache();
        assertThat(service.resolveForPersistence("00000000-0000-0000-0000-000000000000"))
                .isEqualTo("00000000-0000-0000-0000-000000000000");
    }

    @Test
    void normalizeObjectIdKey_acceptsBraces() {
        assertThat(EntraGuidEntityResolutionService.normalizeObjectIdKey("{c4813d11-92bb-4c93-bcc6-596989b6613d}"))
                .isEqualTo("c4813d11-92bb-4c93-bcc6-596989b6613d");
        assertThat(EntraGuidEntityResolutionService.normalizeObjectIdKey("not-a-guid")).isNull();
    }
}
