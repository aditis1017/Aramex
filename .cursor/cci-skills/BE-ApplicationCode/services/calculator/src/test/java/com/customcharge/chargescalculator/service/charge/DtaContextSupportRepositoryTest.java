package com.customcharge.chargescalculator.service.charge;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.persistencelayer.dto.ServiceChargeDescriptionMappingDto;
import com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DtaContextSupportRepositoryTest {

    @Mock
    private ServiceChargeMappingRepository serviceChargeMappingRepository;
    private DtaContextSupport dtaContextSupport;

    @BeforeEach
    void setUp() {
        dtaContextSupport = new DtaContextSupport(serviceChargeMappingRepository);
    }

    @Test
    void remapForDownstream_shouldQueryByFromEntityToEntityAndChargeType() {
        Map<String, BigDecimal> raw = Map.of("customs duty", new BigDecimal("10"));
        List<ServiceChargeDescriptionMappingDto> mappings = List.of(
                new ServiceChargeDescriptionMappingDto("Customs Duty", "C1", "Import Customs", "C2"));
        when(serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                "DXB", "RUH", "ECS"))
                .thenReturn(mappings);

        Map<String, BigDecimal> remapped =
                dtaContextSupport.resolveForDownstreamCharge("DXB", "RUH", "ECS", raw);

        verify(serviceChargeMappingRepository).findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                "DXB", "RUH", "ECS");
        assertThat(remapped.get("import customs")).isEqualByComparingTo("10");
    }

    @Test
    void resolveForDownstreamCharge_shouldUseEcsMappingChargeType_forDtsOnActual() {
        Map<String, BigDecimal> raw = Map.of("fee", new BigDecimal("5"));
        when(serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                "DXB", "RUH", "ECS"))
                .thenReturn(List.of());
        when(serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                BatchProcessingConstants.GLOBAL_ENTITY_CODE,
                BatchProcessingConstants.GLOBAL_ENTITY_CODE,
                "ECS"))
                .thenReturn(List.of());

        dtaContextSupport.resolveForDownstreamCharge("DXB", "RUH", "DTS", raw, true);

        verify(serviceChargeMappingRepository).findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                "DXB", "RUH", "ECS");
        verify(serviceChargeMappingRepository).findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                BatchProcessingConstants.GLOBAL_ENTITY_CODE,
                BatchProcessingConstants.GLOBAL_ENTITY_CODE,
                "ECS");
    }

    @Test
    void resolveForDownstreamCharge_shouldRemapInboundToOutbound_forDtsUsingEcsMapping() {
        Map<String, BigDecimal> raw = Map.of("customs duty inbound", new BigDecimal("110"));
        List<ServiceChargeDescriptionMappingDto> globalMappings = List.of(
                new ServiceChargeDescriptionMappingDto(
                        "Customs Duty Inbound", "C1", "Customs Duty Outbound", "C2"));
        when(serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                "RUH", "DXB", "ECS"))
                .thenReturn(List.of());
        when(serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                BatchProcessingConstants.GLOBAL_ENTITY_CODE,
                BatchProcessingConstants.GLOBAL_ENTITY_CODE,
                "ECS"))
                .thenReturn(globalMappings);

        Map<String, BigDecimal> remapped =
                dtaContextSupport.resolveForDownstreamCharge("RUH", "DXB", "DTS", raw, true);

        assertThat(remapped.get("customs duty outbound")).isEqualByComparingTo("110");
    }

    @Test
    void remapForDownstream_shouldFallbackToGlobalMapper_whenEntityMappingMissing() {
        Map<String, BigDecimal> raw = Map.of("customs duty", new BigDecimal("10"));
        List<ServiceChargeDescriptionMappingDto> globalMappings = List.of(
                new ServiceChargeDescriptionMappingDto("Customs Duty", "C1", "Import Customs", "C2"));
        when(serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                "DXB", "RUH", "DTS"))
                .thenReturn(List.of());
        when(serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                BatchProcessingConstants.GLOBAL_ENTITY_CODE,
                BatchProcessingConstants.GLOBAL_ENTITY_CODE,
                "DTS"))
                .thenReturn(globalMappings);

        Map<String, BigDecimal> remapped =
                dtaContextSupport.remapForDownstream("DXB", "RUH", "DTS", raw);

        assertThat(remapped.get("import customs")).isEqualByComparingTo("10");
    }

    @Test
    void resolveForDownstreamCharge_shouldReturnEmpty_whenRawContextMissing() {
        assertThat(dtaContextSupport.resolveForDownstreamCharge("DXB", "RUH", "ECS", null)).isEmpty();
        assertThat(dtaContextSupport.resolveForDownstreamCharge("DXB", "RUH", "ECS", Map.of())).isEmpty();
    }

    @Test
    void resolveForDownstreamCharge_shouldReturnRawContext_whenChargeTypeNotMapped() {
        Map<String, BigDecimal> raw = Map.of("fee", new BigDecimal("4"));

        assertThat(dtaContextSupport.resolveForDownstreamCharge("DXB", "RUH", "DTC", raw))
                .isEqualTo(raw);
    }

    @Test
    void remapForDownstream_shouldCopyRaw_whenEntityCodesBlank() {
        Map<String, BigDecimal> raw = Map.of("fee", new BigDecimal("7"));

        assertThat(dtaContextSupport.remapForDownstream(" ", "RUH", "ECS", raw)).isEqualTo(raw);
        assertThat(dtaContextSupport.remapForDownstream("DXB", " ", "ECS", raw)).isEqualTo(raw);
        assertThat(dtaContextSupport.remapForDownstream("DXB", "RUH", " ", raw)).isEqualTo(raw);
    }
}
