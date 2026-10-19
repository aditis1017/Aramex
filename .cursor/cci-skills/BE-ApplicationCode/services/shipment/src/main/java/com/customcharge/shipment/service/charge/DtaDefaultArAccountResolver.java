package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.util.RevenueBillingAccountResolver;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Resolves the DTA account configured by destination entity settings (default AR, then entity settings).
 */
@Service
@RequiredArgsConstructor
public class DtaDefaultArAccountResolver {

    public static final String DTA = RevenueBillingAccountResolver.CHARGE_TYPE_DTA;

    private final RevenueBillingAccountResolver revenueBillingAccountResolver;

    @Transactional
    public Optional<Account> resolveOrCreate(String entityCode) {
        if (entityCode == null || entityCode.isBlank()) {
            return Optional.empty();
        }
        String normalizedEntityCode = entityCode.strip();
        return ShipmentProcessingContext.cachedDtaArAccount(
                normalizedEntityCode,
                () -> revenueBillingAccountResolver.resolveBillingAccount(
                        normalizedEntityCode, DTA, null));
    }
}
