package com.customcharge.shipment.service.charge.processing.ratesheet;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.accountRedis.AccountRedisRateSheetSnapshot;
import com.customcharge.persistencelayer.util.RateSheetItemMatchSupport;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import com.customcharge.shipment.service.charge.PartyArAccountResolver;
import com.customcharge.shipment.service.charge.ServiceChargeAccountResolver;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import com.customcharge.shipment.service.charge.processing.model.ResolvedRateSheet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.customcharge.shipment.constants.LogMessages.ENTITY_SETTINGS_ACCOUNT_NOT_FOUND;
import static com.customcharge.shipment.constants.LogMessages.RATE_SHEET_NO_BILLABLE_ITEMS;
import static com.customcharge.shipment.constants.LogMessages.RATE_SHEET_SKIPPED_NO_ENTITY;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class RateSheetLookupService {

    AccountRedisChargeDataService accountRedisChargeDataService;
    ServiceChargeAccountResolver serviceChargeAccountResolver;
    PartyArAccountResolver partyArAccountResolver;

    public Optional<ResolvedRateSheet> resolveForEntitySettings(Shipment shipment, String chargeType) {
        return resolveForEntitySettings(shipment, chargeType, null);
    }

    public Optional<ResolvedRateSheet> resolveForEntitySettings(
            Shipment shipment, String chargeType, String rateSheetCategoryType) {
        String entityCode = billingEntityCode(shipment, chargeType);
        if (entityCode.isEmpty()) {
            log.warn(RATE_SHEET_SKIPPED_NO_ENTITY, chargeType);
            return Optional.empty();
        }
        return resolveForEntitySettings(entityCode, chargeType, shipment, rateSheetCategoryType);
    }

    public Optional<ResolvedRateSheet> resolveForEntitySettings(String entityCode, String chargeType) {
        return resolveForEntitySettings(entityCode, chargeType, null, null);
    }

    public Optional<ResolvedRateSheet> resolveForEntitySettings(
            String entityCode,
            String chargeType,
            Shipment shipment) {
        return resolveForEntitySettings(entityCode, chargeType, shipment, null);
    }

    public Optional<ResolvedRateSheet> resolveForEntitySettings(
            String entityCode,
            String chargeType,
            Shipment shipment,
            String rateSheetCategoryType) {
        if (entityCode == null || entityCode.isBlank() || chargeType == null || chargeType.isBlank()) {
            return Optional.empty();
        }

        Optional<Account> account = serviceChargeAccountResolver.resolveEntitySettings(
                entityCode.trim(), chargeType.trim());
        if (account.isEmpty()) {
            log.warn(ENTITY_SETTINGS_ACCOUNT_NOT_FOUND, entityCode, chargeType);
            return Optional.empty();
        }
        return resolveForAccount(account.get(), shipment, rateSheetCategoryType);
    }

    public Optional<ResolvedRateSheet> resolveForAccount(Account account) {
        return resolveForAccount(account, null, null);
    }

    public Optional<ResolvedRateSheet> resolveForAccount(Account account, Shipment shipment) {
        return resolveForAccount(account, shipment, null);
    }

    public Optional<ResolvedRateSheet> resolveForAccount(
            Account account, Shipment shipment, String rateSheetCategoryType) {
        if (account == null || account.getId() == null) {
            return Optional.empty();
        }
        return ShipmentProcessingContext.cachedRateSheet(
                account.getId(),
                () -> accountRedisChargeDataService
                        .getRateSheetSnapshot(account, account.getEntityCode(), account.getChargeType())
                        .flatMap(snapshot -> toResolvedRateSheet(account, snapshot, shipment, rateSheetCategoryType)));
    }

    public Optional<ResolvedRateSheet> resolveForArAccountNumber(
            String arAccountNumber, Shipment shipment, String chargeType) {
        return resolveForArAccountNumber(arAccountNumber, shipment, chargeType, null);
    }

    public Optional<ResolvedRateSheet> resolveForArAccountNumber(
            String arAccountNumber,
            Shipment shipment,
            String chargeType,
            String rateSheetCategoryType) {
        if (arAccountNumber == null || arAccountNumber.isBlank()) {
            return Optional.empty();
        }
        String entityCode = billingEntityCode(shipment, chargeType);
        if (entityCode.isEmpty()) {
            return Optional.empty();
        }
        return serviceChargeAccountResolver
                .resolveArChargeAccount(arAccountNumber.trim(), entityCode, chargeType)
                .flatMap(account -> resolveForAccount(account, shipment, rateSheetCategoryType));
    }

    /**
     * Resolves the active rate sheet for a shipment charge. DTC and DTA use the same unified account
     * resolver as service charges and billing; when the resolved AR account has no matching rows,
     * entity settings is used. When AR rows match the shipment criteria, AR is used. DTS and DT3
     * use party AR before the same entity fallback rules. Used for SH013 charge creation and SH267
     * recalculation paths that resolve revenue copy-derived charges.
     */
    public Optional<ResolvedRateSheet> resolveForShipmentCharge(Shipment shipment, String chargeTypeCode) {
        return resolveForShipmentCharge(shipment, chargeTypeCode, null);
    }

    public Optional<ResolvedRateSheet> resolveForShipmentCharge(
            Shipment shipment, String chargeTypeCode, String rateSheetCategoryType) {
        if (shipment == null || chargeTypeCode == null || chargeTypeCode.isBlank()) {
            return Optional.empty();
        }
        String normalizedChargeType = chargeTypeCode.strip();
        if (ShipmentChargeTypeSupport.usesEntitySettingsAccountOnly(normalizedChargeType)) {
            return resolveForEntitySettings(shipment, normalizedChargeType, rateSheetCategoryType);
        }
        if (ShipmentChargeTypeSupport.DTA.equalsIgnoreCase(normalizedChargeType)
                || ShipmentChargeTypeSupport.DTC.equalsIgnoreCase(normalizedChargeType)) {
            return resolveForUnifiedRevenueCharge(shipment, normalizedChargeType, rateSheetCategoryType);
        }
        if (ShipmentChargeTypeSupport.DTS.equalsIgnoreCase(normalizedChargeType)
                || ShipmentChargeTypeSupport.DT3.equalsIgnoreCase(normalizedChargeType)) {
            return resolveForPartyAr(shipment, normalizedChargeType, rateSheetCategoryType);
        }

        return resolveForEntitySettings(shipment, normalizedChargeType, rateSheetCategoryType);
    }

    private Optional<ResolvedRateSheet> resolveForUnifiedRevenueCharge(
            Shipment shipment, String chargeType, String rateSheetCategoryType) {
        String entityCode = billingEntityCode(shipment, chargeType);
        if (entityCode.isEmpty()) {
            return Optional.empty();
        }
        return resolveForArWithEntityFallback(
                shipment,
                chargeType,
                rateSheetCategoryType,
                serviceChargeAccountResolver.resolve(shipment, entityCode, chargeType));
    }

    private Optional<ResolvedRateSheet> resolveForPartyAr(
            Shipment shipment, String chargeType, String rateSheetCategoryType) {
        String entityCode = billingEntityCode(shipment, chargeType);
        if (entityCode.isEmpty()) {
            return Optional.empty();
        }
        return resolveForArWithEntityFallback(
                shipment,
                chargeType,
                rateSheetCategoryType,
                partyArAccountResolver.resolveOrCreate(shipment, entityCode, chargeType));
    }

    /**
     * Resolves billable rate sheet rows from the resolved account. When the resolved AR account has
     * no matching rows (including no rate sheet at all), entity settings is used. When AR rows
     * match the shipment criteria, AR is used and entity settings is not consulted.
     */
    private Optional<ResolvedRateSheet> resolveForArWithEntityFallback(
            Shipment shipment,
            String chargeType,
            String rateSheetCategoryType,
            Optional<Account> resolvedAccount) {
        if (resolvedAccount.isEmpty()) {
            return resolveForEntitySettings(shipment, chargeType, rateSheetCategoryType);
        }

        Account account = resolvedAccount.get();
        Optional<ResolvedRateSheet> fromAccount = resolveForAccount(account, shipment, rateSheetCategoryType);
        if (fromAccount.isPresent()) {
            return fromAccount;
        }

        if (isArAccount(account)) {
            return resolveForEntitySettings(shipment, chargeType, rateSheetCategoryType);
        }

        return Optional.empty();
    }

    private static boolean isArAccount(Account account) {
        return account != null && AccountNature.AR.equals(account.getAccountNature());
    }

    private Optional<ResolvedRateSheet> toResolvedRateSheet(
            Account account,
            AccountRedisRateSheetSnapshot snapshot,
            Shipment shipment,
            String rateSheetCategoryType) {
        RateSheet rateSheet = snapshot.rateSheet();
        if (rateSheet == null || rateSheet.getId() == null) {
            return Optional.empty();
        }
        List<RateSheetItem> items = loadBillableItems(snapshot.items(), shipment, rateSheetCategoryType);
        if (items.isEmpty()) {
            log.warn(RATE_SHEET_NO_BILLABLE_ITEMS,
                    rateSheet.getId(), shipment != null ? shipment.getShipmentType() : null);
            return Optional.empty();
        }
        return Optional.of(new ResolvedRateSheet(account, rateSheet, items));
    }

    private List<RateSheetItem> loadBillableItems(
            List<RateSheetItem> sourceItems, Shipment shipment, String rateSheetCategoryType) {
        if (sourceItems == null || sourceItems.isEmpty()) {
            return List.of();
        }
        List<RateSheetItem> matchingItems = sourceItems.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .filter(item -> RateSheetItemMatchSupport.matchesRateSheetItem(
                        item, shipment, rateSheetCategoryType))
                .filter(item -> item.getRates() != null && !item.getRates().isEmpty())
                .toList();
        return RateSheetItemMatchSupport.selectMostSpecificPerServiceCode(matchingItems);
    }

    private static String billingEntityCode(Shipment shipment, String chargeType) {
        return ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, chargeType);
    }
}
