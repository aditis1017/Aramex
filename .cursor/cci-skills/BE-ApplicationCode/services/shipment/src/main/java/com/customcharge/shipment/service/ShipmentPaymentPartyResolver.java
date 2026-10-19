package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

import static com.customcharge.shipment.constants.MasterDataCategoryCodes.PAYMENT_TYPE;
import static com.customcharge.shipment.constants.ShipmentConstants.*;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentPaymentPartyResolver {


    public Optional<PaymentParty> resolve(ShipmentDto message) {
        String paymentType = resolvePaymentTypeCode(message.getPaymentType());
        String accountNumber = accountNumberForPaymentType(message, paymentType);
        String entityCode = entityCodeForPaymentType(message, paymentType);
        if (accountNumber == null || entityCode == null) {
            return Optional.empty();
        }
        return Optional.of(new PaymentParty(accountNumber, entityCode));
    }

    private String resolvePaymentTypeCode(String rawPaymentType) {
        return rawPaymentType == null || rawPaymentType.isBlank()
                        ? ""
                        : rawPaymentType.trim().toUpperCase(Locale.ROOT);
    }

    private static String accountNumberForPaymentType(ShipmentDto message, String paymentType) {
        if (!StringUtils.hasText(paymentType)) {
            return null;
        }
        return switch (paymentType.trim().toUpperCase(Locale.ROOT)) {
            case PAYMENT_TYPE_COLLECT ,PAYMENT_TYPE_FREE -> text(message.getConsigneeAccountNumber());
            case PAYMENT_TYPE_PREPAID -> text(message.getShipperAccountNumber());
            case PAYMENT_TYPE_THIRD_PARTY -> text(message.getThirdPartyAccountNumber());
            default -> null;
        };
    }

    private static String entityCodeForPaymentType(ShipmentDto message, String paymentType) {
        if (!StringUtils.hasText(paymentType)) {
            return null;
        }
        return switch (paymentType.trim().toUpperCase(Locale.ROOT)) {
            case PAYMENT_TYPE_PREPAID -> text(message.getHawbOriginEntity());
            case PAYMENT_TYPE_FREE, PAYMENT_TYPE_COLLECT -> text(message.getDestinationEntity());
            case PAYMENT_TYPE_THIRD_PARTY -> text(message.getThirdPartyEntity());
            default -> null;
        };
    }

    private static String text(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public record PaymentParty(String accountNumber, String entityCode) {
    }
}
