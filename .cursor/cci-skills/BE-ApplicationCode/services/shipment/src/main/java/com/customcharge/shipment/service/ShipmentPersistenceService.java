package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dao.KeywordRuleDao;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.shipment.observability.ShipmentProcessingPhaseTimer;
import com.customcharge.shipment.service.mapper.ShipmentEntityMapper;
import com.customcharge.shipment.service.rules.CategoryTypeRuleResolver;
import com.customcharge.shipment.service.rules.RuleResolverSupport;
import com.customcharge.shipment.service.rules.ShipmentTypeRuleResolver;
import com.customcharge.shipment.util.FrdmFlagSupport;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.customcharge.shipment.constants.LogMessages.*;
import static com.customcharge.shipment.constants.QueueMessages.INCOMPLETE_SHIPMENT_IGNORED;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentPersistenceService {

    ShipmentRepository shipmentRepository;
    ChargeRepository chargeRepository;
    ShipmentEntityMapper shipmentEntityMapper;
    ShipmentTypeRuleResolver shipmentTypeRuleResolver;
    CategoryTypeRuleResolver categoryTypeRuleResolver;
    KeywordRuleDao keywordRuleDao;
    ShipmentPersistenceWriter shipmentPersistenceWriter;
    ObjectProvider<ExchangeRateService> exchangeRateService;
    ShipmentProcessingPhaseTimer phaseTimer;
    ShipmentArAccountService shipmentArAccountService;
    ShipmentBusinessTypeResolver shipmentBusinessTypeResolver;
    ShipmentMawbLinkageService shipmentMawbLinkageService;


    public Optional<PreparedNewShipment> prepareNewShipment(ShipmentDto message) {
        String hawb = normalizeHawb(message.getHawb());
        if (hawb == null) {
            return Optional.empty();
        }
        message.setHawb(hawb);

        Boolean hawbExists = phaseTimer.supply(hawb, "hawbCheck", () -> shipmentRepository.existsByHawbNumber(hawb));
        if (!Boolean.TRUE.equals(message.getIsShipmentFull()) && !hawbExists) {
            log.warn(SHIPMENT_INCOMPLETE_SKIPPED, hawb);
            throw new IncompleteShipmentException(INCOMPLETE_SHIPMENT_IGNORED);
        }
        if (hawbExists) {
            log.warn(SHIPMENT_DUPLICATE_SKIPPED, hawb);
            return Optional.empty();
        }

        message.setFrdmFlag(FrdmFlagSupport.resolve(message.getServices()));

        Optional<ARAccount> arAccount = phaseTimer.supply(
                hawb, "arAccountLookup", () -> shipmentArAccountService.findArAccount(message));
        arAccount.flatMap(shipmentArAccountService::resolveDtcShipmentType)
                .ifPresent(message::setShipmentType);

        Shipment shipment = new Shipment();
        if (StringUtils.hasText(message.getConsigneeName())) {
            shipment.setB2BShipment(keywordRuleDao.evaluateB2BUsingFunction(message.getConsigneeName()));
        }
        String shipmentBusinessType =
                shipmentBusinessTypeResolver.resolve(shipment, arAccount.orElse(null), message);
        shipment.setShipmentBusinessType(shipmentBusinessType);
        shipment.setAccountBusinessType(message.getAccountBusinessType());
        message.setBusinessType(shipmentBusinessType);
        ResolvedRules resolvedRules = phaseTimer.supply(hawb, "ruleResolution", () ->
                RuleResolverSupport.runWithTemporarySarCost(
                        message,
                        exchangeRateService.getIfAvailable(),
                        () -> {
                            Optional<RuleResolverSupport.ResolvedRule> shipmentTypeResult =
                                    shipmentTypeRuleResolver.resolveShipmentType(message, false);
                            shipmentTypeResult.ifPresent(result -> message.setShipmentType(result.value()));
                            Optional<RuleResolverSupport.ResolvedRule> categoryTypeResult =
                                    categoryTypeRuleResolver.resolveCategoryType(message, false);
                            categoryTypeResult.ifPresent(result -> message.setCategoryType(result.value()));
                            return new ResolvedRules(shipmentTypeResult, categoryTypeResult);
                        }));

        phaseTimer.run(hawb, "entityMapping", () -> shipmentEntityMapper.map(message, shipment));

        List<RuleResolverSupport.RuleAssignment> ruleAssignments = buildRuleAssignments(resolvedRules);
        shipmentMawbLinkageService.applyFirstMawbConsoleFromDetails(shipment);
        return Optional.of(new PreparedNewShipment(shipment, hawb, ruleAssignments));
    }

    /**
     * Loads an existing shipment that has no charge rows yet (e.g. after a failed charge transaction).
     */
    public Optional<Shipment> findShipmentNeedingCharges(String hawb) {
        String normalized = normalizeHawb(hawb);
        if (normalized == null) {
            return Optional.empty();
        }
        return shipmentRepository
                .findByHawbNumber(normalized)
                .filter(shipment -> shipment.getId() != null)
                .filter(shipment -> !chargeRepository.existsByShipment_Id(shipment.getId()));
    }

    private static List<RuleResolverSupport.RuleAssignment> buildRuleAssignments(ResolvedRules resolvedRules) {
        List<RuleResolverSupport.RuleAssignment> assignments = new ArrayList<>(2);
        resolvedRules.shipmentTypeResult()
                .map(RuleResolverSupport.ResolvedRule::assignment)
                .ifPresent(assignments::add);
        resolvedRules.categoryTypeResult()
                .map(RuleResolverSupport.ResolvedRule::assignment)
                .ifPresent(assignments::add);
        return assignments;
    }

    private static String normalizeHawb(String hawb) {
        if (hawb == null || hawb.isBlank()) {
            return null;
        }
        return hawb.trim();
    }

    private record ResolvedRules(
            Optional<RuleResolverSupport.ResolvedRule> shipmentTypeResult,
            Optional<RuleResolverSupport.ResolvedRule> categoryTypeResult) {
    }
}
