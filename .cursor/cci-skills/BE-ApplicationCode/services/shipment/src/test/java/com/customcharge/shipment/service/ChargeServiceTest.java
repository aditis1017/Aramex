package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import com.customcharge.persistencelayer.constants.ChargeAccountTypeConstants;
import com.customcharge.persistencelayer.util.ChargeAccountTypeResolver;
import com.customcharge.shipment.service.charge.ChargeCurrencyResolver;
import com.customcharge.shipment.service.charge.ShipmentBillingEntitySupport;
import com.customcharge.shipment.service.charge.UnifiedChargeLineItemService;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContextFactory;
import com.customcharge.shipment.service.cif.ChargeCifValueSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.customcharge.shipment.constants.ShipmentConstants.CATEGORY_TYPE_EXEMPTED;
import static com.customcharge.shipment.constants.ShipmentConstants.PENDING_CALCULATION;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_BUSINESS_TYPE_B2B;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_BUSINESS_TYPE_B2C;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_DDU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargeServiceTest {

    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ShipmentBillingEntitySupport shipmentBillingEntitySupport;
    @Mock
    private UnifiedChargeLineItemService unifiedChargeLineItemService;
    @Mock
    private ChargeCurrencyResolver chargeCurrencyResolver;
    @Mock
    private ChargeCifValueSupport chargeCifValueSupport;
    @Mock
    private ShipmentProcessingContextFactory shipmentProcessingContextFactory;
    @Mock
    private ChargeAccountTypeResolver chargeAccountTypeResolver;

    @InjectMocks
    private ChargeService chargeService;

    @BeforeEach
    void setUp() {
        lenient().when(shipmentProcessingContextFactory.begin(any(), anyList())).thenReturn(null);
        lenient().doNothing().when(shipmentProcessingContextFactory).end();
        lenient().when(chargeCurrencyResolver.resolve(any(Shipment.class), anyString())).thenReturn("SAR");
        lenient().doNothing().when(shipmentBillingEntitySupport).applyToCharge(any(Charge.class), any(Shipment.class));
        lenient().doNothing().when(chargeCifValueSupport).applySh013CogSnapshot(any(Shipment.class), any(Charge.class));
        lenient().doNothing().when(unifiedChargeLineItemService)
                .createLineItems(any(), anyList(), anyString(), eq(true));
        lenient().when(chargeAccountTypeResolver.resolve(anyString(), anyString(), anyString(), any()))
                .thenReturn(Optional.of(ChargeAccountTypeConstants.ACCOUNT));
    }

    @Test
    void createCharges_shouldNoOp_whenShipmentNull() {
        chargeService.createCharges(null);

        verify(chargeRepository, never()).saveAll(anyList());
    }

    @Test
    void createCharges_shouldNoOp_whenNoChargeCodesMapped() {
        Shipment shipment = new Shipment();
        shipment.setShipmentType("UNKNOWN");

        chargeService.createCharges(shipment);

        verify(chargeRepository, never()).saveAll(anyList());
    }

    @Test
    void createCharges_shouldUseUnifiedFlowForLvShipment() {
        Shipment shipment = lvShipment("DDU");
        Charge saved = chargeWithId("ECD");
        Charge savedRevenue = chargeWithId("DTC");
        when(chargeRepository.saveAll(anyList())).thenReturn(List.of(savedRevenue, saved));

        chargeService.createCharges(shipment);

        verify(unifiedChargeLineItemService).createLineItems(eq(shipment), anyList(), eq("LV"), eq(true));
    }

    @Test
    void createCharges_shouldUseUnifiedFlowForHvCategory() {
        Shipment shipment = lvShipment("DDU");
        shipment.setCategoryType("HV");
        Charge saved = chargeWithId("ECD");
        when(chargeRepository.saveAll(anyList())).thenReturn(List.of(saved));

        chargeService.createCharges(shipment);

        verify(unifiedChargeLineItemService).createLineItems(eq(shipment), anyList(), eq("HV"), eq(true));
    }

    @Test
    void createCharges_shouldTreatBlankCategoryAsHvForRateSheet() {
        Shipment shipment = lvShipment("DDU");
        shipment.setCategoryType(null);
        shipment.setProductType(null);
        Charge saved = chargeWithId("ECD");
        when(chargeRepository.saveAll(anyList())).thenReturn(List.of(saved));

        chargeService.createCharges(shipment);

        verify(unifiedChargeLineItemService).createLineItems(eq(shipment), anyList(), eq("HV"), eq(true));
    }

    @Test
    void createCharges_shouldPersistShipmentAfterLineItems() {
        Shipment shipment = lvShipment("DDU");
        shipment.setShipmentBusinessType(SHIPMENT_BUSINESS_TYPE_B2C);
        Charge saved = chargeWithId("ECD");
        when(chargeRepository.saveAll(anyList())).thenReturn(List.of(saved));

        chargeService.createCharges(shipment);

        ArgumentCaptor<Shipment> captor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(captor.capture());
        assertThat(captor.getValue().getShipmentBusinessType()).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2C);
    }

    @Test
    void createCharges_shouldApplySh013CifSnapshotToAllCharges() {
        Shipment shipment = lvShipment("DDU");
        Charge saved = chargeWithId("ECD");
        when(chargeRepository.saveAll(anyList())).thenReturn(List.of(saved));

        chargeService.createCharges(shipment);

        verify(chargeCifValueSupport).applySh013CogSnapshot(shipment, saved);
        verify(chargeRepository, org.mockito.Mockito.times(1)).saveAll(anyList());
    }

    @Test
    void createCharges_shouldSetPendingStatusAndCurrency() {
        Shipment shipment = lvShipment("DDU");
        when(chargeRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Charge> charges = invocation.getArgument(0);
            charges.forEach(charge -> {
                assertThat(charge.getStatus()).isEqualTo(PENDING_CALCULATION);
                assertThat(charge.getCurrency()).isEqualTo("SAR");
            });
            return charges;
        });

        chargeService.createCharges(shipment);

        verify(chargeRepository, org.mockito.Mockito.times(1)).saveAll(anyList());
    }

    @Test
    void createCharges_shouldNoOp_whenCategoryExemptedAndShipmentTypeMissing() {
        Shipment shipment = new Shipment();
        shipment.setCategoryType(CATEGORY_TYPE_EXEMPTED);
        shipment.setHAWBDestinationEntity("RUH");

        chargeService.createCharges(shipment);

        verify(chargeRepository, never()).saveAll(anyList());
    }

    @Test
    void createCharges_shouldCreateCharges_whenCategoryExemptedAndShipmentTypePresent() {
        Shipment shipment = new Shipment();
        shipment.setCategoryType(CATEGORY_TYPE_EXEMPTED);
        shipment.setShipmentType("DDO");
        shipment.setHAWBDestinationEntity("RUH");
        Charge saved = chargeWithId("ECD");
        when(chargeRepository.saveAll(anyList())).thenReturn(List.of(saved));

        chargeService.createCharges(shipment);

        assertThat(shipment.getShipmentType()).isEqualTo("DDO");
        verify(chargeRepository).saveAll(anyList());
    }

    @Test
    void createCharges_shouldSetAccountTypeFromResolver() {
        Shipment shipment = lvShipment("DDU");
        shipment.setConsigneeNumber("CONS-1");
        when(chargeAccountTypeResolver.resolve("DDU", "DTC", "RUH", "CONS-1"))
                .thenReturn(Optional.of(ChargeAccountTypeConstants.CASH));
        when(chargeAccountTypeResolver.resolve("DDU", "ECD", "RUH", "CONS-1"))
                .thenReturn(Optional.of(ChargeAccountTypeConstants.ACCOUNT));
        when(chargeRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Charge> charges = invocation.getArgument(0);
            Charge ecd = charges.stream().filter(charge -> "ECD".equals(charge.getChargeCode())).findFirst().orElseThrow();
            Charge dtc = charges.stream().filter(charge -> "DTC".equals(charge.getChargeCode())).findFirst().orElseThrow();
            assertThat(ecd.getAccountType()).isEqualTo(ChargeAccountTypeConstants.ACCOUNT);
            assertThat(dtc.getAccountType()).isEqualTo(ChargeAccountTypeConstants.CASH);
            return charges;
        });

        chargeService.createCharges(shipment);

        verify(chargeAccountTypeResolver).resolve("DDU", "ECD", "RUH", "CONS-1");
        verify(chargeAccountTypeResolver).resolve("DDU", "DTC", "RUH", "CONS-1");
    }

    @Test
    void createCharges_shouldUseShipperForDomDtcAccountType() {
        Shipment shipment = lvShipment("DDU");
        shipment.setProductGroup("DOM");
        shipment.setConsigneeNumber("CONS-1");
        shipment.setShipperNumber("SHIP-1");
        when(chargeAccountTypeResolver.resolve("DDU", "DTC", "RUH", "SHIP-1"))
                .thenReturn(Optional.of(ChargeAccountTypeConstants.ACCOUNT));
        when(chargeAccountTypeResolver.resolve("DDU", "ECD", "RUH", "SHIP-1"))
                .thenReturn(Optional.of(ChargeAccountTypeConstants.ACCOUNT));
        when(chargeRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        chargeService.createCharges(shipment);

        verify(chargeAccountTypeResolver).resolve("DDU", "DTC", "RUH", "SHIP-1");
        verify(chargeAccountTypeResolver).resolve("DDU", "ECD", "RUH", "SHIP-1");
    }

    private static Shipment lvShipment(String shipmentType) {
        Shipment shipment = new Shipment();
        shipment.setShipmentType(shipmentType);
        shipment.setCategoryType("LV");
        shipment.setHAWBDestinationEntity("RUH");
        return shipment;
    }

    private static Charge chargeWithId(String code) {
        Charge charge = new Charge();
        charge.setChargeCode(code);
        return charge;
    }
}
