package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.shipment.service.masterdata.MasterDataCodeLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentPaymentPartyResolverTest {

    @Mock
    private MasterDataCodeLookupService masterDataCodeLookupService;

    @InjectMocks
    private ShipmentPaymentPartyResolver resolver;

    @Test
    void resolve_shouldUseConsigneeAndOriginEntity_whenPaymentTypeCollect() {
        ShipmentDto message = baseMessage();
        message.setPaymentType("C");
        message.setConsigneeAccountNumber("CONS-1");
        message.setDestinationEntity("RUH");
        when(masterDataCodeLookupService.resolveCode(eq("PAYMENT_TYPE"), anyString()))
                .thenReturn(Optional.of("C"));

        ShipmentPaymentPartyResolver.PaymentParty party = resolver.resolve(message).orElseThrow();

        assertThat(party.accountNumber()).isEqualTo("CONS-1");
        assertThat(party.entityCode()).isEqualTo("RUH");
    }

    @Test
    void resolve_shouldUseShipperAndOriginEntity_whenPaymentTypePrepaid() {
        ShipmentDto message = baseMessage();
        message.setPaymentType("P");
        message.setShipperAccountNumber("SHIP-1");
        when(masterDataCodeLookupService.resolveCode(eq("PAYMENT_TYPE"), anyString()))
                .thenReturn(Optional.of("P"));

        ShipmentPaymentPartyResolver.PaymentParty party = resolver.resolve(message).orElseThrow();

        assertThat(party.accountNumber()).isEqualTo("SHIP-1");
        assertThat(party.entityCode()).isEqualTo("RUH");
    }

    @Test
    void resolve_shouldUseThirdPartyNumberAndThirdPartyEntity_whenPaymentTypeThirdParty() {
        ShipmentDto message = baseMessage();
        message.setPaymentType("3");
        message.setThirdPartyAccountNumber("TP-1");
        message.setThirdPartyEntity("JED");
        when(masterDataCodeLookupService.resolveCode(eq("PAYMENT_TYPE"), anyString()))
                .thenReturn(Optional.of("3"));

        ShipmentPaymentPartyResolver.PaymentParty party = resolver.resolve(message).orElseThrow();

        assertThat(party.accountNumber()).isEqualTo("TP-1");
        assertThat(party.entityCode()).isEqualTo("JED");
    }

    @Test
    void resolve_shouldReturnEmpty_whenThirdPartyEntityMissingForPaymentTypeThirdParty() {
        ShipmentDto message = baseMessage();
        message.setPaymentType("3");
        message.setThirdPartyNumber("TP-1");
        when(masterDataCodeLookupService.resolveCode(eq("PAYMENT_TYPE"), anyString()))
                .thenReturn(Optional.of("3"));

        assertThat(resolver.resolve(message)).isEmpty();
    }

    @Test
    void resolve_shouldReturnEmpty_whenOriginEntityMissing() {
        ShipmentDto message = new ShipmentDto();
        message.setPaymentType("C");
        message.setConsigneeAccountNumber("CONS-1");

        assertThat(resolver.resolve(message)).isEmpty();
    }

    private static ShipmentDto baseMessage() {
        ShipmentDto message = new ShipmentDto();
        message.setHawbOriginEntity("RUH");
        return message;
    }
}
