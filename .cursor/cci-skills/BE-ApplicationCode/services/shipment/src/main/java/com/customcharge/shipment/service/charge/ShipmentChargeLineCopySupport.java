package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService.ChargeLineCopyRequest;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService.CopyMatchMode;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService.CopyTaxMode;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ShipmentChargeLineCopySupport {

    private static final Map<String, CopyPair> COPY_PAIRS = Map.of(
            ShipmentChargeTypeSupport.DTC,
            new CopyPair(ShipmentChargeTypeSupport.ECD, CopyMatchMode.DESCRIPTION, CopyTaxMode.AMOUNT_ONLY),
            ShipmentChargeTypeSupport.DTA,
            new CopyPair(ShipmentChargeTypeSupport.ECD, CopyMatchMode.DESCRIPTION, CopyTaxMode.AMOUNT_ONLY),
            ShipmentChargeTypeSupport.ECS,
            new CopyPair(
                    ShipmentChargeTypeSupport.DTA,
                    CopyMatchMode.SERVICE_CHARGE_MAPPING,
                    CopyTaxMode.EFFECTIVE_X),
            ShipmentChargeTypeSupport.EC3,
            new CopyPair(
                    ShipmentChargeTypeSupport.DTA,
                    CopyMatchMode.SERVICE_CHARGE_MAPPING,
                    CopyTaxMode.EFFECTIVE_X),
            ShipmentChargeTypeSupport.DTS,
            new CopyPair(ShipmentChargeTypeSupport.ECS, CopyMatchMode.DESCRIPTION, CopyTaxMode.AMOUNT_ONLY),
            ShipmentChargeTypeSupport.DT3,
            new CopyPair(ShipmentChargeTypeSupport.EC3, CopyMatchMode.DESCRIPTION, CopyTaxMode.AMOUNT_ONLY));

    private final ChargeLineCopyService chargeLineCopyService;
    private final ChargeRepository chargeRepository;
    private final ChargeLineItemRepository chargeLineItemRepository;

    public ShipmentChargeLineCopySupport(
            ChargeLineCopyService chargeLineCopyService,
            ChargeRepository chargeRepository,
            ChargeLineItemRepository chargeLineItemRepository) {
        this.chargeLineCopyService = chargeLineCopyService;
        this.chargeRepository = chargeRepository;
        this.chargeLineItemRepository = chargeLineItemRepository;
    }

    public boolean isCopyDerived(String chargeCode) {
        return chargeCode != null && COPY_PAIRS.containsKey(normalize(chargeCode));
    }

    public List<ChargeLineItem> copyToTarget(
            Shipment shipment,
            String targetChargeCode,
            String targetEntityCode,
            Charge targetCharge,
            List<Account> targetAccounts) {
        CopyPair pair = COPY_PAIRS.get(normalize(targetChargeCode));
        if (pair == null) {
            throw new IllegalStateException("No copy pair for charge: " + targetChargeCode);
        }
        Charge sourceCharge = resolveSourceCharge(shipment, pair.sourceChargeType());
        ChargeLineCopyRequest request = new ChargeLineCopyRequest(
                pair.sourceChargeType(),
                normalize(targetChargeCode),
                shipment,
                resolveSourceEntityCode(shipment, pair.sourceChargeType()),
                targetEntityCode,
                sourceCharge,
                targetCharge,
                targetAccounts,
                resolveSourceLineItems(sourceCharge, pair.sourceChargeType()),
                null,
                true,
                pair.matchMode(),
                null,
                null,
                pair.taxMode());
        return chargeLineCopyService.copy(request);
    }

    private Charge resolveSourceCharge(Shipment shipment, String sourceChargeType) {
        ShipmentProcessingContext context = ShipmentProcessingContext.get();
        if (context != null) {
            return context.findChargeByCode(sourceChargeType)
                    .orElseThrow(() -> new IllegalStateException(
                            "Source charge not found: " + sourceChargeType));
        }
        return chargeRepository
                .findByShipment_IdAndChargeCode(shipment.getId(), sourceChargeType)
                .orElseThrow(() -> new IllegalStateException(
                        "Source charge not found: " + sourceChargeType));
    }

    private List<ChargeLineItem> resolveSourceLineItems(Charge sourceCharge, String sourceChargeType) {
        ShipmentProcessingContext context = ShipmentProcessingContext.get();
        if (context != null) {
            return context.findLineItemsByChargeCode(sourceChargeType)
                    .orElseGet(() -> chargeLineItemRepository.findByCharge_Id(sourceCharge.getId()));
        }
        return chargeLineItemRepository.findByCharge_Id(sourceCharge.getId());
    }

    private String resolveSourceEntityCode(Shipment shipment, String sourceChargeType) {
        return switch (normalize(sourceChargeType)) {
            case ShipmentChargeTypeSupport.ECS ->
                    blankToEmpty(shipment.getOriginEntity());
            case ShipmentChargeTypeSupport.EC3 ->
                    ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, ShipmentChargeTypeSupport.EC3);
            default -> ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, sourceChargeType);
        };
    }

    private static String normalize(String chargeCode) {
        return chargeCode.strip().toUpperCase(Locale.ROOT);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.strip();
    }

    private record CopyPair(String sourceChargeType, CopyMatchMode matchMode, CopyTaxMode taxMode) {
    }
}
