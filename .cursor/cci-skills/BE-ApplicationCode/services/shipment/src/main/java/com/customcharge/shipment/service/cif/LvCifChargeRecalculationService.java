package com.customcharge.shipment.service.cif;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import com.customcharge.shipment.service.charge.ChargeCategorySupport;
import com.customcharge.shipment.service.charge.ChargeLineItemTaxSupport;
import com.customcharge.shipment.service.charge.ChargeServiceCodeSupport;
import com.customcharge.shipment.service.charge.ServiceCodeEligibilityService;
import com.customcharge.shipment.service.charge.ShipmentBillingEntitySupport;
import com.customcharge.shipment.service.charge.ShipmentChargeAmountSupport;
import com.customcharge.shipment.service.charge.ShipmentChargeAmountSupport;
import com.customcharge.shipment.service.charge.processing.calculation.RateAmountCalculator;
import com.customcharge.shipment.service.charge.processing.model.ResolvedRateSheet;
import com.customcharge.shipment.service.charge.processing.ratesheet.RateSheetLookupService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.customcharge.shipment.constants.LogMessages.CIF_LV_LINE_ITEMS_RECALCULATED;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class LvCifChargeRecalculationService {

    RateSheetLookupService rateSheetLookupService;
    ChargeLineItemRepository chargeLineItemRepository;
    ChargeRepository chargeRepository;
    RateAmountCalculator rateAmountCalculator;
    ShipmentChargeAmountSupport chargeAmountSupport;
    ShipmentBillingEntitySupport shipmentBillingEntitySupport;
    ServiceCodeEligibilityService serviceCodeEligibilityService;
    AccountRedisChargeDataService accountRedisChargeDataService;
    ChargeLineItemTaxSupport chargeLineItemTaxSupport;

    public void recalculate(Shipment shipment, List<Charge> charges) {
        for (Charge charge : charges) {
            if (recalculateCifRateSheetLines(shipment, charge)) {
                finalizeChargeTotals(shipment, charge);
            }
        }
        ChargeCategorySupport.applyShipmentChargeStatuses(shipment, charges);
        if (!charges.isEmpty()) {
            chargeRepository.saveAll(charges);
        }
    }

    private boolean recalculateCifRateSheetLines(Shipment shipment, Charge charge) {
        Optional<ResolvedRateSheet> resolvedRateSheet =
                rateSheetLookupService.resolveForShipmentCharge(shipment, charge.getChargeCode());
        if (resolvedRateSheet.isEmpty()
                || !RateSheetItemCifSupport.resolvedSheetHasCifRate(resolvedRateSheet.get())) {
            return false;
        }

        List<ChargeLineItem> lineItems = chargeLineItemRepository.findByCharge_Id(charge.getId());
        if (lineItems.isEmpty()) {
            return false;
        }

        ResolvedRateSheet resolved = resolvedRateSheet.get();
        Account account = resolved.account();
        String entityCode = account != null ? account.getEntityCode() : null;
        String chargeCode = charge.getChargeCode();
        String normalizedChargeCode = ShipmentChargeTypeSupport.normalize(chargeCode);

        boolean anyUpdated = false;
        for (RateSheetItem rateSheetItem : resolved.items()) {
            if (!RateSheetItemCifSupport.hasCifRate(rateSheetItem)) {
                continue;
            }
            if (!serviceCodeEligibilityService.isRateSheetItemEligible(
                    account, entityCode, chargeCode, rateSheetItem)) {
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

            RateAmountCalculator.CalculatedRateAmount calculated =
                    rateAmountCalculator.calculateWithBasisRate(rateSheetItem, shipment, chargeCode);
            Optional<ServiceCodes> master = accountRedisChargeDataService.findServiceCodeMaster(
                    entityCode, normalizedChargeCode, normalizedServiceCode);
            ChargeLineItemTaxSupport.TaxedLineAmount taxed = chargeLineItemTaxSupport.computeFromRawRate(
                    calculated.amount(), shipment, charge, account, entityCode, normalizedServiceCode,
                    master.orElse(null), calculated.basisExchangeRate());
            chargeLineItemTaxSupport.applyToLineItem(lineItem, taxed);
            anyUpdated = true;
        }

        if (anyUpdated) {
            chargeLineItemRepository.saveAll(lineItems);
        }
        return anyUpdated;
    }

    private void finalizeChargeTotals(Shipment shipment, Charge charge) {
        List<ChargeLineItem> lineItems = chargeLineItemRepository.findByCharge_Id(charge.getId());
        chargeAmountSupport.applyChargeHeaderAmounts(shipment, charge, lineItems);
        shipmentBillingEntitySupport.applyToCharge(charge, shipment);
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
}
