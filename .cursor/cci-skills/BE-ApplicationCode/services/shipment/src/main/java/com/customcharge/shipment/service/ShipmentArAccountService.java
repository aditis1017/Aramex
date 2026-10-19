package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.util.AccountLoaders;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

import static com.customcharge.shipment.service.charge.DomesticShipmentSupport.isDomesticProductGroup;
import static com.customcharge.shipment.service.charge.DomesticShipmentSupport.resolveDomEntityCode;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentArAccountService {

    ShipmentPaymentPartyResolver paymentPartyResolver;
    ARAccountRepository arAccountRepository;

    public Optional<ARAccount> findArAccount(ShipmentDto message) {
        if (isDomesticProductGroup(message.getProductGroup())) {
            String shipperAccountNumber = text(message.getShipperAccountNumber());
            if (!StringUtils.hasText(shipperAccountNumber)) {
                return Optional.empty();
            }
            return AccountLoaders.findArMaster(
                    arAccountRepository,
                    shipperAccountNumber,
                    resolveDomEntityCode(message.getConsigneeCountryCode()));
        }
        return paymentPartyResolver
                .resolve(message)
                .flatMap(party -> AccountLoaders.findArMaster(
                        arAccountRepository, party.accountNumber(), party.entityCode()));
    }

    private static String text(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public Optional<String> resolveDtcShipmentType(ARAccount arAccount) {
        if (arAccount == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(arAccount.getCustomType())
                .filter(StringUtils::hasText)
                .map(String::trim);
    }
}
