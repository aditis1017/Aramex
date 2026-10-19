package com.customcharge.shipment.service.rules;

import com.customcharge.persistencelayer.cache.MasterCategoryClosureLookupService;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.rules.GlobalSettingRuleLookupService;
import com.customcharge.persistencelayer.rules.RuleEngine;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.customcharge.shipment.constants.MasterDataCategoryCodes.CATEGORY_TYPE;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_DDO;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CategoryTypeRuleResolver {

    MasterCategoryClosureLookupService masterCategoryClosureLookupService;
    GlobalSettingRuleLookupService globalSettingRuleLookupService;
    RuleEngine ruleEngine;
    ObjectProvider<ExchangeRateService> exchangeRateService;

    public Optional<RuleResolverSupport.ResolvedRule> resolveCategoryType(ShipmentDto message) {
        return resolveCategoryType(message, true);
    }

    public Optional<RuleResolverSupport.ResolvedRule> resolveCategoryType(
            ShipmentDto message, boolean applySarConversion) {
        if (message == null) {
            return Optional.empty();
        }
        if (hasText(message.getCategoryType())) {
            return Optional.of(new RuleResolverSupport.ResolvedRule(
                    message.getCategoryType(), null));
        }

        String destination = message.getConsigneeCountryCode();
        if (!hasText(destination)) {
            return Optional.empty();
        }

        List<String> candidateCategoryTypes =
                masterCategoryClosureLookupService.loadCandidateCodes(CATEGORY_TYPE);
        if (candidateCategoryTypes.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Map<String, ?>> rulesByCategoryType = new LinkedHashMap<>();
        for (String categoryTypeCode : candidateCategoryTypes) {
            Map<String, ?> rules =
                    globalSettingRuleLookupService.loadDrlMap(destination, categoryTypeCode);
            if (!rules.isEmpty()) {
                rulesByCategoryType.put(categoryTypeCode, rules);
            }
        }

        List<PriorityOrderedRuleDrl.RuleEntry> orderedRules =
                PriorityOrderedRuleDrl.mergedOrderedRuleEntries(rulesByCategoryType);
        if (orderedRules.isEmpty()) {
            return Optional.empty();
        }

        if (applySarConversion) {
            Double originalCostOfGoods = message.getCostOfGoods();
            RuleResolverSupport.applyCostOfGoodsSarConversion(message, exchangeRateService.getIfAvailable());
            try {
                return evaluateCategoryTypeRules(message, orderedRules);
            } finally {
                message.setCostOfGoods(originalCostOfGoods);
            }
        }
        return evaluateCategoryTypeRules(message, orderedRules);
    }

    private Optional<RuleResolverSupport.ResolvedRule> evaluateCategoryTypeRules(
            ShipmentDto message,
            List<PriorityOrderedRuleDrl.RuleEntry> orderedRules) {
        Optional<RuleResolverSupport.RuleAssignment> assignment = RuleResolverSupport.executeOrderedRules(
                message,
                orderedRules,
                null,
                ruleEngine,
                GlobalSettingRuleCacheSupport::adaptDrlForCategoryOnShipmentMessageDto,
                ShipmentDto::getCategoryType,
                log,
                "merged_category_rules"
        );
        if (assignment.isPresent()) {
            message.setCategoryType(assignment.get().type());
            return Optional.of(new RuleResolverSupport.ResolvedRule(
                    assignment.get().type(), assignment.get()));
        }

        return Optional.empty();
    }

    private static boolean isDdoShipment(ShipmentDto message) {
        return hasText(message.getShipmentType())
                && SHIPMENT_TYPE_DDO.equalsIgnoreCase(message.getShipmentType().trim());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
