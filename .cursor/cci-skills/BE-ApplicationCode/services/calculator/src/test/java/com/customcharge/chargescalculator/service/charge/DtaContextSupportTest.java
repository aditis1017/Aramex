package com.customcharge.chargescalculator.service.charge;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.dto.ServiceChargeDescriptionMappingDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DtaContextSupportTest {

    @Test
    void remapByDescription_shouldRenameAndSumMatchingKeys() {
        Map<String, BigDecimal> raw = Map.of(
                "customs duty", new BigDecimal("10"),
                "other fee", new BigDecimal("2"));
        List<ServiceChargeDescriptionMappingDto> mappings = List.of(
                new ServiceChargeDescriptionMappingDto("Customs Duty", "C1", "Import Customs", "C2"),
                new ServiceChargeDescriptionMappingDto("customs duty", "C1", "Import Customs", "C2"));

        Map<String, BigDecimal> remapped = DtaContextSupport.remapByDescription(raw, mappings);

        assertThat(remapped.get("import customs")).isEqualByComparingTo("20");
        assertThat(remapped.get("other fee")).isEqualByComparingTo("2");
    }

    @Test
    void remapByDescription_shouldKeepUnmatchedKeys() {
        Map<String, BigDecimal> raw = Map.of("storage", new BigDecimal("5"));
        Map<String, BigDecimal> remapped = DtaContextSupport.remapByDescription(raw, List.of());

        assertThat(remapped.get("storage")).isEqualByComparingTo("5");
    }

    @Test
    void dtaLineContext_shouldMergeCaptureAndPersistRawMap() {
        Map<String, BigDecimal> capture = new LinkedHashMap<>();
        DtaContextSupport.DtaLineContext context =
                new DtaContextSupport.DtaLineContext(Map.of(), false, false, capture);
        ChargeCalculationContext calculationContext = CalculatorTestDataFactory.chargeContext();

        context.mergeCapture("Customs Duty", new BigDecimal("10"));
        context.mergeCapture("customs duty", new BigDecimal("2"));
        context.mergeCapture(null, BigDecimal.ONE);
        context.mergeCapture(" ", BigDecimal.ONE);
        context.persist(calculationContext);

        assertThat(capture.get("customs duty")).isEqualByComparingTo("12");
        assertThat(calculationContext.getCalculatedCharges())
                .containsEntry(BatchProcessingConstants.CALCULATED_CHARGES_KEY_DTA_REVENUE, capture);
    }

    @Test
    void dtaLineContext_shouldIgnorePersist_whenCaptureMissing() {
        DtaContextSupport.DtaLineContext context =
                new DtaContextSupport.DtaLineContext(Map.of(), false, false, null);
        ChargeCalculationContext calculationContext = CalculatorTestDataFactory.chargeContext();

        context.persist(calculationContext);

        assertThat(calculationContext.getCalculatedCharges()).isEmpty();
    }

    @Test
    void forCharge_shouldBuildDtaCaptureForDtaCharge() {
        DtaContextSupport support = new DtaContextSupport(mock(com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository.class));
        ChargeLineSupport chargeLineSupport = mock(ChargeLineSupport.class);
        ChargeCalculationContext ctx = CalculatorTestDataFactory.chargeContext();
        when(chargeLineSupport.resolveDestinationEntityCode(ctx.getShipment())).thenReturn("DXB");

        DtaContextSupport.DtaLineContext result = support.forCharge(
                ctx, ctx.getShipment(), BatchProcessingConstants.CHARGE_CODE_DTA, "DXB", chargeLineSupport);

        assertThat(result.useDtaAmounts()).isFalse();
        assertThat(result.rawCapture()).isNotNull();
    }

    @Test
    void remapByDescription_shouldIncludeAllMappedTargets_evenWhenSourceMissing() {
        Map<String, BigDecimal> raw = Map.of("other fee", new BigDecimal("2"));
        List<ServiceChargeDescriptionMappingDto> mappings = List.of(
                new ServiceChargeDescriptionMappingDto("Customs Duty", "C1", "Import Customs", "C2"),
                new ServiceChargeDescriptionMappingDto("Storage Fee", "C3", "Warehouse", "C4"));

        Map<String, BigDecimal> remapped = DtaContextSupport.remapByDescription(raw, mappings);

        assertThat(remapped.get("import customs")).isEqualByComparingTo("0");
        assertThat(remapped.get("warehouse")).isEqualByComparingTo("0");
        assertThat(remapped.get("other fee")).isEqualByComparingTo("2");
    }

    @Test
    void forCharge_shouldEnableCaptureForEcsEstimate() {
        DtaContextSupport support = new DtaContextSupport(mock(com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository.class));
        ChargeLineSupport chargeLineSupport = mock(ChargeLineSupport.class);
        ChargeCalculationContext ctx = CalculatorTestDataFactory.chargeContext();
        when(chargeLineSupport.resolveDestinationEntityCode(ctx.getShipment())).thenReturn("DXB");

        DtaContextSupport.DtaLineContext result = support.forCharge(
                ctx, ctx.getShipment(), BatchProcessingConstants.CHARGE_CODE_ECS, "RUH", chargeLineSupport);

        assertThat(result.rawCapture()).isNotNull();
        assertThat(result.siblingChargeContext()).isFalse();
    }

    @Test
    void forCharge_shouldUseSiblingContext_forDtsEstimate() {
        DtaContextSupport support = new DtaContextSupport(mock(com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository.class));
        ChargeLineSupport chargeLineSupport = mock(ChargeLineSupport.class);
        ChargeCalculationContext ctx = CalculatorTestDataFactory.chargeContext();
        ctx.getCalculatedCharges().put(
                BatchProcessingConstants.CALCULATED_CHARGES_KEY_DTA_REVENUE,
                Map.of("import customs", new BigDecimal("110")));
        when(chargeLineSupport.resolveDestinationEntityCode(ctx.getShipment())).thenReturn("DXB");

        DtaContextSupport.DtaLineContext result = support.forCharge(
                ctx, ctx.getShipment(), BatchProcessingConstants.CHARGE_CODE_DTS, "RUH", chargeLineSupport);

        assertThat(result.siblingChargeContext()).isTrue();
        assertThat(result.amountsByDescription().get("import customs")).isEqualByComparingTo("110");
        assertThat(result.rawCapture()).isNull();
    }

    @Test
    void remapByDescription_shouldSkipBlankTargetDescriptions() {
        Map<String, BigDecimal> raw = Map.of("fee", new BigDecimal("3"));
        List<ServiceChargeDescriptionMappingDto> mappings = List.of(
                new ServiceChargeDescriptionMappingDto("fee", "C1", " ", "C2"));

        Map<String, BigDecimal> remapped = DtaContextSupport.remapByDescription(raw, mappings);

        assertThat(remapped).isEmpty();
    }
}
