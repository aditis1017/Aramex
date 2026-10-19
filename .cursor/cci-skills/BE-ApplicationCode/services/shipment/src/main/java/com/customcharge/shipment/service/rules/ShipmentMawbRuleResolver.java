package com.customcharge.shipment.service.rules;

import com.customcharge.persistencelayer.cache.CacheService;
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

import static com.customcharge.shipment.constants.MasterDataCategoryCodes.MAWB_FILTER;
import static com.customcharge.shipment.constants.MasterDataCategoryCodes.SHIPMENT_FILTER;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentMawbRuleResolver {

    CacheService cacheService;
    RuleEngine ruleEngine;
    GlobalSettingRuleLookupService globalSettingRuleLookupService;


    public void applyMawbFilterRules(ShipmentDto message) {
        if (message == null || Boolean.TRUE.equals(message.getIsMawbShipment())) {
            return;
        }

        String destination = message.getConsigneeCountryCode();
        if (!hasText(destination)) {
            return;
        }

        Map<String, ?> rules = globalSettingRuleLookupService.loadDrlMap(destination, MAWB_FILTER);
        List<String> orderedDrls = PriorityOrderedRuleDrl.orderedDrlStrings(rules);
        if (orderedDrls.isEmpty()) {
            return;
        }

        RuleResolverSupport.executeOrderedRulesUntilTrue(
                message,
                orderedDrls,
                ruleEngine,
                GlobalSettingRuleCacheSupport::adaptDrlFactToShipmentDto,
                ShipmentDto::getIsMawbShipment,
                log,
                "targetCode=" + MAWB_FILTER
        );
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
