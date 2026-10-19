package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.constants.ChargeAccountTypeConstants;
import com.customcharge.persistencelayer.entities.Account;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * Resolves {@link com.customcharge.persistencelayer.entities.Charge#accountType} for shipment charges.
 * DDP charges are always ACCOUNT. DDU uses ACCOUNT for ECD; DTC compares resolved billing AR account
 * number with the shipment consignee number.
 */
@Component
public class ChargeAccountTypeResolver {

    private static final String SHIPMENT_TYPE_DDP = "DDP";
    private static final String SHIPMENT_TYPE_DDU = "DDU";
    private static final String SHIPMENT_TYPE_DDO = "DDO";
    private static final String CHARGE_TYPE_ECD = "ECD";
    private static final String CHARGE_TYPE_DTC = "DTC";

    private final RevenueBillingAccountResolver revenueBillingAccountResolver;

    public ChargeAccountTypeResolver(RevenueBillingAccountResolver revenueBillingAccountResolver) {
        this.revenueBillingAccountResolver = revenueBillingAccountResolver;
    }

    public Optional<String> resolve(
            String shipmentTypeCode, String chargeCode, String entityCode, String consigneeNumber) {
        if (!StringUtils.hasText(shipmentTypeCode) || !StringUtils.hasText(chargeCode)) {
            return Optional.empty();
        }
        String normalizedShipmentType = normalize(shipmentTypeCode);
        String normalizedChargeCode = normalize(chargeCode);

        if (SHIPMENT_TYPE_DDP.equals(normalizedShipmentType)) {
            return Optional.of(ChargeAccountTypeConstants.ACCOUNT);
        }
        if (!SHIPMENT_TYPE_DDU.equals(normalizedShipmentType)
                && !SHIPMENT_TYPE_DDO.equals(normalizedShipmentType)) {
            return Optional.empty();
        }

        if (CHARGE_TYPE_ECD.equals(normalizedChargeCode)) {
            return Optional.of(ChargeAccountTypeConstants.ACCOUNT);
        }
        if (CHARGE_TYPE_DTC.equals(normalizedChargeCode)) {
            return Optional.of(resolveDtcAccountType(entityCode, consigneeNumber));
        }
        return Optional.empty();
    }

    private String resolveDtcAccountType(String entityCode, String consigneeNumber) {
        if (!StringUtils.hasText(entityCode)) {
            return ChargeAccountTypeConstants.CASH;
        }
        Optional<Account> billingAccount = revenueBillingAccountResolver.resolveBillingAccount(
                entityCode.strip(), CHARGE_TYPE_DTC, normalizePartyNumber(consigneeNumber));
        if (billingAccount.isEmpty()) {
            return ChargeAccountTypeConstants.CASH;
        }
        String resolvedAccountNumber = ArAccountMasterSupport.masterAccountNumber(billingAccount.get());
        return accountNumbersMatch(resolvedAccountNumber, consigneeNumber)
                ? ChargeAccountTypeConstants.ACCOUNT
                : ChargeAccountTypeConstants.CASH;
    }

    private static String normalizePartyNumber(String value) {
        return value == null ? null : value.strip();
    }

    private static boolean accountNumbersMatch(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        return left.strip().equalsIgnoreCase(right.strip());
    }

    private static String normalize(String value) {
        return value.strip().toUpperCase(Locale.ROOT);
    }
}
