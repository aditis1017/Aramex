package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.constant.DomEntityConstants;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentArAccountServiceTest {

    @Mock
    private ShipmentPaymentPartyResolver paymentPartyResolver;

    @Mock
    private ARAccountRepository arAccountRepository;

    @InjectMocks
    private ShipmentArAccountService shipmentArAccountService;

    @Test
    void findArAccount_shouldUseRuhAndShipper_whenProductGroupDom() {
        ShipmentDto message = new ShipmentDto();
        message.setProductGroup("DOM");
        message.setShipperAccountNumber(" SHIP-1 ");
        ARAccount arAccount = new ARAccount();

        when(arAccountRepository.findByAccountNumberAndEntityCode(
                "SHIP-1", DomEntityConstants.DEFAULT_DOM_ENTITY_CODE))
                .thenReturn(Optional.of(arAccount));

        Optional<ARAccount> result = shipmentArAccountService.findArAccount(message);

        assertThat(result).containsSame(arAccount);
        verify(paymentPartyResolver, never()).resolve(message);
        verify(arAccountRepository).findByAccountNumberAndEntityCode(
                "SHIP-1", DomEntityConstants.DEFAULT_DOM_ENTITY_CODE);
    }

    @Test
    void findArAccount_shouldReturnEmpty_whenDomAndShipperMissing() {
        ShipmentDto message = new ShipmentDto();
        message.setProductGroup("DOM");

        assertThat(shipmentArAccountService.findArAccount(message)).isEmpty();
        verify(paymentPartyResolver, never()).resolve(message);
    }

    @Test
    void findArAccount_shouldUsePaymentPartyResolver_whenNotDom() {
        ShipmentDto message = new ShipmentDto();
        message.setProductGroup("EXP");
        ARAccount arAccount = new ARAccount();

        when(paymentPartyResolver.resolve(message))
                .thenReturn(Optional.of(new ShipmentPaymentPartyResolver.PaymentParty("CONS-1", "JED")));
        when(arAccountRepository.findByAccountNumberAndEntityCode("CONS-1", "JED"))
                .thenReturn(Optional.of(arAccount));

        Optional<ARAccount> result = shipmentArAccountService.findArAccount(message);

        assertThat(result).containsSame(arAccount);
        verify(paymentPartyResolver).resolve(message);
        verify(arAccountRepository).findByAccountNumberAndEntityCode(eq("CONS-1"), eq("JED"));
    }

    @Test
    void resolveDtcShipmentType_shouldReadFromArMaster() {
        ARAccount arAccount = new ARAccount();
        arAccount.setCustomType(" DDU ");

        assertThat(shipmentArAccountService.resolveDtcShipmentType(arAccount)).contains("DDU");
    }
}
