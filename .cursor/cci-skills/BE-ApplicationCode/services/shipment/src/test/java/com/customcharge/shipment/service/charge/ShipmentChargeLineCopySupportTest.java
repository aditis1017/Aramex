package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContextFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentChargeLineCopySupportTest {

    @Mock
    private ChargeLineCopyService chargeLineCopyService;
    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private ChargeLineItemRepository chargeLineItemRepository;
    @Mock
    private com.customcharge.persistencelayer.repository.CurrencyRepository currencyRepository;
    @Mock
    private com.customcharge.persistencelayer.repository.EntityRepository entityRepository;
    @Mock
    private com.customcharge.persistencelayer.repository.ServiceCodesRepository serviceCodesRepository;
    @Mock
    private com.customcharge.persistencelayer.repository.ServiceChargeRepository serviceChargeRepository;

    private ShipmentChargeLineCopySupport support;
    private ShipmentProcessingContextFactory contextFactory;

    @BeforeEach
    void setUp() {
        support = new ShipmentChargeLineCopySupport(
                chargeLineCopyService, chargeRepository, chargeLineItemRepository);
        contextFactory = new ShipmentProcessingContextFactory(
                currencyRepository, entityRepository, serviceCodesRepository, serviceChargeRepository, null);
    }

    @AfterEach
    void tearDown() {
        contextFactory.end();
    }

    @Test
    void isCopyDerived_shouldRecognizeCopyTargets() {
        assertThat(support.isCopyDerived("DTC")).isTrue();
        assertThat(support.isCopyDerived(" dta ")).isTrue();
        assertThat(support.isCopyDerived("ECD")).isFalse();
        assertThat(support.isCopyDerived(null)).isFalse();
    }

    @Test
    void copyToTarget_shouldThrow_whenUnsupportedChargeType() {
        Shipment shipment = new Shipment();
        Charge target = new Charge();

        assertThatThrownBy(() -> support.copyToTarget(shipment, "ECD", "RUH", target, List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No copy pair");
    }

    @Test
    void copyToTarget_shouldUseRepository_whenNoProcessingContext() {
        Shipment shipment = shipment();
        Charge source = charge("ECD");
        Charge target = charge("DTC");
        ChargeLineItem copied = lineItem("DUTY");
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), "ECD"))
                .thenReturn(Optional.of(source));
        when(chargeLineItemRepository.findByCharge_Id(source.getId())).thenReturn(List.of(copied));
        when(chargeLineCopyService.copy(any())).thenReturn(List.of(copied));

        List<ChargeLineItem> result = support.copyToTarget(
                shipment, "DTC", "RUH", target, List.of(new Account()));

        assertThat(result).containsExactly(copied);
        ArgumentCaptor<ChargeLineCopyService.ChargeLineCopyRequest> captor =
                ArgumentCaptor.forClass(ChargeLineCopyService.ChargeLineCopyRequest.class);
        verify(chargeLineCopyService).copy(captor.capture());
        assertThat(captor.getValue().sourceChargeType()).isEqualTo("ECD");
        assertThat(captor.getValue().targetChargeType()).isEqualTo("DTC");
    }

    @Test
    void copyToTarget_shouldUseProcessingContext_whenBound() {
        Shipment shipment = shipment();
        Charge source = charge("ECD");
        Charge target = charge("DTC");
        ChargeLineItem copied = lineItem("DUTY");
        contextFactory.begin(shipment, List.of("ECD", "DTC"));
        shipment.getClass();
        com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext.get()
                .registerCharge("ECD", source);
        com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext.get()
                .registerLineItems("ECD", List.of(copied));
        when(chargeLineCopyService.copy(any())).thenReturn(List.of(copied));

        List<ChargeLineItem> result = support.copyToTarget(
                shipment, "DTC", "RUH", target, List.of(new Account()));

        assertThat(result).containsExactly(copied);
    }

    @Test
    void copyToTarget_shouldUseThirdPartyEntity_forEc3Source() {
        Shipment shipment = shipment();
        shipment.setOriginEntity("DXB");
        shipment.setThirdPartyEntity("JED");
        Charge source = charge("EC3");
        Charge target = charge("DT3");
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), "EC3"))
                .thenReturn(Optional.of(source));
        when(chargeLineItemRepository.findByCharge_Id(source.getId())).thenReturn(List.of());
        when(chargeLineCopyService.copy(any())).thenReturn(List.of());

        support.copyToTarget(shipment, "DT3", "JED", target, List.of(new Account()));

        ArgumentCaptor<ChargeLineCopyService.ChargeLineCopyRequest> captor =
                ArgumentCaptor.forClass(ChargeLineCopyService.ChargeLineCopyRequest.class);
        verify(chargeLineCopyService).copy(captor.capture());
        assertThat(captor.getValue().sourceEntityCode()).isEqualTo("JED");
    }

    private static Shipment shipment() {
        Shipment shipment = new Shipment();
        ReflectionTestUtils.setField(shipment, "id", UUID.randomUUID());
        shipment.setHAWBDestinationEntity("RUH");
        return shipment;
    }

    private static Charge charge(String code) {
        Charge charge = new Charge();
        ReflectionTestUtils.setField(charge, "id", UUID.randomUUID());
        charge.setChargeCode(code);
        return charge;
    }

    private static ChargeLineItem lineItem(String serviceCode) {
        ChargeLineItem line = new ChargeLineItem();
        line.setServiceCode(serviceCode);
        return line;
    }
}
