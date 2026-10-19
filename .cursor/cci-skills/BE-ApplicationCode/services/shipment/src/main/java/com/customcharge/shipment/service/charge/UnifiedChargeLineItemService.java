package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.calculatorhelper.ChargeHeaderAmountSupport;
import com.customcharge.persistencelayer.dto.ServiceChargeQueryCriteria;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.customcharge.persistencelayer.util.ServiceChargeDefaultAccountNumberSupport;
import com.customcharge.persistencelayer.util.ServiceChargeResolver;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContextFactory;
import com.customcharge.shipment.service.charge.processing.calculation.RateAmountCalculator;
import com.customcharge.shipment.service.charge.processing.model.ResolvedRateSheet;
import com.customcharge.shipment.service.charge.processing.ratesheet.RateSheetLookupService;
import com.customcharge.shipment.service.cif.RateSheetItemCifSupport;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.customcharge.shipment.constants.LogMessages.CHARGE_LINE_ITEMS_PREPARED;
import static com.customcharge.shipment.constants.LogMessages.CHARGE_LINE_ITEMS_RECALCULATED_CIF;
import static com.customcharge.shipment.constants.LogMessages.CHARGE_LINE_ITEMS_RECALCULATED_COPIED;
import static com.customcharge.shipment.constants.LogMessages.CHARGE_LINE_ITEMS_SKIPPED_NO_ACCOUNT;
import static com.customcharge.shipment.constants.LogMessages.CHARGE_LINE_ITEMS_SKIPPED_NO_ENTITY;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UnifiedChargeLineItemService {

    ServiceChargeAccountResolver serviceChargeAccountResolver;
    ServiceChargeResolver serviceChargeResolver;
    ServiceCodesRepository serviceCodesRepository;
    RateSheetLookupService rateSheetLookupService;
    RateAmountCalculator rateAmountCalculator;
    ChargeLineItemTaxSupport chargeLineItemTaxSupport;
    ShipmentChargeAmountSupport chargeAmountSupport;
    ShipmentBillingEntitySupport shipmentBillingEntitySupport;
    ShipmentChargeLineCopySupport chargeLineCopySupport;
    ChargeLineItemRepository chargeLineItemRepository;
    ChargeRepository chargeRepository;
    ShipmentProcessingContextFactory shipmentProcessingContextFactory;

    public void createLineItems(Shipment shipment, List<Charge> charges, String rateSheetCategoryType) {
        createLineItems(shipment, charges, rateSheetCategoryType, true);
    }

    public void createLineItems(
            Shipment shipment,
            List<Charge> charges,
            String rateSheetCategoryType,
            boolean persistCharges) {
        if (shipment == null || charges == null || charges.isEmpty()) {
            return;
        }
        boolean exempted = ChargeCategorySupport.isExempted(shipment.getCategoryType());
        List<ChargeLineItem> allLineItems = new ArrayList<>();
        List<Charge> chargesToUpdate = new ArrayList<>();
        ShipmentProcessingContext processingContext = ShipmentProcessingContext.get();

        for (Charge charge : ShipmentChargeProcessingOrder.sort(charges)) {
            if (!ShipmentChargeProcessingOrder.shouldCreateLineItems(shipment, charge.getChargeCode())) {
                finalizeCharge(shipment, charge, List.of(), chargesToUpdate);
                continue;
            }
            List<ChargeLineItem> lineItems = createForCharge(
                    shipment, charge, rateSheetCategoryType, exempted);
            if (!lineItems.isEmpty()) {
                allLineItems.addAll(lineItems);
                log.debug(CHARGE_LINE_ITEMS_PREPARED,
                        lineItems.size(), charge.getId(), charge.getChargeCode());
                if (processingContext != null) {
                    processingContext.registerLineItems(charge.getChargeCode(), lineItems);
                }
            }
            finalizeCharge(shipment, charge, lineItems, chargesToUpdate);
        }

        if (!allLineItems.isEmpty()) {
            chargeLineItemRepository.saveAll(allLineItems);
        }
        ChargeCategorySupport.applyShipmentChargeStatuses(shipment, charges);
        if (persistCharges && !charges.isEmpty()) {
            chargeRepository.saveAll(charges);
        }
    }

    /**
     * SH267 on a non-actualized shipment with unchanged category: recalculate CIF-percentage resolver
     * lines and refresh copy-derived charges when the source chain or the charge's own rate sheet
     * has CIF-based rates.
     */
    public void recalculateLineItems(Shipment shipment, List<Charge> charges, String rateSheetCategoryType) {
        runInProcessingContext(shipment, charges, () ->
                recalculateCifLineItemsInContext(shipment, charges, rateSheetCategoryType));
    }

    /**
     * SH267: zero all existing line items when Drools newly resolves an exempted category.
     */
    public void recalculateExemptedLineItems(Shipment shipment, List<Charge> charges) {
        runInProcessingContext(shipment, charges, () ->
                recalculateExemptedLineItemsInContext(shipment, charges));
    }

    /**
     * SH267 LV to HV on a non-actualized shipment: zero ECD lines, reapply the full rate sheet,
     * then refresh the copy chain.
     */
    public void reapplyFullRateSheetAfterCategoryChange(
            Shipment shipment, List<Charge> charges, String rateSheetCategoryType) {
        runInProcessingContext(shipment, charges, () ->
                reapplyFullRateSheetInContext(shipment, charges, rateSheetCategoryType));
    }

    /**
     * SH267 on an actualized shipment with unchanged category: CIF-percentage ECD estimate lines only.
     */
    public void recalculateActualizedCifOnlyLineItems(
            Shipment shipment, List<Charge> charges, String rateSheetCategoryType) {
        runInProcessingContext(shipment, charges, () ->
                recalculateActualizedCifOnlyInContext(shipment, charges, rateSheetCategoryType));
    }

    /**
     * SH267 on an actualized shipment after LV to HV: refresh ECD estimate lines, apply contracted rates
     * directly on DTA/DTC, then continue the ECS/EC3 and DTS/DT3 copy chain.
     */
    public void recalculateActualizedLvToHvLineItems(
            Shipment shipment, List<Charge> charges, String rateSheetCategoryType) {
        runInProcessingContext(shipment, charges, () ->
                recalculateActualizedLvToHvInContext(shipment, charges, rateSheetCategoryType));
    }

    private void runInProcessingContext(Shipment shipment, List<Charge> charges, Runnable action) {
        if (shipment == null || charges == null || charges.isEmpty()) {
            return;
        }
        List<String> chargeCodes = charges.stream()
                .map(Charge::getChargeCode)
                .filter(StringUtils::hasText)
                .toList();
        shipmentProcessingContextFactory.begin(shipment, chargeCodes);
        try {
            ShipmentProcessingContext processingContext = ShipmentProcessingContext.get();
            if (processingContext != null) {
                charges.forEach(charge -> processingContext.registerCharge(charge.getChargeCode(), charge));
            }
            action.run();
        } finally {
            shipmentProcessingContextFactory.end();
        }
    }

    private void recalculateCifLineItemsInContext(
            Shipment shipment, List<Charge> charges, String rateSheetCategoryType) {
        boolean exempted = ChargeCategorySupport.isExempted(shipment.getCategoryType());
        List<ChargeLineItem> lineItemsToSave = new ArrayList<>();
        List<Charge> chargesToUpdate = new ArrayList<>();
        ShipmentProcessingContext processingContext = ShipmentProcessingContext.get();
        Set<String> updatedChargeCodes = new HashSet<>();

        for (Charge charge : ShipmentChargeProcessingOrder.sort(charges)) {
            if (!ShipmentChargeProcessingOrder.shouldCreateLineItems(shipment, charge.getChargeCode())) {
                continue;
            }

            String chargeCode = ShipmentChargeTypeSupport.normalize(charge.getChargeCode());
            if (chargeLineCopySupport.isCopyDerived(chargeCode)) {
                if (!shouldRecalculateCopyDerivedLine(
                        shipment, chargeCode, rateSheetCategoryType, updatedChargeCodes)) {
                    continue;
                }
                List<ChargeLineItem> updated = recalculateCopyDerivedLines(
                        shipment, charge, chargeCode, rateSheetCategoryType, exempted);
                if (!updated.isEmpty()) {
                    lineItemsToSave.addAll(updated);
                    updatedChargeCodes.add(chargeCode);
                    if (processingContext != null) {
                        processingContext.registerLineItems(chargeCode, updated);
                    }
                    finalizeCharge(shipment, charge, updated, chargesToUpdate);
                    log.debug(
                            CHARGE_LINE_ITEMS_RECALCULATED_COPIED,
                            updated.size(),
                            charge.getId(),
                            chargeCode,
                            shipment.getHawbNumber());
                }
                continue;
            }

            List<ChargeLineItem> updated = recalculateResolverCifLines(
                    shipment, charge, chargeCode, rateSheetCategoryType, exempted);
            if (!updated.isEmpty()) {
                lineItemsToSave.addAll(updated);
                updatedChargeCodes.add(chargeCode);
                if (processingContext != null) {
                    processingContext.registerLineItems(chargeCode, updated);
                }
                finalizeCharge(shipment, charge, updated, chargesToUpdate);
                log.debug(
                        CHARGE_LINE_ITEMS_RECALCULATED_CIF,
                        updated.size(),
                        charge.getId(),
                        chargeCode,
                        shipment.getHawbNumber());
            }
        }

        persistRecalculatedLineItems(shipment, charges, lineItemsToSave, chargesToUpdate);
    }

    private boolean shouldRecalculateCopyDerivedLine(
            Shipment shipment,
            String chargeCode,
            String rateSheetCategoryType,
            Set<String> updatedChargeCodes) {
        if (ShipmentChargeTypeSupport.ECS.equals(chargeCode) || ShipmentChargeTypeSupport.EC3.equals(chargeCode)) {
            return updatedChargeCodes.contains(ShipmentChargeTypeSupport.DTA)
                    || updatedChargeCodes.contains(ShipmentChargeTypeSupport.ECD);
        }
        if (ShipmentChargeTypeSupport.isRevenueCharge(chargeCode)) {
            boolean sourceUpdated = recalculateCopySourceChargeCode(chargeCode)
                    .map(updatedChargeCodes::contains)
                    .orElse(false);
            return sourceUpdated || revenueChargeRateSheetHasCifRate(shipment, chargeCode, rateSheetCategoryType);
        }
        return false;
    }

    private static Optional<String> recalculateCopySourceChargeCode(String chargeCode) {
        return switch (ShipmentChargeTypeSupport.normalize(chargeCode)) {
            case ShipmentChargeTypeSupport.DTC, ShipmentChargeTypeSupport.DTA ->
                    Optional.of(ShipmentChargeTypeSupport.ECD);
            case ShipmentChargeTypeSupport.DTS -> Optional.of(ShipmentChargeTypeSupport.ECS);
            case ShipmentChargeTypeSupport.DT3 -> Optional.of(ShipmentChargeTypeSupport.EC3);
            default -> Optional.empty();
        };
    }

    private boolean revenueChargeRateSheetHasCifRate(
            Shipment shipment, String chargeCode, String rateSheetCategoryType) {
        return rateSheetLookupService.resolveForShipmentCharge(shipment, chargeCode, rateSheetCategoryType)
                .map(RateSheetItemCifSupport::resolvedSheetHasCifRate)
                .orElse(false);
    }

    private void recalculateExemptedLineItemsInContext(Shipment shipment, List<Charge> charges) {
        List<ChargeLineItem> lineItemsToSave = new ArrayList<>();
        List<Charge> chargesToUpdate = new ArrayList<>();

        for (Charge charge : ShipmentChargeProcessingOrder.sort(charges)) {
            if (!ShipmentChargeProcessingOrder.shouldCreateLineItems(shipment, charge.getChargeCode())
                    || charge.getId() == null) {
                continue;
            }
            List<ChargeLineItem> lineItems = chargeLineItemRepository.findByCharge_Id(charge.getId());
            if (lineItems.isEmpty()) {
                continue;
            }
            lineItems.forEach(this::zeroLine);
            lineItemsToSave.addAll(lineItems);
            finalizeCharge(shipment, charge, lineItems, chargesToUpdate);
        }

        persistRecalculatedLineItems(shipment, charges, lineItemsToSave, chargesToUpdate);
    }

    private void recalculateActualizedCifOnlyInContext(
            Shipment shipment, List<Charge> charges, String rateSheetCategoryType) {
        boolean exempted = ChargeCategorySupport.isExempted(shipment.getCategoryType());
        List<ChargeLineItem> lineItemsToSave = new ArrayList<>();
        List<Charge> chargesToUpdate = new ArrayList<>();
        ShipmentProcessingContext processingContext = ShipmentProcessingContext.get();

        for (Charge charge : ShipmentChargeProcessingOrder.sort(charges)) {
            if (!ShipmentChargeProcessingOrder.shouldCreateLineItems(shipment, charge.getChargeCode())) {
                continue;
            }
            String chargeCode = ShipmentChargeTypeSupport.normalize(charge.getChargeCode());
            if (!ShipmentChargeTypeSupport.ECD.equals(chargeCode)) {
                continue;
            }

            List<ChargeLineItem> updated = recalculateResolverCifLines(
                    shipment,
                    charge,
                    chargeCode,
                    rateSheetCategoryType,
                    exempted,
                    UnifiedChargeLineItemService::isEstimateLineItem);
            if (!updated.isEmpty()) {
                lineItemsToSave.addAll(updated);
                if (processingContext != null) {
                    processingContext.registerLineItems(chargeCode, updated);
                }
                finalizeCharge(shipment, charge, updated, chargesToUpdate);
            }
        }

        persistRecalculatedLineItems(shipment, charges, lineItemsToSave, chargesToUpdate);
    }

    private void recalculateActualizedLvToHvInContext(
            Shipment shipment, List<Charge> charges, String rateSheetCategoryType) {
        boolean exempted = ChargeCategorySupport.isExempted(shipment.getCategoryType());
        List<ChargeLineItem> lineItemsToSave = new ArrayList<>();
        List<Charge> chargesToUpdate = new ArrayList<>();
        ShipmentProcessingContext processingContext = ShipmentProcessingContext.get();
        boolean ecdUpdated = false;

        for (Charge charge : ShipmentChargeProcessingOrder.sort(charges)) {
            if (!ShipmentChargeProcessingOrder.shouldCreateLineItems(shipment, charge.getChargeCode())) {
                continue;
            }

            String chargeCode = ShipmentChargeTypeSupport.normalize(charge.getChargeCode());
            List<ChargeLineItem> updated = switch (chargeCode) {
                case ShipmentChargeTypeSupport.ECD -> applyFullRateSheetToResolverLines(
                        shipment,
                        charge,
                        chargeCode,
                        rateSheetCategoryType,
                        exempted,
                        UnifiedChargeLineItemService::isEstimateLineItem,
                        true);
                case ShipmentChargeTypeSupport.DTA, ShipmentChargeTypeSupport.DTC ->
                        ecdUpdated
                                ? applyContractedRateDirectFromRateSheet(
                                        shipment, charge, chargeCode, rateSheetCategoryType, exempted, true)
                                : List.of();
                case ShipmentChargeTypeSupport.ECS, ShipmentChargeTypeSupport.EC3 ->
                        ecdUpdated
                                ? copyAmountsOnlyFromSource(shipment, charge, chargeCode, exempted)
                                : List.of();
                case ShipmentChargeTypeSupport.DTS, ShipmentChargeTypeSupport.DT3 ->
                        ecdUpdated
                                ? reapplyRevenueCopyWithContractedRate(
                                        shipment, charge, chargeCode, rateSheetCategoryType, exempted, true)
                                : List.of();
                default -> List.of();
            };

            if (updated.isEmpty()) {
                continue;
            }
            if (ShipmentChargeTypeSupport.ECD.equals(chargeCode)) {
                ecdUpdated = true;
            }
            lineItemsToSave.addAll(updated);
            if (processingContext != null) {
                processingContext.registerLineItems(chargeCode, updated);
            }
            finalizeCharge(shipment, charge, updated, chargesToUpdate);
        }

        persistRecalculatedLineItems(shipment, charges, lineItemsToSave, chargesToUpdate);
    }

    private void reapplyFullRateSheetInContext(
            Shipment shipment,
            List<Charge> charges,
            String rateSheetCategoryType) {
        boolean exempted = ChargeCategorySupport.isExempted(shipment.getCategoryType());
        List<ChargeLineItem> lineItemsToSave = new ArrayList<>();
        List<Charge> chargesToUpdate = new ArrayList<>();
        ShipmentProcessingContext processingContext = ShipmentProcessingContext.get();
        boolean resolverLinesUpdated = false;

        for (Charge charge : ShipmentChargeProcessingOrder.sort(charges)) {
            if (!ShipmentChargeProcessingOrder.shouldCreateLineItems(shipment, charge.getChargeCode())) {
                continue;
            }

            String chargeCode = ShipmentChargeTypeSupport.normalize(charge.getChargeCode());
            if (chargeLineCopySupport.isCopyDerived(chargeCode)) {
                if (!resolverLinesUpdated) {
                    continue;
                }
                List<ChargeLineItem> updated = reapplyRevenueCopyWithContractedRate(
                        shipment, charge, chargeCode, rateSheetCategoryType, exempted, true);
                if (!updated.isEmpty()) {
                    lineItemsToSave.addAll(updated);
                    if (processingContext != null) {
                        processingContext.registerLineItems(chargeCode, updated);
                    }
                    finalizeCharge(shipment, charge, updated, chargesToUpdate);
                }
                continue;
            }

            List<ChargeLineItem> updated = applyFullRateSheetToResolverLines(
                    shipment,
                    charge,
                    chargeCode,
                    rateSheetCategoryType,
                    exempted,
                    line -> true,
                    true);
            if (!updated.isEmpty()) {
                lineItemsToSave.addAll(updated);
                resolverLinesUpdated = true;
                if (processingContext != null) {
                    processingContext.registerLineItems(chargeCode, updated);
                }
                finalizeCharge(shipment, charge, updated, chargesToUpdate);
            }
        }

        persistRecalculatedLineItems(shipment, charges, lineItemsToSave, chargesToUpdate);
    }

    private void persistRecalculatedLineItems(
            Shipment shipment,
            List<Charge> allCharges,
            List<ChargeLineItem> lineItemsToSave,
            List<Charge> chargesToUpdate) {
        if (!lineItemsToSave.isEmpty()) {
            chargeLineItemRepository.saveAll(lineItemsToSave);
        }
        ChargeCategorySupport.applyShipmentChargeStatuses(shipment, allCharges);
        if (!allCharges.isEmpty()) {
            chargeRepository.saveAll(allCharges);
        } else if (!chargesToUpdate.isEmpty()) {
            chargeRepository.saveAll(chargesToUpdate);
        }
    }

    private List<ChargeLineItem> applyFullRateSheetToResolverLines(
            Shipment shipment,
            Charge charge,
            String chargeCode,
            String rateSheetCategoryType,
            boolean exempted,
            Predicate<ChargeLineItem> lineFilter,
            boolean zeroBeforeApply) {
        if (exempted || charge.getId() == null) {
            return List.of();
        }

        String entityCode = ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, chargeCode);
        if (!StringUtils.hasText(entityCode)) {
            return List.of();
        }

        Optional<Account> account = serviceChargeAccountResolver.resolve(shipment, entityCode, chargeCode);
        if (account.isEmpty()) {
            return List.of();
        }

        Optional<Account> billingAccount =
                serviceChargeAccountResolver.resolveBillingAccount(shipment, entityCode, chargeCode, account.get());
        if (billingAccount.isEmpty()) {
            return List.of();
        }

        List<ChargeLineItem> lineItems = chargeLineItemRepository.findByCharge_Id(charge.getId());
        if (lineItems.isEmpty()) {
            return List.of();
        }

        if (zeroBeforeApply) {
            lineItems.stream().filter(lineFilter).forEach(this::zeroLine);
        }

        Optional<ResolvedRateSheet> resolvedRateSheet =
                rateSheetLookupService.resolveForAccount(account.get(), shipment, rateSheetCategoryType);
        if (resolvedRateSheet.isEmpty()) {
            return zeroBeforeApply ? lineItems : List.of();
        }

        boolean anyUpdated = false;
        for (RateSheetItem rateSheetItem : resolvedRateSheet.get().items()) {
            Optional<String> serviceCodeOpt = ChargeServiceCodeSupport.fromRateSheetItem(rateSheetItem);
            if (serviceCodeOpt.isEmpty()) {
                continue;
            }
            String serviceCode = ChargeServiceCodeSupport.normalize(serviceCodeOpt.get());
            for (ChargeLineItem lineItem : lineItems) {
                if (!serviceCode.equalsIgnoreCase(ChargeServiceCodeSupport.normalize(lineItem.getServiceCode()))
                        || !lineFilter.test(lineItem)) {
                    continue;
                }

                RateAmountCalculator.CalculatedRateAmount calculated =
                        rateAmountCalculator.calculateWithBasisRate(rateSheetItem, shipment, chargeCode);
                ServiceCodes master = loadMaster(entityCode, chargeCode, serviceCode).orElse(null);
                ChargeLineItemTaxSupport.TaxedLineAmount taxed = chargeLineItemTaxSupport.computeFromRawRate(
                        calculated.amount(),
                        shipment,
                        charge,
                        account.get(),
                        entityCode,
                        serviceCode,
                        master,
                        false,
                        calculated.basisExchangeRate());
                chargeLineItemTaxSupport.applyToLineItem(lineItem, taxed);
                lineItem.setAccount(billingAccount.get());
                anyUpdated = true;
            }
        }

        if (anyUpdated || zeroBeforeApply) {
            return lineItems;
        }
        return List.of();
    }

    private List<ChargeLineItem> applyContractedRateDirectFromRateSheet(
            Shipment shipment,
            Charge charge,
            String chargeCode,
            String rateSheetCategoryType,
            boolean exempted,
            boolean zeroContractedRateFirst) {
        if (exempted || charge.getId() == null) {
            return List.of();
        }

        String entityCode = ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, chargeCode);
        if (!StringUtils.hasText(entityCode)) {
            return List.of();
        }

        Optional<Account> account = serviceChargeAccountResolver.resolve(shipment, entityCode, chargeCode);
        if (account.isEmpty()) {
            return List.of();
        }

        Optional<Account> billingAccount =
                serviceChargeAccountResolver.resolveBillingAccount(shipment, entityCode, chargeCode, account.get());
        if (billingAccount.isEmpty()) {
            return List.of();
        }

        List<ChargeLineItem> existingLineItems = chargeLineItemRepository.findByCharge_Id(charge.getId());
        if (existingLineItems.isEmpty()) {
            return List.of();
        }

        Map<String, RateSheetItem> rateSheetByCode = indexRateSheetByServiceCode(
                rateSheetLookupService.resolveForShipmentCharge(shipment, chargeCode, rateSheetCategoryType));
        for (ChargeLineItem line : existingLineItems) {
            line.setAccount(billingAccount.get());
            if (zeroContractedRateFirst) {
                line.setContractedRate(BigDecimal.ZERO);
            }
            applyContractedRateFromRateSheet(line, shipment, charge, chargeCode, rateSheetByCode);
        }
        return existingLineItems;
    }

    private List<ChargeLineItem> copyAmountsOnlyFromSource(
            Shipment shipment,
            Charge charge,
            String chargeCode,
            boolean exempted) {
        if (exempted) {
            return List.of();
        }

        String entityCode = ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, chargeCode);
        if (!StringUtils.hasText(entityCode) || charge.getId() == null) {
            return List.of();
        }

        Optional<Account> account = serviceChargeAccountResolver.resolve(shipment, entityCode, chargeCode);
        if (account.isEmpty()) {
            return List.of();
        }

        Optional<Account> billingAccount =
                serviceChargeAccountResolver.resolveBillingAccount(shipment, entityCode, chargeCode, account.get());
        if (billingAccount.isEmpty()) {
            return List.of();
        }

        List<ChargeLineItem> existingLineItems = chargeLineItemRepository.findByCharge_Id(charge.getId());
        if (existingLineItems.isEmpty()) {
            return List.of();
        }

        List<ChargeLineItem> copied = chargeLineCopySupport.copyToTarget(
                shipment, chargeCode, entityCode, charge, List.of(account.get()));
        if (copied.isEmpty()) {
            return List.of();
        }

        mergeCopiedAmountsIntoExisting(existingLineItems, copied);
        existingLineItems.forEach(line -> line.setAccount(billingAccount.get()));
        return existingLineItems;
    }

    private List<ChargeLineItem> reapplyRevenueCopyWithContractedRate(
            Shipment shipment,
            Charge charge,
            String chargeCode,
            String rateSheetCategoryType,
            boolean exempted,
            boolean zeroContractedRateFirst) {
        String entityCode = ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, chargeCode);
        if (!StringUtils.hasText(entityCode) || charge.getId() == null) {
            return List.of();
        }

        Optional<Account> account = serviceChargeAccountResolver.resolve(shipment, entityCode, chargeCode);
        if (account.isEmpty()) {
            return List.of();
        }

        Optional<Account> billingAccount =
                serviceChargeAccountResolver.resolveBillingAccount(shipment, entityCode, chargeCode, account.get());
        if (billingAccount.isEmpty()) {
            return List.of();
        }

        List<ChargeLineItem> existingLineItems = chargeLineItemRepository.findByCharge_Id(charge.getId());
        if (existingLineItems.isEmpty()) {
            return List.of();
        }

        List<ChargeLineItem> copied = chargeLineCopySupport.copyToTarget(
                shipment, chargeCode, entityCode, charge, List.of(account.get()));
        if (copied.isEmpty()) {
            return List.of();
        }

        if (ShipmentChargeTypeSupport.ECS.equals(chargeCode) || ShipmentChargeTypeSupport.EC3.equals(chargeCode)) {
            mergeCopiedAmountsIntoExisting(existingLineItems, copied);
            existingLineItems.forEach(line -> line.setAccount(billingAccount.get()));
            return existingLineItems;
        }

        if (exempted) {
            existingLineItems.forEach(this::zeroLine);
            return existingLineItems;
        }

        Map<String, RateSheetItem> rateSheetByCode = indexRateSheetByServiceCode(
                rateSheetLookupService.resolveForShipmentCharge(shipment, chargeCode, rateSheetCategoryType));
        mergeCopiedAmountsIntoExisting(existingLineItems, copied);
        for (ChargeLineItem line : existingLineItems) {
            line.setAccount(billingAccount.get());
            if (zeroContractedRateFirst) {
                line.setContractedRate(BigDecimal.ZERO);
            }
            applyContractedRateFromRateSheet(line, shipment, charge, chargeCode, rateSheetByCode);
        }
        return existingLineItems;
    }

    private static boolean isEstimateLineItem(ChargeLineItem lineItem) {
        return lineItem != null && lineItem.getFileReferenceId() == null;
    }

    private List<ChargeLineItem> recalculateResolverCifLines(
            Shipment shipment,
            Charge charge,
            String chargeCode,
            String rateSheetCategoryType,
            boolean exempted) {
        return recalculateResolverCifLines(
                shipment, charge, chargeCode, rateSheetCategoryType, exempted, line -> true);
    }

    private List<ChargeLineItem> recalculateResolverCifLines(
            Shipment shipment,
            Charge charge,
            String chargeCode,
            String rateSheetCategoryType,
            boolean exempted,
            Predicate<ChargeLineItem> lineFilter) {
        if (exempted || charge.getId() == null) {
            return List.of();
        }

        String entityCode = ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, chargeCode);
        if (!StringUtils.hasText(entityCode)) {
            return List.of();
        }

        Optional<Account> account = serviceChargeAccountResolver.resolve(shipment, entityCode, chargeCode);
        if (account.isEmpty()) {
            return List.of();
        }

        Optional<Account> billingAccount =
                serviceChargeAccountResolver.resolveBillingAccount(shipment, entityCode, chargeCode, account.get());
        if (billingAccount.isEmpty()) {
            return List.of();
        }

        Optional<ResolvedRateSheet> resolvedRateSheet =
                rateSheetLookupService.resolveForAccount(account.get(), shipment, rateSheetCategoryType);
        if (resolvedRateSheet.isEmpty()
                || !RateSheetItemCifSupport.resolvedSheetHasCifRate(resolvedRateSheet.get())) {
            return List.of();
        }

        List<ChargeLineItem> lineItems = chargeLineItemRepository.findByCharge_Id(charge.getId());
        if (lineItems.isEmpty()) {
            return List.of();
        }

        boolean anyUpdated = false;
        for (RateSheetItem rateSheetItem : resolvedRateSheet.get().items()) {
            if (!RateSheetItemCifSupport.hasCifRate(rateSheetItem)) {
                continue;
            }
            Optional<String> serviceCodeOpt = ChargeServiceCodeSupport.fromRateSheetItem(rateSheetItem);
            if (serviceCodeOpt.isEmpty()) {
                continue;
            }
            String serviceCode = ChargeServiceCodeSupport.normalize(serviceCodeOpt.get());
            for (ChargeLineItem lineItem : lineItems) {
                if (!serviceCode.equalsIgnoreCase(ChargeServiceCodeSupport.normalize(lineItem.getServiceCode()))
                        || !lineFilter.test(lineItem)) {
                    continue;
                }

                RateAmountCalculator.CalculatedRateAmount calculated =
                        rateAmountCalculator.calculateWithBasisRate(rateSheetItem, shipment, chargeCode);
                ServiceCodes master = loadMaster(entityCode, chargeCode, serviceCode).orElse(null);
                ChargeLineItemTaxSupport.TaxedLineAmount taxed = chargeLineItemTaxSupport.computeFromRawRate(
                        calculated.amount(),
                        shipment,
                        charge,
                        account.get(),
                        entityCode,
                        serviceCode,
                        master,
                        false,
                        calculated.basisExchangeRate());
                chargeLineItemTaxSupport.applyToLineItem(lineItem, taxed);
                lineItem.setAccount(billingAccount.get());
                anyUpdated = true;
            }
        }

        return anyUpdated ? lineItems : List.of();
    }

    private List<ChargeLineItem> recalculateCopyDerivedLines(
            Shipment shipment,
            Charge charge,
            String chargeCode,
            String rateSheetCategoryType,
            boolean exempted) {
        String entityCode = ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, chargeCode);
        if (!StringUtils.hasText(entityCode) || charge.getId() == null) {
            return List.of();
        }

        Optional<Account> account = serviceChargeAccountResolver.resolve(shipment, entityCode, chargeCode);
        if (account.isEmpty()) {
            return List.of();
        }

        Optional<Account> billingAccount =
                serviceChargeAccountResolver.resolveBillingAccount(shipment, entityCode, chargeCode, account.get());
        if (billingAccount.isEmpty()) {
            return List.of();
        }

        List<ChargeLineItem> existingLineItems = chargeLineItemRepository.findByCharge_Id(charge.getId());
        if (existingLineItems.isEmpty()) {
            return List.of();
        }

        List<ChargeLineItem> copied = chargeLineCopySupport.copyToTarget(
                shipment, chargeCode, entityCode, charge, List.of(account.get()));
        if (copied.isEmpty()) {
            return List.of();
        }

        if (ShipmentChargeTypeSupport.ECS.equals(chargeCode) || ShipmentChargeTypeSupport.EC3.equals(chargeCode)) {
            mergeCopiedAmountsIntoExisting(existingLineItems, copied);
            existingLineItems.forEach(line -> line.setAccount(billingAccount.get()));
            return existingLineItems;
        }

        if (exempted) {
            existingLineItems.forEach(this::zeroLine);
            return existingLineItems;
        }

        Map<String, RateSheetItem> rateSheetByCode = indexRateSheetByServiceCode(
                rateSheetLookupService.resolveForShipmentCharge(shipment, chargeCode, rateSheetCategoryType));
        mergeCopiedAmountsIntoExisting(existingLineItems, copied);
        for (ChargeLineItem line : existingLineItems) {
            line.setAccount(billingAccount.get());
            line.setContractedRate(BigDecimal.ZERO);
            applyContractedRateFromRateSheet(line, shipment, charge, chargeCode, rateSheetByCode);
        }
        return existingLineItems;
    }

    private List<ChargeLineItem> createForCharge(
            Shipment shipment, Charge charge, String rateSheetCategoryType, boolean exempted) {
        String chargeCode = ShipmentChargeTypeSupport.normalize(charge.getChargeCode());
        String entityCode = ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, chargeCode);
        if (!StringUtils.hasText(entityCode)) {
            log.debug(CHARGE_LINE_ITEMS_SKIPPED_NO_ENTITY, chargeCode);
            return List.of();
        }

        Optional<Account> account = serviceChargeAccountResolver.resolve(shipment, entityCode, chargeCode);
        if (account.isEmpty()) {
            log.debug(CHARGE_LINE_ITEMS_SKIPPED_NO_ACCOUNT, chargeCode, entityCode);
            return List.of();
        }

        Optional<Account> billingAccount =
                serviceChargeAccountResolver.resolveBillingAccount(shipment, entityCode, chargeCode, account.get());
        if (billingAccount.isEmpty()) {
            return List.of();
        }

        if (chargeLineCopySupport.isCopyDerived(chargeCode)) {
            return createCopyDerivedLines(
                    shipment, charge, chargeCode, entityCode, account.get(), billingAccount.get(), exempted,
                    rateSheetCategoryType);
        }
        return createResolverLines(
                shipment, charge, chargeCode, entityCode, account.get(), billingAccount.get(), exempted,
                rateSheetCategoryType);
    }

    private List<ChargeLineItem> createResolverLines(
            Shipment shipment,
            Charge charge,
            String chargeCode,
            String entityCode,
            Account account,
            Account billingAccount,
            boolean exempted,
            String rateSheetCategoryType) {
        List<ServiceChargeResponse> serviceCharges = serviceChargeResolver.resolve(
                ServiceChargeQueryCriteria.builder()
                        .entityCode(entityCode)
                        .account(account)
                        .chargeTypes(List.of(chargeCode))
                        .active(true)
                        .build());
        if (serviceCharges.isEmpty()) {
            return List.of();
        }

        ShipmentProcessingContext processingContext = ShipmentProcessingContext.get();
        if (processingContext != null) {
            processingContext.ensureServiceChargesIndexed(account, entityCode, chargeCode);
        }

        Map<String, RateSheetItem> rateSheetByCode = indexRateSheetByServiceCode(
                rateSheetLookupService.resolveForAccount(account, shipment, rateSheetCategoryType));

        List<ChargeLineItem> lineItems = new ArrayList<>();
        for (ServiceChargeResponse serviceCharge : serviceCharges) {
            String serviceCode = ChargeServiceCodeSupport.normalize(serviceCharge.getServiceCode());
            if (!StringUtils.hasText(serviceCode)) {
                continue;
            }
            ChargeLineItem line = new ChargeLineItem();
            line.setCharge(charge);
            line.setAccount(billingAccount);
            line.setServiceCode(serviceCode);
            line.setServiceCodeDescription(serviceCharge.getDescription());
            line.setInfoAxsId(serviceCharge.getInfoAxsId());
            line.setContractedRate(BigDecimal.ZERO);
            if (ShipmentChargeTypeSupport.usesEntitySettingsAccountOnly(chargeCode)) {
                ServiceCharge apDefaults = new ServiceCharge();
                apDefaults.setDefaultAccountNumber(serviceCharge.getDefaultAccountNumber());
                line.setApAccountNumber(ServiceChargeDefaultAccountNumberSupport.resolve(account, apDefaults));
            }

            RateSheetItem rateSheetItem = rateSheetByCode.get(serviceCode);
            BigDecimal rawAmount = BigDecimal.ZERO;
            Double basisExchangeRate = null;
            if (!exempted && rateSheetItem != null) {
                RateAmountCalculator.CalculatedRateAmount calculated =
                        rateAmountCalculator.calculateWithBasisRate(rateSheetItem, shipment, chargeCode);
                rawAmount = calculated.amount();
                basisExchangeRate = calculated.basisExchangeRate();
            }

            ServiceCodes master = loadMaster(entityCode, chargeCode, serviceCode).orElse(null);
            ChargeLineItemTaxSupport.TaxedLineAmount taxed = chargeLineItemTaxSupport.computeFromRawRate(
                    rawAmount, shipment, charge, account, entityCode, serviceCode, master, exempted,
                    basisExchangeRate);
            chargeLineItemTaxSupport.applyToLineItem(line, taxed);
            lineItems.add(line);
        }
        return lineItems;
    }

    private List<ChargeLineItem> createCopyDerivedLines(
            Shipment shipment,
            Charge charge,
            String chargeCode,
            String entityCode,
            Account account,
            Account billingAccount,
            boolean exempted,
            String rateSheetCategoryType) {
        List<ChargeLineItem> copied = chargeLineCopySupport.copyToTarget(
                shipment, chargeCode, entityCode, charge, List.of(account));
        if (copied.isEmpty()) {
            return List.of();
        }
        if (ShipmentChargeTypeSupport.ECS.equals(chargeCode) || ShipmentChargeTypeSupport.EC3.equals(chargeCode)) {
            copied.forEach(line -> line.setAccount(billingAccount));
            return copied;
        }
        if (exempted) {
            copied.forEach(this::zeroLine);
            return copied;
        }

        Map<String, RateSheetItem> rateSheetByCode = indexRateSheetByServiceCode(
                rateSheetLookupService.resolveForShipmentCharge(shipment, chargeCode, rateSheetCategoryType));
        for (ChargeLineItem line : copied) {
            line.setAccount(billingAccount);
            applyContractedRateFromRateSheet(
                    line, shipment, charge, chargeCode, rateSheetByCode);
        }
        return copied;
    }

    private void applyContractedRateFromRateSheet(
            ChargeLineItem line,
            Shipment shipment,
            Charge charge,
            String chargeCode,
            Map<String, RateSheetItem> rateSheetByCode) {
        String serviceCode = ChargeServiceCodeSupport.normalize(line.getServiceCode());
        RateSheetItem rateSheetItem = rateSheetByCode.get(serviceCode);
        if (rateSheetItem == null) {
            line.setContractedRate(BigDecimal.ZERO);
            return;
        }

        BigDecimal rawContracted = rateAmountCalculator.calculate(rateSheetItem, shipment, chargeCode);
        line.setContractedRate(chargeLineItemTaxSupport.computeContractedRate(
                rawContracted, shipment, charge));
    }

    private void finalizeCharge(
            Shipment shipment, Charge charge, List<ChargeLineItem> lineItems, List<Charge> chargesToUpdate) {
        shipmentBillingEntitySupport.applyToCharge(charge, shipment);
        chargeAmountSupport.applyChargeHeaderAmounts(shipment, charge, lineItems);
        chargesToUpdate.add(charge);
    }

    public static BigDecimal computeChargeTotal(String chargeCode, List<ChargeLineItem> lineItems) {
        return ChargeHeaderAmountSupport.compute(chargeCode, lineItems).estimateAmount();
    }

    private Optional<ServiceCodes> loadMaster(String entityCode, String chargeCode, String serviceCode) {
        ShipmentProcessingContext processingContext = ShipmentProcessingContext.get();
        if (processingContext != null) {
            return processingContext.findServiceCode(entityCode, chargeCode, serviceCode);
        }
        return serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                entityCode, serviceCode, ShipmentChargeTypeSupport.normalize(chargeCode));
    }

    private static Map<String, RateSheetItem> indexRateSheetByServiceCode(Optional<ResolvedRateSheet> resolved) {
        if (resolved.isEmpty()) {
            return Map.of();
        }
        Map<String, RateSheetItem> indexed = new HashMap<>();
        for (RateSheetItem item : resolved.get().items()) {
            ChargeServiceCodeSupport.fromRateSheetItem(item)
                    .map(ChargeServiceCodeSupport::normalize)
                    .filter(StringUtils::hasText)
                    .ifPresent(code -> indexed.putIfAbsent(code, item));
        }
        return indexed;
    }

    private void zeroLine(ChargeLineItem line) {
        line.setAmount(BigDecimal.ZERO);
        line.setPreTaxableAmount(BigDecimal.ZERO);
        line.setContractedRate(BigDecimal.ZERO);
    }

    private static void mergeCopiedAmountsIntoExisting(
            List<ChargeLineItem> existingLineItems, List<ChargeLineItem> copiedLineItems) {
        Map<String, ChargeLineItem> copiedByServiceCode = new HashMap<>();
        for (ChargeLineItem copied : copiedLineItems) {
            String serviceCode = ChargeServiceCodeSupport.normalize(copied.getServiceCode());
            if (StringUtils.hasText(serviceCode)) {
                copiedByServiceCode.putIfAbsent(serviceCode, copied);
            }
        }
        for (ChargeLineItem existing : existingLineItems) {
            String serviceCode = ChargeServiceCodeSupport.normalize(existing.getServiceCode());
            ChargeLineItem copied = copiedByServiceCode.get(serviceCode);
            if (copied == null) {
                continue;
            }
            existing.setAmount(copied.getAmount());
            existing.setPreTaxableAmount(copied.getPreTaxableAmount());
            existing.setExchangeRate(copied.getExchangeRate());
            existing.setContractedRate(copied.getContractedRate());
            if (StringUtils.hasText(copied.getApAccountNumber())) {
                existing.setApAccountNumber(copied.getApAccountNumber());
            }
        }
    }
}
