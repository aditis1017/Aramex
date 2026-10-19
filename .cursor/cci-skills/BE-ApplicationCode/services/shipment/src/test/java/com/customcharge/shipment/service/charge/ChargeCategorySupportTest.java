package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.customcharge.shipment.constants.ShipmentConstants.CATEGORY_TYPE_EXEMPTED;
import static com.customcharge.shipment.constants.ShipmentConstants.CATEGORY_TYPE_HV;
import static com.customcharge.shipment.constants.ShipmentConstants.CATEGORY_TYPE_LV;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_DDP;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_DDU;
import static org.assertj.core.api.Assertions.assertThat;

class ChargeCategorySupportTest {

    @Test
    void captureRateSheetCategory_shouldPreserveLvAndDefaultBlankToHv() {
        Shipment lv = new Shipment();
        lv.setCategoryType(CATEGORY_TYPE_LV);
        assertThat(ChargeCategorySupport.captureRateSheetCategory(lv)).isEqualTo(CATEGORY_TYPE_LV);

        Shipment blank = new Shipment();
        assertThat(ChargeCategorySupport.captureRateSheetCategory(blank)).isEqualTo(CATEGORY_TYPE_HV);
    }

    @Test
    void isHvOrExempted_shouldRecognizeSupportedCategories() {
        assertThat(ChargeCategorySupport.isHvOrExempted(CATEGORY_TYPE_HV)).isTrue();
        assertThat(ChargeCategorySupport.isHvOrExempted(CATEGORY_TYPE_EXEMPTED)).isTrue();
        assertThat(ChargeCategorySupport.isHvOrExempted(CATEGORY_TYPE_LV)).isFalse();
        assertThat(ChargeCategorySupport.isHvOrExempted(null)).isTrue();
    }

    @Test
    void statusFromEstimateAmount_shouldReturnDueOrPending() {
        assertThat(ChargeCategorySupport.statusFromEstimateAmount(BigDecimal.valueOf(150))).isEqualTo("DUE");
        assertThat(ChargeCategorySupport.statusFromEstimateAmount(BigDecimal.ONE)).isEqualTo("DUE");
        assertThat(ChargeCategorySupport.statusFromEstimateAmount(BigDecimal.ZERO)).isEqualTo("PENDING CALCULATION");
        assertThat(ChargeCategorySupport.statusFromEstimateAmount(null)).isEqualTo("PENDING CALCULATION");
    }

    @Test
    void applyShipmentChargeStatuses_shouldDriveDtcAndDtaFromEcd() {
        Shipment shipment = dduShipment();
        Charge ecd = charge("ECD", BigDecimal.valueOf(100));
        Charge dtc = charge("DTC", BigDecimal.ZERO);
        Charge dta = charge("DTA", BigDecimal.valueOf(50));

        ChargeCategorySupport.applyShipmentChargeStatuses(shipment, List.of(ecd, dtc, dta));

        assertThat(ecd.getStatus()).isEqualTo("DUE");
        assertThat(dtc.getStatus()).isEqualTo("DUE");
        assertThat(dta.getStatus()).isEqualTo("DUE");
    }

    @Test
    void applyShipmentChargeStatuses_shouldSetPendingWhenEcdEstimateIsZero() {
        Shipment shipment = dduShipment();
        Charge ecd = charge("ECD", BigDecimal.ZERO);
        Charge dtc = charge("DTC", BigDecimal.valueOf(50));

        ChargeCategorySupport.applyShipmentChargeStatuses(shipment, List.of(ecd, dtc));

        assertThat(ecd.getStatus()).isEqualTo("PENDING CALCULATION");
        assertThat(dtc.getStatus()).isEqualTo("PENDING CALCULATION");
    }

    @Test
    void applyShipmentChargeStatuses_shouldDriveDtsFromEcs() {
        Shipment shipment = ddpShipment();
        Charge ecs = charge("ECS", BigDecimal.valueOf(80));
        Charge dts = charge("DTS", BigDecimal.ZERO);

        ChargeCategorySupport.applyShipmentChargeStatuses(shipment, List.of(ecs, dts));

        assertThat(ecs.getStatus()).isEqualTo("DUE");
        assertThat(dts.getStatus()).isEqualTo("DUE");
    }

    @Test
    void applyShipmentChargeStatuses_shouldDriveDt3FromEc3() {
        Shipment shipment = ddpShipment();
        Charge ec3 = charge("EC3", null);
        Charge dt3 = charge("DT3", BigDecimal.TEN);

        ChargeCategorySupport.applyShipmentChargeStatuses(shipment, List.of(ec3, dt3));

        assertThat(ec3.getStatus()).isEqualTo("PENDING CALCULATION");
        assertThat(dt3.getStatus()).isEqualTo("PENDING CALCULATION");
    }

    @Test
    void applyShipmentChargeStatuses_shouldMarkAllChargesExempted() {
        Shipment shipment = new Shipment();
        shipment.setShipmentType(SHIPMENT_TYPE_DDU);
        shipment.setCategoryType(CATEGORY_TYPE_EXEMPTED);
        Charge ecd = charge("ECD", BigDecimal.valueOf(100));
        Charge dtc = charge("DTC", BigDecimal.valueOf(100));

        ChargeCategorySupport.applyShipmentChargeStatuses(shipment, List.of(ecd, dtc));

        assertThat(ecd.getStatus()).isEqualTo("Exempted");
        assertThat(dtc.getStatus()).isEqualTo("Exempted");
    }

    @Test
    void applyShipmentChargeStatuses_shouldNoOpForNonDduDdpShipmentTypes() {
        Shipment shipment = new Shipment();
        shipment.setShipmentType("DDO");
        shipment.setCategoryType(CATEGORY_TYPE_LV);
        Charge ecd = charge("ECD", BigDecimal.valueOf(100));
        ecd.setStatus("PENDING CALCULATION");

        ChargeCategorySupport.applyShipmentChargeStatuses(shipment, List.of(ecd));

        assertThat(ecd.getStatus()).isEqualTo("PENDING CALCULATION");
    }

    @Test
    void isLvToHvTransition_shouldOnlyMatchLvToHv() {
        assertThat(ChargeCategorySupport.isLvToHvTransition(CATEGORY_TYPE_LV, CATEGORY_TYPE_HV)).isTrue();
        assertThat(ChargeCategorySupport.isLvToHvTransition(CATEGORY_TYPE_HV, CATEGORY_TYPE_LV)).isFalse();
        assertThat(ChargeCategorySupport.isLvToHvTransition(CATEGORY_TYPE_LV, CATEGORY_TYPE_LV)).isFalse();
    }

    private static Shipment dduShipment() {
        Shipment shipment = new Shipment();
        shipment.setShipmentType(SHIPMENT_TYPE_DDU);
        shipment.setCategoryType(CATEGORY_TYPE_LV);
        return shipment;
    }

    private static Shipment ddpShipment() {
        Shipment shipment = new Shipment();
        shipment.setShipmentType(SHIPMENT_TYPE_DDP);
        shipment.setCategoryType(CATEGORY_TYPE_HV);
        return shipment;
    }

    private static Charge charge(String code, BigDecimal estimateAmount) {
        Charge charge = new Charge();
        charge.setChargeCode(code);
        charge.setEstimateAmount(estimateAmount);
        return charge;
    }
}
