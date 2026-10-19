package com.customcharge.dashboard.search;

import com.customcharge.persistencelayer.search.SearchCriteria;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps dashboard view filter field names to {@link com.customcharge.persistencelayer.entities.Charge}
 * JPA paths for entity-first pagination (avoids scanning all charges by {@code created_time}).
 */
public final class EntityChargeChargeFieldPaths {

    private static final String ENTITY_FIELD = "entity";

    private static final Map<String, String> VIEW_TO_CHARGE_PATH = Map.ofEntries(
            Map.entry("chargetype", "chargeCode"),
            Map.entry("chargestatus", "status"),
            Map.entry("trackingnumber", "shipment.hawbNumber"),
            Map.entry("originentity", "shipment.originEntity"),
            Map.entry("destinationentity", "shipment.HAWBDestinationEntity"),
            Map.entry("thirdpartentity", "shipment.thirdPartyEntity"),
            Map.entry("thirdpartcountry", "shipment.thirdPartyCountry"),
            Map.entry("businesstype", "shipment.shipmentBusinessType"),
            Map.entry("category", "shipment.categoryType"),
            Map.entry("origincountry", "shipment.ShipperCountryCode"),
            Map.entry("destinationcountry", "shipment.consigneeCountryCode"),
            Map.entry("console", "shipment.MAWBConsole"),
            Map.entry("pickupdate", "shipment.pickupDate"),
            Map.entry("alerteddate", "shipment.HAWBAlertedDate"));

    private EntityChargeChargeFieldPaths() {
    }

    public static boolean isEntityField(String fieldName) {
        return fieldName != null
                && ENTITY_FIELD.equals(fieldName.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * True when every non-entity criterion maps to a {@link com.customcharge.persistencelayer.entities.Charge} path.
     */
    public static boolean allNonEntityCriteriaSupportedOnCharge(List<SearchCriteria> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return false;
        }
        boolean hasChargeMappedCriterion = false;
        for (SearchCriteria criterion : criteria) {
            if (criterion == null || criterion.getFieldName() == null) {
                continue;
            }
            if (isEntityField(criterion.getFieldName())) {
                continue;
            }
            if (remapToChargePath(criterion) == null) {
                return false;
            }
            hasChargeMappedCriterion = true;
        }
        return hasChargeMappedCriterion;
    }

    public static SearchCriteria remapToChargePath(SearchCriteria criteria) {
        if (criteria == null || criteria.getFieldName() == null) {
            return null;
        }
        String key = criteria.getFieldName().trim().toLowerCase(Locale.ROOT);
        if (ENTITY_FIELD.equals(key)) {
            return null;
        }
        String chargePath = VIEW_TO_CHARGE_PATH.get(key);
        if (chargePath == null) {
            return null;
        }
        SearchCriteria remapped = new SearchCriteria();
        remapped.setFieldName(chargePath);
        remapped.setCondition(criteria.getCondition());
        remapped.setValue(criteria.getValue());
        remapped.setSecondValue(criteria.getSecondValue());
        return remapped;
    }
}
