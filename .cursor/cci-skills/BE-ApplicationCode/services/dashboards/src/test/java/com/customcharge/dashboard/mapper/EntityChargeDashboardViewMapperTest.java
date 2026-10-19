package com.customcharge.dashboard.mapper;

import com.customcharge.dashboard.constants.ChargeTypeConstants;
import com.customcharge.dashboard.dto.EntityChargeGridRowDTO;
import com.customcharge.persistencelayer.entities.EntityChargeDashboardView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EntityChargeDashboardViewMapperTest {

    private final EntityChargeDashboardViewMapper mapper = new EntityChargeDashboardViewMapper();

    @Test
    void toGridRow_shouldReturnNull_whenRowMissing() {
        assertThat(mapper.toGridRow(null)).isNull();
    }

    @Test
    void toGridRow_shouldMapResolvedApAccountFromView() {
        EntityChargeDashboardView row = baseRow("ECD");
        row.setAccountNumber("AP-100");
        row.setAccountName("AP Account Name");

        EntityChargeGridRowDTO dto = mapper.toGridRow(row);

        assertThat(dto.getAccountNumber()).isEqualTo("AP-100");
        assertThat(dto.getAccountName()).isEqualTo("AP Account Name");
        assertThat(dto.getChargeGroup()).isEqualTo(ChargeTypeConstants.CHARGE_GROUP_COST_AP);
        assertThat(dto.getChargeCode()).isEqualTo("ECD");
    }

    @Test
    void toGridRow_shouldMapResolvedArAccountFromView() {
        EntityChargeDashboardView row = baseRow("DTA");
        row.setAccountNumber("AR-DTA-1");
        row.setAccountName("DTA Account");

        EntityChargeGridRowDTO dto = mapper.toGridRow(row);

        assertThat(dto.getAccountNumber()).isEqualTo("AR-DTA-1");
        assertThat(dto.getAccountName()).isEqualTo("DTA Account");
        assertThat(dto.getChargeGroup()).isEqualTo(ChargeTypeConstants.CHARGE_GROUP_REVENUE_AR);
    }

    @Test
    void toGridRow_shouldMapResolvedShipmentPartyFromView() {
        EntityChargeDashboardView row = baseRow("DTC");
        row.setAccountNumber("CONS-1");
        row.setAccountName("Consignee");

        EntityChargeGridRowDTO dto = mapper.toGridRow(row);

        assertThat(dto.getAccountNumber()).isEqualTo("CONS-1");
        assertThat(dto.getAccountName()).isEqualTo("Consignee");
        assertThat(dto.getThirdParty()).isEqualTo("THIRD/AE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"DTC", "DTA", "DTS", "DT3"})
    void toGridRow_shouldUseOutstandingAmount_forRevenueCharges(String chargeType) {
        EntityChargeGridRowDTO dto = mapper.toGridRow(baseRow(chargeType));

        assertThat(dto.getTotalOutstanding()).isEqualByComparingTo("10");
        assertThat(dto.getTotalBalance()).isEqualByComparingTo("10");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ECD", "ECS", "EC3"})
    void toGridRow_shouldUseActualAmount_forCostCharges(String chargeType) {
        EntityChargeGridRowDTO dto = mapper.toGridRow(baseRow(chargeType));

        assertThat(dto.getTotalOutstanding()).isEqualByComparingTo("1");
        assertThat(dto.getTotalBalance()).isEqualByComparingTo("1");
    }

    private static EntityChargeDashboardView baseRow(String chargeType) {
        EntityChargeDashboardView row = new EntityChargeDashboardView();
        row.setEntity("RUH");
        row.setTrackingnumber("AWB-1");
        row.setOriginentity("DXB");
        row.setDestinationentity("RUH");
        row.setThirdpartentity("THIRD");
        row.setThirdpartcountry("AE");
        row.setChargetype(chargeType);
        row.setCurrency("USD");
        row.setOutstandingAmount(BigDecimal.TEN);
        row.setActualAmount(BigDecimal.ONE);
        row.setChargestatus("DUE");
        return row;
    }
}
