package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.Shipment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateSheetItemMatchSupportTest {

    @Test
    void matchesRateSheetItem_shouldMatchWhenBusinessAndCategoryAreNull() {
        RateSheetItem item = new RateSheetItem();
        item.setShipmentType("DDU");
        Shipment shipment = new Shipment();
        shipment.setShipmentType("DDU");
        shipment.setShipmentBusinessType("B2B");
        shipment.setCategoryType("LV");

        assertThat(RateSheetItemMatchSupport.matchesRateSheetItem(item, shipment)).isTrue();
    }

    @Test
    void matchesRateSheetItem_shouldTreatAllAsWildcardForBusinessAndCategory() {
        RateSheetItem item = new RateSheetItem();
        item.setShipmentType("DDU");
        item.setBusinessType(RateSheetItemMatchSupport.VALUE_ALL);
        item.setCategoryType(RateSheetItemMatchSupport.VALUE_ALL);
        Shipment shipment = new Shipment();
        shipment.setShipmentType("DDU");
        shipment.setShipmentBusinessType("B2C");
        shipment.setCategoryType("HV");

        assertThat(RateSheetItemMatchSupport.matchesRateSheetItem(item, shipment)).isTrue();
    }

    @Test
    void matchesRateSheetItem_shouldTreatAllShipmentTypeAsWildcard() {
        RateSheetItem item = rateSheetItem("ALL", "B2B", "LV", "DUTY");
        Shipment shipment = shipment("DDP", "B2B", "LV");

        assertThat(RateSheetItemMatchSupport.matchesRateSheetItem(item, shipment)).isTrue();
    }

    @Test
    void matchesRateSheetItem_shouldMatchCatchAllRowForAnyShipmentCombination() {
        RateSheetItem item = rateSheetItem("ALL", "ALL", "ALL", "DUTY");

        assertThat(RateSheetItemMatchSupport.matchesRateSheetItem(item, shipment("DDU", "B2B", "LV")))
                .isTrue();
        assertThat(RateSheetItemMatchSupport.matchesRateSheetItem(item, shipment("DDP", "B2C", "HV")))
                .isTrue();
        assertThat(RateSheetItemMatchSupport.matchesRateSheetItem(item, shipment("DDO", "B2B", "HV")))
                .isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "DDU, B2B, LV, DDU, B2B, LV, true",
            "DDP, B2C, HV, DDP, B2C, HV, true",
            "DDO, B2B, LV, DDO, B2B, LV, true",
            "DDU, B2B, LV, DDP, B2B, LV, false",
            "ALL, B2B, LV, DDU, B2B, LV, true",
            "DDU, ALL, LV, DDU, B2C, LV, true",
            "DDU, B2B, ALL, DDU, B2B, HV, true",
            "DDU, B2B, LV, DDU, B2C, LV, false"
    })
    void matchesRateSheetItem_shouldEvaluateShipmentBusinessCategoryCombinations(
            String itemShipmentType,
            String itemBusinessType,
            String itemCategoryType,
            String shipmentType,
            String businessType,
            String categoryType,
            boolean expectedMatch) {
        RateSheetItem item = rateSheetItem(itemShipmentType, itemBusinessType, itemCategoryType, "DUTY");

        assertThat(RateSheetItemMatchSupport.matchesRateSheetItem(
                        item, shipment(shipmentType, businessType, categoryType)))
                .isEqualTo(expectedMatch);
    }

    @Test
    void matchesRateSheetItem_shouldRejectMismatchedBusinessType() {
        RateSheetItem item = new RateSheetItem();
        item.setShipmentType("DDU");
        item.setBusinessType("B2B");
        item.setCategoryType("LV");
        Shipment shipment = new Shipment();
        shipment.setShipmentType("DDU");
        shipment.setShipmentBusinessType("B2C");
        shipment.setCategoryType("LV");

        assertThat(RateSheetItemMatchSupport.matchesRateSheetItem(item, shipment)).isFalse();
    }

    @Test
    void specificityScore_shouldPreferSpecificConfiguration() {
        RateSheetItem catchAll = rateSheetItem("ALL", "ALL", "ALL", "DUTY");
        RateSheetItem exact = rateSheetItem("DDU", "B2B", "LV", "DUTY");
        RateSheetItem partial = rateSheetItem("DDU", "ALL", "LV", "DUTY");

        assertThat(RateSheetItemMatchSupport.specificityScore(catchAll)).isZero();
        assertThat(RateSheetItemMatchSupport.specificityScore(partial)).isEqualTo(2);
        assertThat(RateSheetItemMatchSupport.specificityScore(exact)).isEqualTo(3);
    }

    @Test
    void selectMostSpecificPerServiceCode_shouldKeepExactMatchOverCatchAll() {
        RateSheetItem catchAll = rateSheetItem("ALL", "ALL", "ALL", "DUTY");
        RateSheetItem exact = rateSheetItem("DDU", "B2B", "LV", "DUTY");
        RateSheetItem partial = rateSheetItem("DDU", "ALL", "LV", "DUTY");

        List<RateSheetItem> selected = RateSheetItemMatchSupport.selectMostSpecificPerServiceCode(
                List.of(catchAll, partial, exact));

        assertThat(selected).containsExactly(exact);
    }

    @Test
    void selectMostSpecificPerServiceCode_shouldKeepSeparateRowsPerServiceCode() {
        RateSheetItem dutyCatchAll = rateSheetItem("ALL", "ALL", "ALL", "DUTY");
        RateSheetItem dutyExact = rateSheetItem("DDP", "B2C", "HV", "DUTY");
        RateSheetItem expressExact = rateSheetItem("DDO", "B2B", "LV", "EXPRESS");

        List<RateSheetItem> selected = RateSheetItemMatchSupport.selectMostSpecificPerServiceCode(
                List.of(dutyCatchAll, dutyExact, expressExact));

        assertThat(selected).containsExactlyInAnyOrder(dutyExact, expressExact);
    }

    @Test
    void isCatchAll_shouldIdentifyFullWildcardRow() {
        assertThat(RateSheetItemMatchSupport.isCatchAll(rateSheetItem("ALL", "ALL", "ALL", "DUTY")))
                .isTrue();
        assertThat(RateSheetItemMatchSupport.isCatchAll(rateSheetItem("DDU", "ALL", "ALL", "DUTY")))
                .isFalse();
    }

    private static RateSheetItem rateSheetItem(
            String shipmentType, String businessType, String categoryType, String serviceCode) {
        RateSheetItem item = new RateSheetItem();
        item.setShipmentType(shipmentType);
        item.setBusinessType(businessType);
        item.setCategoryType(categoryType);
        item.setServiceCode(serviceCode);
        return item;
    }

    private static Shipment shipment(String shipmentType, String businessType, String categoryType) {
        Shipment shipment = new Shipment();
        shipment.setShipmentType(shipmentType);
        shipment.setShipmentBusinessType(businessType);
        shipment.setCategoryType(categoryType);
        return shipment;
    }
}
