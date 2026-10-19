package com.customcharge.chargescalculator.service.ratesheet;

import com.customcharge.chargescalculator.service.calculation.RateAmountCalculator;
import com.customcharge.chargescalculator.service.ratesheet.RateSheetAmountBreakdown;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.RateSheetItemRepository;
import com.customcharge.persistencelayer.repository.RateSheetRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class RateSheetCoreAmountResolver {

    RateSheetRepository rateSheetRepository;
    RateSheetItemRepository rateSheetItemRepository;
    RateAmountCalculator rateAmountCalculator;

    public BigDecimal resolve(Account account, Shipment shipment, ServiceChargeResponse serviceCharge) {
        return resolveBreakdown(account, shipment, serviceCharge).total();
    }

    public RateSheetAmountBreakdown resolveBreakdown(
            Account account,
            Shipment shipment,
            ServiceChargeResponse serviceCharge) {
        if (account == null || account.getId() == null || shipment == null || serviceCharge == null) {
            return RateSheetAmountBreakdown.zero();
        }
        String serviceCode = normalize(serviceCharge.getServiceCode());
        if (serviceCode.isEmpty()) {
            return RateSheetAmountBreakdown.zero();
        }
        return rateSheetRepository.findByAccount_Id(account.getId())
                .filter(this::isActiveRateSheet)
                .map(RateSheet::getId)
                .map(rateSheetId -> resolveBreakdownFromRateSheet(rateSheetId, shipment, serviceCode))
                .orElse(RateSheetAmountBreakdown.zero());
    }

    private RateSheetAmountBreakdown resolveBreakdownFromRateSheet(
            UUID rateSheetId,
            Shipment shipment,
            String serviceCode) {
        List<RateSheetItem> items = rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId);
        return items.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .filter(item -> matchesShipmentType(item, shipment))
                .filter(item -> serviceCode.equalsIgnoreCase(normalize(item.getServiceCode())))
                .findFirst()
                .map(item -> rateAmountCalculator.calculateBreakdown(item, shipment))
                .orElse(RateSheetAmountBreakdown.zero());
    }

    private boolean isActiveRateSheet(RateSheet rateSheet) {
        return rateSheet != null && !Boolean.FALSE.equals(rateSheet.getActive());
    }

    private static boolean matchesShipmentType(RateSheetItem item, Shipment shipment) {
        String itemType = normalize(item.getShipmentType());
        String shipmentType = normalize(shipment.getShipmentType());
        return itemType.equalsIgnoreCase(shipmentType);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
