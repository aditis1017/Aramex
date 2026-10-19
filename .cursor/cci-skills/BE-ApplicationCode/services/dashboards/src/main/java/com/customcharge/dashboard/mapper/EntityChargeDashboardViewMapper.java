package com.customcharge.dashboard.mapper;

import com.customcharge.dashboard.constants.ChargeTypeConstants;
import com.customcharge.dashboard.dto.EntityChargeGridRowDTO;
import com.customcharge.dashboard.util.EntityChargeStatusMapper;
import com.customcharge.persistencelayer.entities.EntityChargeDashboardView;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class EntityChargeDashboardViewMapper {

    public EntityChargeGridRowDTO toGridRow(EntityChargeDashboardView row) {
        if (row == null) {
            return null;
        }
        BigDecimal totalAmount = resolveTotalAmount(row);
        return EntityChargeGridRowDTO.builder()
                .entity(row.getEntity())
                .trackingNumber(row.getTrackingnumber())
                .accountNumber(trimToNull(row.getAccountNumber()))
                .accountName(trimToNull(row.getAccountName()))
                .origin(formatEntityOnly(row.getOriginentity()))
                .destination(formatEntityOnly(row.getDestinationentity()))
                .thirdParty(formatLocation(row.getThirdpartentity(), row.getThirdpartcountry()))
                .chargeGroup(resolveChargeGroup(row.getChargetype()))
                .chargeCode(row.getChargetype())
                .currency(row.getCurrency())
                .totalOutstanding(totalAmount)
                .totalBalance(totalAmount)
                .status(EntityChargeStatusMapper.toDisplay(row.getChargestatus()))
                .build();
    }

    static BigDecimal resolveTotalAmount(EntityChargeDashboardView row) {
        if (row == null) {
            return null;
        }
        String chargeType = ChargeTypeConstants.normalize(row.getChargetype());
        if (ChargeTypeConstants.COST_AP.contains(chargeType)) {
            return row.getActualAmount();
        }
        if (ChargeTypeConstants.REVENUE_AR.contains(chargeType)) {
            return row.getOutstandingAmount();
        }
        return row.getOutstandingAmount();
    }

    private static String resolveChargeGroup(String chargeCode) {
        if (chargeCode == null || chargeCode.isBlank()) {
            return null;
        }
        String normalized = chargeCode.trim().toUpperCase(Locale.ROOT);
        if (ChargeTypeConstants.COST_AP.contains(normalized)) {
            return ChargeTypeConstants.CHARGE_GROUP_COST_AP;
        }
        if (ChargeTypeConstants.REVENUE_AR.contains(normalized)) {
            return ChargeTypeConstants.CHARGE_GROUP_REVENUE_AR;
        }
        return null;
    }

    private static String formatEntityOnly(String entity) {
        return StringUtils.hasText(entity) ? entity.trim() : null;
    }

    private static String formatLocation(String entity, String country) {
        boolean hasEntity = StringUtils.hasText(entity);
        boolean hasCountry = StringUtils.hasText(country);
        if (hasEntity && hasCountry) {
            return entity.trim() + "/" + country.trim();
        }
        if (hasEntity) {
            return entity.trim();
        }
        return hasCountry ? country.trim() : null;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
