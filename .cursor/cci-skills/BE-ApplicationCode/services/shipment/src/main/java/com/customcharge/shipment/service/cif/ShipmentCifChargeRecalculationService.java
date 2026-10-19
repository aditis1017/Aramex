package com.customcharge.shipment.service.cif;

import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.shipment.service.charge.ChargeCategorySupport;
import com.customcharge.shipment.service.charge.ShipmentChargeProcessingOrder;
import com.customcharge.shipment.service.charge.UnifiedChargeLineItemService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.customcharge.shipment.constants.LogMessages.CIF_RECALCULATION_SKIPPED_NO_CHARGES;
import static com.customcharge.shipment.constants.LogMessages.SH267_RECALCULATION_SKIPPED;
import static com.customcharge.shipment.constants.LogMessages.SH267_SKIPPED_ALREADY_EXEMPTED;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentCifChargeRecalculationService {

    ChargeRepository chargeRepository;
    UnifiedChargeLineItemService unifiedChargeLineItemService;
    ChargeCifValueSupport chargeCifValueSupport;
    ShipmentCifCategoryReevaluationService categoryReevaluationService;

    public void recalculate(Shipment shipment, HawbComment2CifParser.ParsedCif originalCif) {
        if (shipment == null || shipment.getId() == null || originalCif == null) {
            return;
        }

        ShipmentCifCategoryReevaluationService.CategoryReevaluationResult categoryResult =
                categoryReevaluationService.evaluate(shipment);
        if (categoryResult.mode() == Sh267ChargeRecalculationMode.SKIP_ALREADY_EXEMPTED) {
            log.warn(SH267_SKIPPED_ALREADY_EXEMPTED, shipment.getHawbNumber());
            return;
        }

        List<Charge> charges = chargeRepository.findByShipment_IdWithEntity(shipment.getId());
        if (charges.isEmpty()) {
            log.debug(CIF_RECALCULATION_SKIPPED_NO_CHARGES, shipment.getHawbNumber());
            return;
        }

        List<Charge> orderedCharges = ShipmentChargeProcessingOrder.sort(charges);
        String rateSheetCategoryType = resolveRateSheetCategoryType(shipment, categoryResult);
        switch (categoryResult.mode()) {
            case NEWLY_EXEMPTED -> unifiedChargeLineItemService.recalculateExemptedLineItems(
                    shipment, orderedCharges);
            case LV_TO_HV_NORMAL -> unifiedChargeLineItemService.reapplyFullRateSheetAfterCategoryChange(
                    shipment, orderedCharges, rateSheetCategoryType);
            case ACTUALIZED_CIF_ONLY -> unifiedChargeLineItemService.recalculateActualizedCifOnlyLineItems(
                    shipment, orderedCharges, rateSheetCategoryType);
            case ACTUALIZED_LV_TO_HV -> unifiedChargeLineItemService.recalculateActualizedLvToHvLineItems(
                    shipment, orderedCharges, rateSheetCategoryType);
            case CATEGORY_UNCHANGED_NORMAL -> unifiedChargeLineItemService.reapplyFullRateSheetAfterCategoryChange(
                    shipment, orderedCharges, rateSheetCategoryType);
            default -> log.debug(
                    SH267_RECALCULATION_SKIPPED,
                    shipment.getHawbNumber(),
                    categoryResult.mode());
        }

        if (categoryResult.mode() != Sh267ChargeRecalculationMode.NEWLY_EXEMPTED) {
            orderedCharges.forEach(charge -> applyCifSnapshots(shipment, charge, originalCif));
        }
        chargeRepository.saveAll(orderedCharges);
    }

    private void applyCifSnapshots(
            Shipment shipment, Charge charge, HawbComment2CifParser.ParsedCif originalCif) {
        chargeCifValueSupport.addSh267CifContribution(
                shipment, charge, originalCif.amount(), originalCif.currency());
    }

    /**
     * After LV to HV re-evaluation, use the resolved HV category for rate sheet lookup even when
     * {@link ChargeCategorySupport#captureRateSheetCategory} would still return LV from product type.
     */
    private static String resolveRateSheetCategoryType(
            Shipment shipment,
            ShipmentCifCategoryReevaluationService.CategoryReevaluationResult categoryResult) {
        if (categoryResult.mode() == Sh267ChargeRecalculationMode.LV_TO_HV_NORMAL
                || categoryResult.mode() == Sh267ChargeRecalculationMode.ACTUALIZED_LV_TO_HV) {
            if (StringUtils.hasText(categoryResult.resolvedCategoryType())) {
                return categoryResult.resolvedCategoryType().strip();
            }
        }
        if (categoryResult.mode() == Sh267ChargeRecalculationMode.CATEGORY_UNCHANGED_NORMAL
                && StringUtils.hasText(categoryResult.resolvedCategoryType())) {
            return categoryResult.resolvedCategoryType().strip();
        }
        return ChargeCategorySupport.captureRateSheetCategory(shipment);
    }
}
