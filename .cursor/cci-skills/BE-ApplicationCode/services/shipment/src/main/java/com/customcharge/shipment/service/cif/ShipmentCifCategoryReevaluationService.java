package com.customcharge.shipment.service.cif;

import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.ShipmentRuleAssignmentRepository;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.shipment.config.ShipmentProperties;
import com.customcharge.shipment.constants.ShipmentConstants;
import com.customcharge.shipment.service.charge.ChargeCategorySupport;
import com.customcharge.shipment.service.mapper.ShipmentToDtoMapper;
import com.customcharge.shipment.service.rules.CategoryTypeRuleResolver;
import com.customcharge.shipment.service.rules.RuleResolverSupport;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentCifCategoryReevaluationService {

    ShipmentToDtoMapper shipmentToDtoMapper;
    CategoryTypeRuleResolver categoryTypeRuleResolver;
    ChargeRepository chargeRepository;
    ShipmentRuleAssignmentRepository shipmentRuleAssignmentRepository;
    ObjectProvider<ExchangeRateService> exchangeRateService;
    ShipmentProperties shipmentProperties;

    public record CategoryReevaluationResult(
            Sh267ChargeRecalculationMode mode,
            String previousCategoryType,
            String resolvedCategoryType) {
    }

    public CategoryReevaluationResult evaluate(Shipment shipment) {
        if (shipment == null) {
            return new CategoryReevaluationResult(
                    Sh267ChargeRecalculationMode.CATEGORY_UNCHANGED_NORMAL, null, null);
        }

        String previousCategory = normalizeCategory(shipment.getCategoryType());
        if (ChargeCategorySupport.isExempted(previousCategory)) {
            return new CategoryReevaluationResult(
                    Sh267ChargeRecalculationMode.SKIP_ALREADY_EXEMPTED, previousCategory, previousCategory);
        }

        ShipmentDto ruleMessage = shipmentToDtoMapper.mapForCategoryRules(shipment);
        ruleMessage.setCostOfGoods(resolveCostOfGoodsForRules(shipment));

        Optional<RuleResolverSupport.ResolvedRule> categoryResult =
                categoryTypeRuleResolver.resolveCategoryType(ruleMessage);
        String resolvedCategory = categoryResult
                .map(RuleResolverSupport.ResolvedRule::value)
                .map(ShipmentCifCategoryReevaluationService::normalizeCategory)
                .orElse(previousCategory);

        if (ChargeCategorySupport.isExempted(resolvedCategory)) {
            shipment.setCategoryType(ShipmentConstants.CATEGORY_TYPE_EXEMPTED);
            persistRuleAssignment(shipment.getHawbNumber(), categoryResult);
            return new CategoryReevaluationResult(
                    Sh267ChargeRecalculationMode.NEWLY_EXEMPTED, previousCategory, resolvedCategory);
        }

        boolean categoryChanged = !resolvedCategory.equalsIgnoreCase(previousCategory);
        if (categoryChanged) {
            shipment.setCategoryType(resolvedCategory);
            persistRuleAssignment(shipment.getHawbNumber(), categoryResult);
        }

        if (categoryChanged && ChargeCategorySupport.isLvToHvTransition(previousCategory, resolvedCategory)) {
            if (isShipmentActualized(shipment)) {
                return new CategoryReevaluationResult(
                        Sh267ChargeRecalculationMode.ACTUALIZED_LV_TO_HV, previousCategory, resolvedCategory);
            }
            return new CategoryReevaluationResult(
                    Sh267ChargeRecalculationMode.LV_TO_HV_NORMAL, previousCategory, resolvedCategory);
        }

        if (isShipmentActualized(shipment)) {
            return new CategoryReevaluationResult(
                    Sh267ChargeRecalculationMode.ACTUALIZED_CIF_ONLY, previousCategory, resolvedCategory);
        }

        return new CategoryReevaluationResult(
                Sh267ChargeRecalculationMode.CATEGORY_UNCHANGED_NORMAL, previousCategory, resolvedCategory);
    }

    private double resolveCostOfGoodsForRules(Shipment shipment) {
        double costOfGoods = shipment.getCostOfGoods() != null ? shipment.getCostOfGoods() : 0.0;
        double cifInCustoms = cifAmountInCustomsCurrency(shipment);
        return round(costOfGoods + cifInCustoms);
    }

    private double cifAmountInCustomsCurrency(Shipment shipment) {
        Double cifValue = shipment.getCifValue();
        if (cifValue == null || cifValue <= 0) {
            return 0.0;
        }

        String cifCurrency = normalizeCurrency(shipment.getCifValueCurrency());
        String customsCurrency = normalizeCurrency(shipment.getCustomsCurrency());
        if (!StringUtils.hasText(customsCurrency)
                || !StringUtils.hasText(cifCurrency)
                || cifCurrency.equalsIgnoreCase(customsCurrency)) {
            return cifValue;
        }

        ExchangeRateService exchangeRate = exchangeRateService.getIfAvailable();
        if (exchangeRate == null) {
            return cifValue;
        }
        CurrencyConversionResult result = exchangeRate.convertAmount(cifValue, cifCurrency, customsCurrency);
        if (result == null || result.convertedAmount() == null) {
            return cifValue;
        }
        return round(result.convertedAmount());
    }

    private boolean isShipmentActualized(Shipment shipment) {
        if (shipment.getId() == null) {
            return false;
        }
        List<Charge> charges = chargeRepository.findByShipment_IdWithEntity(shipment.getId());
        return charges.stream().anyMatch(charge -> ChargeActualizedStatusSupport.isActualized(charge.getStatus()));
    }

    private void persistRuleAssignment(
            String hawb, Optional<RuleResolverSupport.ResolvedRule> resolvedRule) {
        resolvedRule
                .map(RuleResolverSupport.ResolvedRule::assignment)
                .ifPresent(assignment -> RuleResolverSupport.saveAssignment(
                        shipmentRuleAssignmentRepository, hawb, assignment));
    }

    private static String normalizeCategory(String categoryType) {
        return categoryType == null ? "" : categoryType.strip();
    }

    private static String normalizeCurrency(String currency) {
        return currency == null ? "" : currency.strip();
    }

    private double round(double amount) {
        return BigDecimal.valueOf(amount)
                .setScale(shipmentProperties.getMonetaryScale(), RoundingMode.HALF_UP)
                .doubleValue();
    }
}
