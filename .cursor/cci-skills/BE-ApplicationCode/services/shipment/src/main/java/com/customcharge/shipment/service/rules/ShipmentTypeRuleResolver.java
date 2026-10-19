package com.customcharge.shipment.service.rules;

import com.customcharge.persistencelayer.cache.MasterCategoryClosureLookupService;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.rules.GlobalSettingRuleLookupService;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
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

import static com.customcharge.shipment.constants.MasterDataCategoryCodes.CUSTOMS_TYPE;
import static com.customcharge.shipment.constants.MasterDataCategoryCodes.SHIPMENT_TYPE;
import static com.customcharge.shipment.constants.ShipmentConstants.ACCOUNT_SHIPPER_RUH_OVERRIDE;
import static com.customcharge.shipment.constants.ShipmentConstants.CUSTOM_TYPE_DDO;
import static com.customcharge.shipment.constants.ShipmentConstants.ENTITY_RUH;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_DDO;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_DDU;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_FALLBACK_RULE_EVALUATION_CODES;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_PRIMARY_RULE_EVALUATION_CODES;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentTypeRuleResolver {

    MasterCategoryClosureLookupService masterCategoryClosureLookupService;
    GlobalSettingRuleLookupService globalSettingRuleLookupService;
    RuleEngine ruleEngine;
    ARAccountRepository arAccountRepository;
    ObjectProvider<ExchangeRateService> exchangeRateService;

    public Optional<RuleResolverSupport.ResolvedRule> resolveShipmentType(ShipmentDto message) {
        return resolveShipmentType(message, true);
    }

    public Optional<RuleResolverSupport.ResolvedRule> resolveShipmentType(
            ShipmentDto message, boolean applySarConversion) {
        if (message == null) {
            return Optional.empty();
        }

        boolean isDrooleTriggered = false;

        String existingShipmentType = hasText(message.getShipmentType())
                ? message.getShipmentType().trim()
                : null;

        String destination = message.getConsigneeCountryCode();
        if (!hasText(destination)) {
            return toExistingShipmentTypeResult(message, existingShipmentType);
        }

        List<String> candidateShipmentTypes =
                masterCategoryClosureLookupService.loadCandidateCodes(CUSTOMS_TYPE, SHIPMENT_TYPE);
        if (candidateShipmentTypes.isEmpty()) {
            return toExistingShipmentTypeResult(message, existingShipmentType);
        }

        if (ENTITY_RUH.equals(destination) && ACCOUNT_SHIPPER_RUH_OVERRIDE.equals(message.getShipperAccountNumber())) {
            message.setShipmentType(SHIPMENT_TYPE_DDU);
            return Optional.of(new RuleResolverSupport.ResolvedRule(SHIPMENT_TYPE_DDU, null));
        }

        Double originalCostOfGoods = message.getCostOfGoods();
        if (applySarConversion) {
            RuleResolverSupport.applyCostOfGoodsSarConversion(message, exchangeRateService.getIfAvailable());
        }
        try {
            Optional<RuleResolverSupport.RuleAssignment> assignment = evaluateMergedShipmentTypeRules(
                    message,
                    destination,
                    candidateShipmentTypes,
                    SHIPMENT_TYPE_PRIMARY_RULE_EVALUATION_CODES
            );
            if (assignment.isEmpty()) {
                assignment = evaluateMergedShipmentTypeRules(
                        message,
                        destination,
                        candidateShipmentTypes,
                        SHIPMENT_TYPE_FALLBACK_RULE_EVALUATION_CODES
                );
            }
            if (assignment.isPresent()) {
                isDrooleTriggered = true;
            }
            if (isDrooleTriggered) {
                message.setShipmentType(assignment.get().type());
                return Optional.of(new RuleResolverSupport.ResolvedRule(
                        assignment.get().type(), assignment.get()));
            }

            return toExistingShipmentTypeResult(message, existingShipmentType);
        } finally {
            if (applySarConversion) {
                message.setCostOfGoods(originalCostOfGoods);
            }
        }
    }

    private static Optional<RuleResolverSupport.ResolvedRule> toExistingShipmentTypeResult(
            ShipmentDto message,
            String existingShipmentType) {
        if (existingShipmentType == null) {
            message.setShipmentType(null);
            return Optional.empty();
        }
        message.setShipmentType(existingShipmentType);
        return Optional.of(new RuleResolverSupport.ResolvedRule(existingShipmentType, null));
    }

    private Optional<RuleResolverSupport.RuleAssignment> evaluateMergedShipmentTypeRules(
            ShipmentDto message,
            String destination,
            List<String> candidateShipmentTypes,
            List<String> phaseCodes) {

        Map<String, Map<String, ?>> rulesByShipmentType = new LinkedHashMap<>();
        for (String phaseCode : phaseCodes) {
            if (candidateShipmentTypes.stream().noneMatch(code -> code.equalsIgnoreCase(phaseCode))) {
                continue;
            }
            Map<String, ?> rules = globalSettingRuleLookupService.loadDrlMap(destination, phaseCode);
            if (!rules.isEmpty()) {
                rulesByShipmentType.put(phaseCode, rules);
            }
        }
        if (rulesByShipmentType.isEmpty()) {
            return Optional.empty();
        }

        List<PriorityOrderedRuleDrl.RuleEntry> orderedRules =
                PriorityOrderedRuleDrl.mergedOrderedRuleEntries(rulesByShipmentType);
        if (orderedRules.isEmpty()) {
            return Optional.empty();
        }

        return RuleResolverSupport.executeOrderedRules(
                message,
                orderedRules,
                null,
                ruleEngine,
                GlobalSettingRuleCacheSupport::adaptDrlFactToShipmentDto,
                ShipmentDto::getShipmentType,
                log,
                "merged_shipment_type_rules=" + phaseCodes,
                true
        );
    }

    private Optional<String> resolveShipmentTypeFromArDdoAccount(ShipmentDto message) {
        if (!hasText(message.getConsigneeAccountNumber()) || !hasText(message.getDestinationEntity())) {
            return Optional.empty();
        }
        boolean isDdoArAccount = arAccountRepository.findByAccountNumberAndEntityCode(
                        message.getConsigneeAccountNumber().trim(),
                        message.getDestinationEntity().trim())
                .map(arAccount -> CUSTOM_TYPE_DDO.equalsIgnoreCase(arAccount.getCustomType()))
                .orElse(false);
        if (!isDdoArAccount) {
            return Optional.empty();
        }
        message.setShipmentType(SHIPMENT_TYPE_DDO);
        return Optional.of(SHIPMENT_TYPE_DDO);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
