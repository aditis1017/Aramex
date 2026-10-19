package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import com.customcharge.shipment.observability.ShipmentProcessingPhaseTimer;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Resolves DTS/DT3 AR billing accounts from shipment party numbers. When the AR master exists
 * but the per-charge account config is missing, creates a minimal config row (same pattern as
 * {@link DtaDefaultArAccountResolver}). DTC uses {@link RevenueBillingAccountResolver}.
 */
@Service
@RequiredArgsConstructor
public class PartyArAccountResolver {

    private final AccountRepository accountRepository;
    private final ARAccountRepository arAccountRepository;
    private final ObjectProvider<ServiceChargeAccountResolver> serviceChargeAccountResolver;
    private final PartyArAccountChargeConfigService partyArAccountChargeConfigService;
    private final ShipmentProcessingPhaseTimer phaseTimer;

    @Transactional
    public Optional<Account> resolveOrCreate(Shipment shipment, String entityCode, String chargeType) {
        if (shipment == null || entityCode == null || entityCode.isBlank() || chargeType == null || chargeType.isBlank()) {
            return Optional.empty();
        }
        String normalizedChargeType = ShipmentChargeTypeSupport.normalize(chargeType);
        if (!ShipmentChargeTypeSupport.DTS.equals(normalizedChargeType)
                && !ShipmentChargeTypeSupport.DT3.equals(normalizedChargeType)) {
            return Optional.empty();
        }

        Optional<String> partyAccountNumber =
                ShipmentPartyAccountSupport.resolvePartyAccountNumber(shipment, normalizedChargeType);
        if (partyAccountNumber.isEmpty()) {
            return Optional.empty();
        }

        String normalizedEntityCode = entityCode.strip();
        String partyNumber = partyAccountNumber.get();
        String hawb = shipment.getHawbNumber() != null ? shipment.getHawbNumber() : "unknown";
        return phaseTimer.supply(hawb, "partyArAccountResolve", () -> ShipmentProcessingContext.cachedAccount(
                "PARTY_AR:" + partyNumber + ':' + normalizedEntityCode + ':' + normalizedChargeType,
                () -> resolveOrCreateUncached(partyNumber, normalizedEntityCode, normalizedChargeType)));
    }

    private Optional<Account> resolveOrCreateUncached(
            String partyAccountNumber, String entityCode, String chargeType) {
        ServiceChargeAccountResolver chargeAccounts = serviceChargeAccountResolver.getObject();
        Optional<Account> entitySettings = chargeAccounts.resolveEntitySettings(entityCode, chargeType);
        if (entitySettings.isEmpty()) {
            return Optional.empty();
        }

        Optional<Account> existingChargeConfig =
                chargeAccounts.resolveArChargeAccount(partyAccountNumber, entityCode, chargeType);
        if (existingChargeConfig.isPresent()) {
            return existingChargeConfig;
        }

        Optional<ARAccount> arAccount = arAccountRepository.findByAccountNumberAndEntityCode(
                partyAccountNumber, entityCode);
        if (arAccount.isEmpty()) {
            return Optional.empty();
        }

        Optional<Account> existingByMaster =
                accountRepository.findByArAccountIdAndChargeType(arAccount.get().getId(), chargeType);
        if (existingByMaster.isPresent()) {
            return existingByMaster;
        }

        return Optional.of(partyArAccountChargeConfigService.createChargeConfigIfNeeded(
                arAccount.get(), entitySettings.get().getEntity(), chargeType));
    }
}
