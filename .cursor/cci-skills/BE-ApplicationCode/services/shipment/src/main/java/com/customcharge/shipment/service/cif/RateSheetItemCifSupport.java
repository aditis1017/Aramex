package com.customcharge.shipment.service.cif;

import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.shipment.service.charge.processing.model.ResolvedRateSheet;

public final class RateSheetItemCifSupport {

    private RateSheetItemCifSupport() {
    }

    public static boolean hasCifRate(RateSheetItem item) {
        if (item == null || item.getRates() == null || item.getRates().isEmpty()) {
            return false;
        }
        return item.getRates().stream()
                .anyMatch(rate -> rate.getPercentageOf() == PercentageOf.CIF_VALUE);
    }

    public static boolean resolvedSheetHasCifRate(ResolvedRateSheet sheet) {
        if (sheet == null || sheet.items() == null) {
            return false;
        }
        return sheet.items().stream().anyMatch(RateSheetItemCifSupport::hasCifRate);
    }
}
