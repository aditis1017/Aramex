package com.customcharge.shipment.service.rules;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.rules.GlobalSettingRuleLookupService;
import com.customcharge.persistencelayer.rules.RuleEngine;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.customcharge.shipment.constants.MasterDataCategoryCodes.SHIPMENT_FILTER;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentFullnessRuleResolver {

    GlobalSettingRuleLookupService globalSettingRuleLookupService;
    RuleEngine ruleEngine;

    public void applyShipmentFilterRules(ShipmentDto message) {
        if (message == null || Boolean.TRUE.equals(message.getIsShipmentFull())) {
            return;
        }

        String destination = message.getConsigneeCountryCode();
        if (!hasText(destination)) {
            return;
        }

        Map<String, ?> rules = globalSettingRuleLookupService.loadDrlMap(destination, SHIPMENT_FILTER);
        List<String> orderedDrls = PriorityOrderedRuleDrl.orderedDrlStrings(rules);
        if (orderedDrls.isEmpty()) {
            return;
        }

        RuleResolverSupport.executeOrderedRulesUntilTrue(
                message,
                orderedDrls,
                ruleEngine,
                GlobalSettingRuleCacheSupport::adaptDrlForShipmentFullness,
                ShipmentDto::getIsShipmentFull,
                log,
                "targetCode=" + SHIPMENT_FILTER
        );
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
