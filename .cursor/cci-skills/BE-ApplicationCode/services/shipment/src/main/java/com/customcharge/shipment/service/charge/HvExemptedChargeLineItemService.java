package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.customcharge.shipment.constants.LogMessages.CIF_LINE_ITEMS_RECALCULATED;

/**
 * CIF-driven recalculation for charge line items created by {@link UnifiedChargeLineItemService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class HvExemptedChargeLineItemService {

    ServiceChargeAccountResolver serviceChargeAccountResolver;
    AccountRedisChargeDataService accountRedisChargeDataService;
    ChargeLineItemRepository chargeLineItemRepository;
    ChargeRepository chargeRepository;
    ShipmentBillingEntitySupport shipmentBillingEntitySupport;
    RateSheetLookupService rateSheetLookupService;
    RateAmountCalculator rateAmountCalculator;
    ShipmentChargeAmountSupport chargeAmountSupport;
    ChargeLineItemTaxSupport chargeLineItemTaxSupport;

    /**
     * Recalculates charge line items whose rate-sheet item includes a CIF percentage rate.
     */
    public void recalculateCifAffectedLineItems(Shipment shipment, Charge charge) {
        if (shipment == null || charge == null || !StringUtils.hasText(charge.getChargeCode())) {
            return;
        }
        String chargeCode = ShipmentChargeTypeSupport.normalize(charge.getChargeCode());
        boolean exempted = ChargeCategorySupport.isExempted(shipment.getCategoryType());
        if (exempted) {
            return;
        }

        String entityCode = ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, chargeCode);
        if (!StringUtils.hasText(entityCode)) {
            return;
        }

        Optional<Account> account = serviceChargeAccountResolver.resolve(shipment, entityCode, chargeCode);
        if (account.isEmpty()) {
            return;
        }

        Optional<Account> billingAccount =
                serviceChargeAccountResolver.resolveBillingAccount(shipment, entityCode, chargeCode, account.get());
        if (billingAccount.isEmpty()) {
            return;
        }

        String rateSheetCategoryType = ChargeCategorySupport.captureRateSheetCategory(shipment);
        Optional<ResolvedRateSheet> resolvedRateSheet = chargeLineCopySupportIsRevenue(chargeCode)
                ? rateSheetLookupService.resolveForShipmentCharge(shipment, chargeCode, rateSheetCategoryType)
                : rateSheetLookupService.resolveForAccount(account.get(), shipment, rateSheetCategoryType);
        if (resolvedRateSheet.isEmpty()
                || !RateSheetItemCifSupport.resolvedSheetHasCifRate(resolvedRateSheet.get())) {
            return;
        }

        List<ChargeLineItem> lineItems = chargeLineItemRepository.findByCharge_Id(charge.getId());
        if (lineItems.isEmpty()) {
            return;
        }

        Map<UUID, ServiceCharge> chargeByMasterId =
                loadAccountChargesByMasterId(account.get(), entityCode, chargeCode);
        List<ServiceCodes> masters = loadMastersForFetch(entityCode, chargeCode);

        boolean updated = false;
        for (RateSheetItem rateSheetItem : resolvedRateSheet.get().items()) {
            if (!RateSheetItemCifSupport.hasCifRate(rateSheetItem)) {
                continue;
            }
            Optional<String> serviceCodeOpt = ChargeServiceCodeSupport.fromRateSheetItem(rateSheetItem);
            if (serviceCodeOpt.isEmpty()) {
                continue;
            }
            String normalizedServiceCode = ChargeServiceCodeSupport.normalize(serviceCodeOpt.get());
            ChargeLineItem lineItem = findLineItemByServiceCode(lineItems, normalizedServiceCode);
            if (lineItem == null) {
                continue;
            }

            ServiceCodes master = resolveMasterForServiceCode(
                    chargeCode, masters, chargeByMasterId, normalizedServiceCode);
            RateAmountCalculator.CalculatedRateAmount calculated = rateAmountCalculator.calculateWithBasisRate(
                    rateSheetItem, shipment, charge.getChargeCode());
            ChargeLineItemTaxSupport.TaxedLineAmount taxed = chargeLineItemTaxSupport.computeFromRawRate(
                    calculated.amount(),
                    shipment,
                    charge,
                    account.get(),
                    entityCode,
                    normalizedServiceCode,
                    master,
                    false,
                    calculated.basisExchangeRate());
            chargeLineItemTaxSupport.applyToLineItem(lineItem, taxed);
            lineItem.setAccount(billingAccount.get());
            updated = true;
        }

        if (updated) {
            chargeLineItemRepository.saveAll(lineItems);
            finalizeCharge(shipment, charge, lineItems);
        }
    }

    private void finalizeCharge(Shipment shipment, Charge charge, List<ChargeLineItem> lineItems) {
        shipmentBillingEntitySupport.applyToCharge(charge, shipment);
        chargeAmountSupport.applyChargeHeaderAmounts(shipment, charge, lineItems);
        if (shipment.getId() == null) {
            chargeRepository.save(charge);
            return;
        }
        List<Charge> shipmentCharges = chargeRepository.findByShipment_IdWithEntity(shipment.getId());
        if (shipmentCharges.isEmpty()) {
            shipmentCharges = List.of(charge);
        }
        ChargeCategorySupport.applyShipmentChargeStatuses(shipment, shipmentCharges);
        chargeRepository.saveAll(shipmentCharges);
    }

    private static boolean chargeLineCopySupportIsRevenue(String chargeCode) {
        return ShipmentChargeTypeSupport.isRevenueCharge(chargeCode);
    }

    private List<ServiceCodes> loadMastersForFetch(String entityCode, String chargeCode) {
        return accountRedisChargeDataService.getEntityServiceCodeMasters(
                entityCode, ShipmentChargeTypeSupport.normalize(chargeCode));
    }

    private Map<UUID, ServiceCharge> loadAccountChargesByMasterId(
            Account account, String entityCode, String chargeCode) {
        return accountRedisChargeDataService.loadServiceChargesByMasterId(
                account, entityCode, ShipmentChargeTypeSupport.normalize(chargeCode));
    }

    private static ChargeLineItem findLineItemByServiceCode(
            List<ChargeLineItem> lineItems, String normalizedServiceCode) {
        for (ChargeLineItem lineItem : lineItems) {
            if (normalizedServiceCode.equalsIgnoreCase(
                    ChargeServiceCodeSupport.normalize(lineItem.getServiceCode()))) {
                return lineItem;
            }
        }
        return null;
    }

    private ServiceCodes resolveMasterForServiceCode(
            String chargeCode,
            List<ServiceCodes> masters,
            Map<UUID, ServiceCharge> chargeByMasterId,
            String normalizedServiceCode) {
        for (ServiceCodes master : masters) {
            Optional<String> masterServiceCode = resolveServiceCode(chargeCode, master, null);
            if (masterServiceCode.isEmpty()
                    || !normalizedServiceCode.equalsIgnoreCase(
                            ChargeServiceCodeSupport.normalize(masterServiceCode.get()))) {
                continue;
            }
            return master;
        }
        return null;
    }

    private Optional<String> resolveServiceCode(
            String chargeCode, ServiceCodes master, ServiceCharge accountCharge) {
        if (master == null || !StringUtils.hasText(master.getServiceCode())) {
            return Optional.empty();
        }
        if (accountCharge != null) {
            ServiceCodes linkedMaster = accountCharge.getServiceCodes();
            if (linkedMaster != null && StringUtils.hasText(linkedMaster.getServiceCode())) {
                return Optional.of(linkedMaster.getServiceCode().strip());
            }
        }
        return Optional.of(master.getServiceCode().strip());
    }
}
