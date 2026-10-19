package com.customcharge.shipment.service.cif;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.enums.RateType;
import com.customcharge.shipment.service.charge.processing.model.ResolvedRateSheet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateSheetItemCifSupportTest {

    @Test
    void hasCifRate_shouldDetectCifPercentage() {
        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateType(RateType.PERCENTAGE);
        rate.setPercentageOf(PercentageOf.CIF_VALUE);
        RateSheetItem item = new RateSheetItem();
        item.setRates(List.of(rate));

        assertThat(RateSheetItemCifSupport.hasCifRate(item)).isTrue();
        assertThat(RateSheetItemCifSupport.hasCifRate(null)).isFalse();
    }

    @Test
    void resolvedSheetHasCifRate_shouldScanItems() {
        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setPercentageOf(PercentageOf.CIF_VALUE);
        RateSheetItem item = new RateSheetItem();
        item.setRates(List.of(rate));
        ResolvedRateSheet sheet = new ResolvedRateSheet(new Account(), new RateSheet(), List.of(item));

        assertThat(RateSheetItemCifSupport.resolvedSheetHasCifRate(sheet)).isTrue();
        assertThat(RateSheetItemCifSupport.resolvedSheetHasCifRate(null)).isFalse();
    }

    @Test
    void hasCifRate_shouldReturnFalse_forNonCifRate() {
        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateType(RateType.FIXED);
        rate.setValue(BigDecimal.TEN);
        RateSheetItem item = new RateSheetItem();
        item.setRates(List.of(rate));

        assertThat(RateSheetItemCifSupport.hasCifRate(item)).isFalse();
    }
}
