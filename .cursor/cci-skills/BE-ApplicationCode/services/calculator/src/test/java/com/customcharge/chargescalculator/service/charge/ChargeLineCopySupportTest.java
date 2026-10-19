package com.customcharge.chargescalculator.service.charge;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.service.batch.RecordProcessingLookups;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargeLineCopySupportTest {

    @Mock
    private ChargeLineCopyService chargeLineCopyService;
    @Mock
    private RecordProcessingLookups recordProcessingLookups;
    @Mock
    private ChargeLineSupport chargeLineSupport;
    @Mock
    private ServiceChargeMappingRepository serviceChargeMappingRepository;

    @InjectMocks
    private ChargeLineCopySupport support;

    @Test
    void isCopyDerivedCharge_shouldRecognizeConfiguredPairs() {
        assertThat(support.isCopyDerivedCharge("DTC")).isTrue();
        assertThat(support.isCopyDerivedCharge("ecd")).isFalse();
        assertThat(support.isCopyDerivedCharge(null)).isFalse();
    }

    @Test
    void copyToTargetCharge_shouldDelegateToCopyService() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        ChargeCalculationContext context = CalculatorTestDataFactory.chargeContext(shipment);
        Account account = CalculatorTestDataFactory.account(UUID.randomUUID(), "DTC");
        Charge targetCharge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        Charge sourceCharge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "ECD");
        ChargeLineItem sourceLine = CalculatorTestDataFactory.chargeLineItem(
                sourceCharge, account, "DUTY", java.math.BigDecimal.TEN);
        ChargeLineItem copied = CalculatorTestDataFactory.chargeLineItem(
                targetCharge, account, "DUTY", java.math.BigDecimal.TEN);
        UUID fileRef = UUID.randomUUID();

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(recordProcessingLookups.findCharge(shipment.getId(), "ECD"))
                .thenReturn(Optional.of(sourceCharge));
        when(recordProcessingLookups.findLineItems(sourceCharge.getId())).thenReturn(List.of(sourceLine));
        when(chargeLineCopyService.copy(any())).thenReturn(List.of(copied));

        List<ChargeLineItem> result = support.copyToTargetCharge(
                context,
                shipment,
                BatchProcessingConstants.CHARGE_CODE_DTC,
                "DXB",
                targetCharge,
                List.of(account),
                fileRef,
                "PAY-1");

        assertThat(result).containsExactly(copied);
        ArgumentCaptor<ChargeLineCopyService.ChargeLineCopyRequest> captor =
                ArgumentCaptor.forClass(ChargeLineCopyService.ChargeLineCopyRequest.class);
        verify(chargeLineCopyService).copy(captor.capture());
        assertThat(captor.getValue().paymentReference()).isEqualTo("PAY-1");
    }

    @Test
    void copyToTargetCharge_shouldUseOriginEntityForEcs() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        ChargeCalculationContext context = CalculatorTestDataFactory.chargeContext(shipment);
        Account account = CalculatorTestDataFactory.account(UUID.randomUUID(), "ECS");
        Charge targetCharge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "ECS");
        Charge sourceCharge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTA");

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(recordProcessingLookups.findCharge(shipment.getId(), "DTA"))
                .thenReturn(Optional.of(sourceCharge));
        when(recordProcessingLookups.findLineItems(sourceCharge.getId())).thenReturn(List.of());
        when(chargeLineCopyService.copy(any())).thenReturn(List.of());

        support.copyToTargetCharge(
                context,
                shipment,
                BatchProcessingConstants.CHARGE_CODE_ECS,
                "RUH",
                targetCharge,
                List.of(account),
                null,
                null);

        verify(chargeLineSupport).resolveDestinationEntityCode(shipment);
    }

    @Test
    void copyToTargetCharge_shouldThrowWhenSourceChargeMissing() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        ChargeCalculationContext context = CalculatorTestDataFactory.chargeContext(shipment);
        Charge targetCharge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(recordProcessingLookups.findCharge(shipment.getId(), "ECD"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> support.copyToTargetCharge(
                context,
                shipment,
                BatchProcessingConstants.CHARGE_CODE_DTC,
                "DXB",
                targetCharge,
                List.of(),
                null,
                null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Source charge not found");
    }
}
