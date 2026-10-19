package com.customcharge.dashboard.mapper;

import com.customcharge.dashboard.constants.ChargeTypeConstants;
import com.customcharge.dashboard.util.EntityChargeStatusMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Shared formatting helpers for entity-charge displays (grid view mapper + shipment charge details).
 */
@Component
public class EntityChargeGridRowMapper {

    public static String resolveChargeGroup(String chargeCode) {
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

    public static String formatEntityOnly(String entity) {
        return StringUtils.hasText(entity) ? entity.trim() : null;
    }

    public static String formatLocation(String entity, String country) {
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

    public static String formatStatus(String status) {
        return EntityChargeStatusMapper.toDisplay(status);
    }
}
