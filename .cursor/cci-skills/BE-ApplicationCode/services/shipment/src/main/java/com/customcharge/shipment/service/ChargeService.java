package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import com.customcharge.persistencelayer.util.ChargeAccountTypeResolver;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import com.customcharge.shipment.service.charge.ChargeCategorySupport;
import com.customcharge.shipment.service.charge.ChargeCurrencyResolver;
import com.customcharge.shipment.service.charge.DomesticShipmentSupport;
import com.customcharge.shipment.service.charge.ShipmentBillingEntitySupport;
import com.customcharge.shipment.service.charge.ShipmentTypeChargeCodes;
import com.customcharge.shipment.service.charge.UnifiedChargeLineItemService;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContextFactory;
import com.customcharge.shipment.service.cif.ChargeCifValueSupport;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

import static com.customcharge.shipment.constants.LogMessages.CHARGE_CREATION_SKIPPED_NO_CODES;
import static com.customcharge.shipment.constants.LogMessages.CHARGE_CREATION_SKIPPED_NULL;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_DDU;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ChargeService {

    ChargeRepository chargeRepository;
    ShipmentRepository shipmentRepository;
    ShipmentBillingEntitySupport shipmentBillingEntitySupport;
    UnifiedChargeLineItemService unifiedChargeLineItemService;
    ChargeCurrencyResolver chargeCurrencyResolver;
    ChargeCifValueSupport chargeCifValueSupport;
    ShipmentProcessingContextFactory shipmentProcessingContextFactory;
    ChargeAccountTypeResolver chargeAccountTypeResolver;

    @Transactional
    public void createCharges(Shipment shipment) {
        if (shipment == null) {
            log.debug(CHARGE_CREATION_SKIPPED_NULL);
            return;
        }
        List<String> chargeCodes = ShipmentTypeChargeCodes.codesForShipment(
                shipment.getShipmentType(), shipment.getPaymentType());
        if (chargeCodes.isEmpty()) {
            log.debug(CHARGE_CREATION_SKIPPED_NO_CODES,
                    shipment.getShipmentType(), shipment.getPaymentType());
            return;
        }

        shipmentProcessingContextFactory.begin(shipment, chargeCodes);
        try {
            List<Charge> toSave = chargeCodes.stream()
                    .map(code -> buildCharge(shipment, code))
                    .toList();
            List<Charge> savedCharges = chargeRepository.saveAll(toSave);
            ShipmentProcessingContext processingContext = ShipmentProcessingContext.get();
            if (processingContext != null) {
                savedCharges.forEach(charge -> processingContext.registerCharge(charge.getChargeCode(), charge));
            }

            String rateSheetCategoryType = ChargeCategorySupport.captureRateSheetCategory(shipment);
            savedCharges.forEach(charge -> chargeCifValueSupport.applySh013CogSnapshot(shipment, charge));
            unifiedChargeLineItemService.createLineItems(
                    shipment, savedCharges, rateSheetCategoryType, true);
            shipmentRepository.save(shipment);
        } finally {
            shipmentProcessingContextFactory.end();
        }
    }

    private Charge buildCharge(Shipment shipment, String chargeCode) {
        Charge charge = new Charge();
        charge.setShipment(shipment);
        charge.setChargeCode(chargeCode);
        charge.setStatus(ChargeCategorySupport.chargeStatusForCategory(shipment.getCategoryType()));
        charge.setCurrency(chargeCurrencyResolver.resolve(shipment, chargeCode));
        charge.setEstimateAmount(BigDecimal.ZERO);
        charge.setActualAmount(BigDecimal.ZERO);
        charge.setOutstandingAmount(BigDecimal.ZERO);

        shipmentBillingEntitySupport.applyToCharge(charge, shipment);
        chargeAccountTypeResolver.resolve(
                        shipment.getShipmentType(),
                        chargeCode,
                        ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, chargeCode),
                        DomesticShipmentSupport.resolveDtcPartyAccountNumberForAccountType(shipment))
                .ifPresent(charge::setAccountType);
        return charge;
    }
}
