package com.customcharge.shipment.service.cif;

import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.shipment.service.charge.ShipmentChargeProcessingOrder;
import com.customcharge.shipment.service.charge.UnifiedChargeLineItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentCifChargeRecalculationServiceTest {

    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private UnifiedChargeLineItemService unifiedChargeLineItemService;
    @Mock
    private ChargeCifValueSupport chargeCifValueSupport;
    @Mock
    private ShipmentCifCategoryReevaluationService categoryReevaluationService;

    @InjectMocks
    private ShipmentCifChargeRecalculationService service;

    @BeforeEach
    void setUpCategoryEvaluation() {
        when(categoryReevaluationService.evaluate(any(Shipment.class)))
                .thenReturn(new ShipmentCifCategoryReevaluationService.CategoryReevaluationResult(
                        Sh267ChargeRecalculationMode.CATEGORY_UNCHANGED_NORMAL, "LV", "LV"));
    }

    @Test
    void recalculate_shouldUseUnifiedLineItemRecalcAndApplyCifSnapshots() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = spy(new Shipment());
        when(shipment.getId()).thenReturn(shipmentId);
        shipment.setCategoryType("LV");
        shipment.setHawbNumber("HAWB-1");
        shipment.setCifValue(25.0);
        shipment.setCifValueCurrency("USD");

        Charge ecd = charge("ECD");
        Charge dtc = charge("DTC");
        when(chargeRepository.findByShipment_IdWithEntity(shipmentId)).thenReturn(List.of(ecd, dtc));

        service.recalculate(shipment, new HawbComment2CifParser.ParsedCif(25.0, "USD"));

        verify(unifiedChargeLineItemService).reapplyFullRateSheetAfterCategoryChange(
                eq(shipment), eq(ShipmentChargeProcessingOrder.sort(List.of(ecd, dtc))), any());
        verify(chargeCifValueSupport).addSh267CifContribution(shipment, ecd, 25.0, "USD");
        verify(chargeCifValueSupport).addSh267CifContribution(shipment, dtc, 25.0, "USD");
        verify(chargeCifValueSupport, never()).applySh013CogSnapshot(any(), any());
        verify(chargeCifValueSupport, never()).applySh267ShippingChargesSnapshot(any(), any(), any(Double.class), any());
        verify(chargeRepository).saveAll(ShipmentChargeProcessingOrder.sort(List.of(ecd, dtc)));
    }

    @Test
    void recalculate_shouldUseUnifiedLineItemRecalcForHvShipments() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = spy(new Shipment());
        when(shipment.getId()).thenReturn(shipmentId);
        shipment.setCategoryType("HV");
        shipment.setHawbNumber("HAWB-2");
        shipment.setCifValue(30.0);
        shipment.setCifValueCurrency("USD");

        Charge ecd = charge("ECD");
        when(chargeRepository.findByShipment_IdWithEntity(shipmentId)).thenReturn(List.of(ecd));

        service.recalculate(shipment, new HawbComment2CifParser.ParsedCif(30.0, "USD"));

        verify(unifiedChargeLineItemService).reapplyFullRateSheetAfterCategoryChange(eq(shipment), anyList(), any());
        verify(chargeCifValueSupport).addSh267CifContribution(shipment, ecd, 30.0, "USD");
        verify(chargeRepository).saveAll(List.of(ecd));
    }

    @Test
    void recalculate_shouldSkipWhenAlreadyExempted() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = spy(new Shipment());
        when(shipment.getId()).thenReturn(shipmentId);
        when(categoryReevaluationService.evaluate(shipment))
                .thenReturn(new ShipmentCifCategoryReevaluationService.CategoryReevaluationResult(
                        Sh267ChargeRecalculationMode.SKIP_ALREADY_EXEMPTED, "Exempted", "Exempted"));

        service.recalculate(shipment, new HawbComment2CifParser.ParsedCif(10.0, "USD"));

        verify(unifiedChargeLineItemService, never()).reapplyFullRateSheetAfterCategoryChange(any(), anyList(), any());
        verify(chargeCifValueSupport, never()).addSh267CifContribution(any(), any(), any(Double.class), any());
        verify(chargeRepository, never()).saveAll(anyList());
    }

    @Test
    void recalculate_shouldReapplyFullRateSheetWhenLvToHv() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = spy(new Shipment());
        when(shipment.getId()).thenReturn(shipmentId);
        shipment.setCategoryType("HV");
        when(categoryReevaluationService.evaluate(shipment))
                .thenReturn(new ShipmentCifCategoryReevaluationService.CategoryReevaluationResult(
                        Sh267ChargeRecalculationMode.LV_TO_HV_NORMAL, "LV", "HV"));

        Charge ecd = charge("ECD");
        when(chargeRepository.findByShipment_IdWithEntity(shipmentId)).thenReturn(List.of(ecd));

        service.recalculate(shipment, new HawbComment2CifParser.ParsedCif(10.0, "USD"));

        verify(unifiedChargeLineItemService).reapplyFullRateSheetAfterCategoryChange(eq(shipment), anyList(), eq("HV"));
        verify(unifiedChargeLineItemService, never()).recalculateLineItems(any(), anyList(), any());
    }

    @Test
    void recalculate_shouldUseActualizedPathWhenShipmentActualized() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = spy(new Shipment());
        when(shipment.getId()).thenReturn(shipmentId);
        shipment.setCategoryType("LV");
        when(categoryReevaluationService.evaluate(shipment))
                .thenReturn(new ShipmentCifCategoryReevaluationService.CategoryReevaluationResult(
                        Sh267ChargeRecalculationMode.ACTUALIZED_CIF_ONLY, "LV", "LV"));

        Charge ecd = charge("ECD");
        ecd.setStatus("ACTUALIZED");
        when(chargeRepository.findByShipment_IdWithEntity(shipmentId)).thenReturn(List.of(ecd));

        service.recalculate(shipment, new HawbComment2CifParser.ParsedCif(10.0, "USD"));

        verify(unifiedChargeLineItemService).recalculateActualizedCifOnlyLineItems(eq(shipment), anyList(), eq("LV"));
    }

    @Test
    void recalculate_shouldUseActualizedLvToHvPathWhenCategoryChanges() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = spy(new Shipment());
        when(shipment.getId()).thenReturn(shipmentId);
        shipment.setCategoryType("HV");
        when(categoryReevaluationService.evaluate(shipment))
                .thenReturn(new ShipmentCifCategoryReevaluationService.CategoryReevaluationResult(
                        Sh267ChargeRecalculationMode.ACTUALIZED_LV_TO_HV, "LV", "HV"));

        Charge ecd = charge("ECD");
        ecd.setStatus("ACTUALIZED");
        when(chargeRepository.findByShipment_IdWithEntity(shipmentId)).thenReturn(List.of(ecd));

        service.recalculate(shipment, new HawbComment2CifParser.ParsedCif(10.0, "USD"));

        verify(unifiedChargeLineItemService).recalculateActualizedLvToHvLineItems(eq(shipment), anyList(), eq("HV"));
    }

    @Test
    void recalculate_shouldUseResolvedHvCategoryWhenProductTypeStillLv() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = spy(new Shipment());
        when(shipment.getId()).thenReturn(shipmentId);
        shipment.setCategoryType("HV");
        shipment.setProductType("LV");
        when(categoryReevaluationService.evaluate(shipment))
                .thenReturn(new ShipmentCifCategoryReevaluationService.CategoryReevaluationResult(
                        Sh267ChargeRecalculationMode.LV_TO_HV_NORMAL, "LV", "HV"));

        Charge ecd = charge("ECD");
        when(chargeRepository.findByShipment_IdWithEntity(shipmentId)).thenReturn(List.of(ecd));

        service.recalculate(shipment, new HawbComment2CifParser.ParsedCif(10.0, "USD"));

        verify(unifiedChargeLineItemService).reapplyFullRateSheetAfterCategoryChange(eq(shipment), anyList(), eq("HV"));
    }

    @Test
    void recalculate_shouldUseResolvedCategoryWhenUnchangedAndProductTypeStillLv() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = spy(new Shipment());
        when(shipment.getId()).thenReturn(shipmentId);
        shipment.setCategoryType("HV");
        shipment.setProductType("LV");
        when(categoryReevaluationService.evaluate(shipment))
                .thenReturn(new ShipmentCifCategoryReevaluationService.CategoryReevaluationResult(
                        Sh267ChargeRecalculationMode.CATEGORY_UNCHANGED_NORMAL, "HV", "HV"));

        Charge ecd = charge("ECD");
        when(chargeRepository.findByShipment_IdWithEntity(shipmentId)).thenReturn(List.of(ecd));

        service.recalculate(shipment, new HawbComment2CifParser.ParsedCif(10.0, "USD"));

        verify(unifiedChargeLineItemService).reapplyFullRateSheetAfterCategoryChange(eq(shipment), anyList(), eq("HV"));
    }

    private static Charge charge(String code) {
        Charge charge = new Charge();
        charge.setChargeCode(code);
        return charge;
    }
}
