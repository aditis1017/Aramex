package com.customcharge.shipment.service.cif;

import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import com.customcharge.shipment.config.ShipmentProperties;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.InOrder;

@ExtendWith(MockitoExtension.class)
class ShipmentCifUpdateServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ShipmentCifChargeRecalculationService cifChargeRecalculationService;
    @Mock
    private ObjectProvider<ExchangeRateService> exchangeRateService;
    @Mock
    private ExchangeRateService exchangeRate;
    @Mock
    private ShipmentProperties shipmentProperties;

    @InjectMocks
    private ShipmentCifUpdateService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(shipmentProperties.getMonetaryScale()).thenReturn(4);
    }

    @Test
    void handle_shouldSkipWhenComment1Unparseable() {
        ShipmentDto message = new ShipmentDto();
        message.setHawb("HAWB-1");
        message.setComment1("invalid");

        service.handle(message);

        verify(shipmentRepository, never()).findByHawbNumber(any());
        verify(cifChargeRecalculationService, never()).recalculate(any(), any());
    }

    @Test
    void handle_shouldSkipWhenShipmentMissing() {
        ShipmentDto message = new ShipmentDto();
        message.setHawb("HAWB-2");
        message.setComment1("10 TND");
        when(shipmentRepository.findByHawbNumber("HAWB-2")).thenReturn(Optional.empty());

        service.handle(message);

        verify(shipmentRepository, never()).save(any());
        verify(cifChargeRecalculationService, never()).recalculate(any(), any());
    }

    @Test
    void handle_shouldSaveAndRecalculateWhenAmountZero() {
        ShipmentDto message = new ShipmentDto();
        message.setHawb("HAWB-3");
        message.setComment1("0 TND");
        Shipment shipment = new Shipment();
        shipment.setHawbNumber("HAWB-3");
        shipment.setCustomsCurrency("TND");
        when(shipmentRepository.findByHawbNumber("HAWB-3")).thenReturn(Optional.of(shipment));

        service.handle(message);

        assertThat(shipment.getCifValue()).isEqualTo(0.0);
        assertThat(shipment.getCifValueCurrency()).isEqualTo("TND");
        verify(shipmentRepository).save(shipment);
        verify(cifChargeRecalculationService).recalculate(shipment, new HawbComment2CifParser.ParsedCif(0.0, "TND"));
    }

    @Test
    void handle_shouldSaveAndRecalculateFromComment1Format() {
        ShipmentDto message = new ShipmentDto();
        message.setHawb("HAWB-7");
        message.setComment1("14 JOD");
        Shipment shipment = new Shipment();
        shipment.setHawbNumber("HAWB-7");
        shipment.setCustomsCurrency("JOD");
        when(shipmentRepository.findByHawbNumber("HAWB-7")).thenReturn(Optional.of(shipment));

        service.handle(message);

        assertThat(shipment.getCifValue()).isEqualTo(14.0);
        assertThat(shipment.getCifValueCurrency()).isEqualTo("JOD");
        verify(shipmentRepository).save(shipment);
        verify(cifChargeRecalculationService).recalculate(shipment, new HawbComment2CifParser.ParsedCif(14.0, "JOD"));
        verify(exchangeRateService, never()).getIfAvailable();
    }

    @Test
    void handle_shouldSaveAndRecalculateWhenAmountPositive() {
        ShipmentDto message = new ShipmentDto();
        message.setHawb("HAWB-4");
        message.setComment1("15 TND");
        Shipment shipment = new Shipment();
        shipment.setHawbNumber("HAWB-4");
        shipment.setCustomsCurrency("TND");
        when(shipmentRepository.findByHawbNumber("HAWB-4")).thenReturn(Optional.of(shipment));

        service.handle(message);

        assertThat(shipment.getCifValue()).isEqualTo(15.0);
        assertThat(shipment.getCifValueCurrency()).isEqualTo("TND");
        verify(shipmentRepository).save(shipment);
        verify(cifChargeRecalculationService).recalculate(shipment, new HawbComment2CifParser.ParsedCif(15.0, "TND"));
        verify(exchangeRateService, never()).getIfAvailable();
    }

    @Test
    void handle_shouldConvertCifToCostOfGoodsCurrencyWhenDifferent() {
        ShipmentDto message = new ShipmentDto();
        message.setHawb("HAWB-6");
        message.setComment1("10 USD");
        Shipment shipment = new Shipment();
        shipment.setHawbNumber("HAWB-6");
        shipment.setCustomsCurrency("TND");
        when(shipmentRepository.findByHawbNumber("HAWB-6")).thenReturn(Optional.of(shipment));
        when(exchangeRateService.getIfAvailable()).thenReturn(exchangeRate);
        when(exchangeRate.convertAmount(10.0, "USD", "TND"))
                .thenReturn(new CurrencyConversionResult(31.5, 3.15));
        doAnswer(invocation -> {
            Shipment shipmentForCalculation = invocation.getArgument(0);
            HawbComment2CifParser.ParsedCif originalCif = invocation.getArgument(1);
            assertThat(shipmentForCalculation.getCifValue()).isEqualTo(31.5);
            assertThat(shipmentForCalculation.getCifValueCurrency()).isEqualTo("TND");
            assertThat(originalCif.amount()).isEqualTo(10.0);
            assertThat(originalCif.currency()).isEqualTo("USD");
            return null;
        }).when(cifChargeRecalculationService).recalculate(eq(shipment), eq(new HawbComment2CifParser.ParsedCif(10.0, "USD")));
        InOrder inOrder = inOrder(cifChargeRecalculationService, shipmentRepository);

        service.handle(message);

        assertThat(shipment.getCifValue()).isEqualTo(10.0);
        assertThat(shipment.getCifValueCurrency()).isEqualTo("USD");
        verify(exchangeRate).convertAmount(eq(10.0), eq("USD"), eq("TND"));
        inOrder.verify(cifChargeRecalculationService).recalculate(shipment, new HawbComment2CifParser.ParsedCif(10.0, "USD"));
        inOrder.verify(shipmentRepository).save(shipment);
    }

    @Test
    void handle_shouldSkipWhenShipmentAlreadyExempted() {
        ShipmentDto message = new ShipmentDto();
        message.setHawb("HAWB-EX");
        message.setComment1("10 TND");
        Shipment shipment = new Shipment();
        shipment.setHawbNumber("HAWB-EX");
        shipment.setCategoryType("Exempted");
        when(shipmentRepository.findByHawbNumber("HAWB-EX")).thenReturn(Optional.of(shipment));

        service.handle(message);

        verify(shipmentRepository, never()).save(any());
        verify(cifChargeRecalculationService, never()).recalculate(any(), any());
    }

    @Test
    void handle_shouldSaveWithoutRecalcWhenAmountNegative() {
        ShipmentDto message = new ShipmentDto();
        message.setHawb("HAWB-5");
        message.setComment1("-5 TND");
        Shipment shipment = new Shipment();
        shipment.setHawbNumber("HAWB-5");
        when(shipmentRepository.findByHawbNumber("HAWB-5")).thenReturn(Optional.of(shipment));

        service.handle(message);

        verify(shipmentRepository).save(shipment);
        verify(cifChargeRecalculationService, never()).recalculate(any(), any());
    }
}
