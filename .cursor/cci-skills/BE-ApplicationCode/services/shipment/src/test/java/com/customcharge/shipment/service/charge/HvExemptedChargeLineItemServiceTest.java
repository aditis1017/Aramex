package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.enums.RateType;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.shipment.service.charge.processing.calculation.RateAmountCalculator;
import com.customcharge.shipment.service.charge.processing.model.ResolvedRateSheet;
import com.customcharge.shipment.service.charge.processing.ratesheet.RateSheetLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.customcharge.shipment.constants.ShipmentConstants.CATEGORY_TYPE_EXEMPTED;
import static com.customcharge.shipment.constants.ShipmentConstants.CATEGORY_TYPE_HV;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HvExemptedChargeLineItemServiceTest {

    @Mock
    private ServiceChargeAccountResolver serviceChargeAccountResolver;
    @Mock
    private AccountRedisChargeDataService accountRedisChargeDataService;
    @Mock
    private ChargeLineItemRepository chargeLineItemRepository;
    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private ChargeLineItemTaxSupport chargeLineItemTaxSupport;
    @Mock
    private RateAmountCalculator rateAmountCalculator;
    @Mock
    private RateSheetLookupService rateSheetLookupService;
    @Mock
    private ShipmentChargeAmountSupport chargeAmountSupport;
    @Mock
    private ShipmentBillingEntitySupport shipmentBillingEntitySupport;

    @InjectMocks
    private HvExemptedChargeLineItemService service;

    @BeforeEach
    void stubCommonDependencies() {
        lenient().when(chargeAmountSupport.formatAmount(any(BigDecimal.class), any(Shipment.class), any(Charge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doAnswer(invocation -> {
            ChargeLineItem item = invocation.getArgument(0);
            ChargeLineItemTaxSupport.TaxedLineAmount taxed = invocation.getArgument(1);
            item.setAmount(taxed.preTaxableAmount());
            item.setPreTaxableAmount(taxed.amount());
            return null;
        }).when(chargeLineItemTaxSupport).applyToLineItem(any(), any());
        lenient().when(serviceChargeAccountResolver.resolveBillingAccount(any(), any(), any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(invocation.getArgument(3)));
    }

    @Test
    void recalculateCifAffectedLineItems_shouldNoOp_whenExempted() {
        Shipment shipment = shipment(CATEGORY_TYPE_EXEMPTED);
        Charge charge = charge("ECD");

        service.recalculateCifAffectedLineItems(shipment, charge);

        verify(rateSheetLookupService, never()).resolveForAccount(any(), any(), any());
    }

    @Test
    void recalculateCifAffectedLineItems_shouldUpdateCifLines_forHvEcd() {
        Shipment shipment = shipment(CATEGORY_TYPE_HV);
        Charge charge = charge("ECD");
        Account account = new Account();
        ChargeLineItem line = new ChargeLineItem();
        line.setServiceCode("DUTY");

        RateSheetItem cifItem = cifRateSheetItem("DUTY", "10");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem));
        ServiceCodes master = new ServiceCodes();
        master.setServiceCode("DUTY");

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(charge.getId())).thenReturn(List.of(line));
        when(accountRedisChargeDataService.getEntityServiceCodeMasters("RUH", "ECD"))
                .thenReturn(List.of(master));
        when(accountRedisChargeDataService.loadServiceChargesByMasterId(account, "RUH", "ECD"))
                .thenReturn(java.util.Map.of());
        when(rateAmountCalculator.calculateWithBasisRate(eq(cifItem), eq(shipment), eq("ECD")))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(new BigDecimal("10"), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        new BigDecimal("10"), new BigDecimal("10"), 1.0));
        ReflectionTestUtils.setField(shipment, "id", UUID.randomUUID());
        shipment.setShipmentType("DDU");
        when(chargeRepository.findByShipment_IdWithEntity(any())).thenReturn(List.of(charge));
        doAnswer(invocation -> {
            Charge savedCharge = invocation.getArgument(1);
            savedCharge.setEstimateAmount(new BigDecimal("10"));
            return null;
        }).when(chargeAmountSupport).applyChargeHeaderAmounts(any(), any(), any());

        service.recalculateCifAffectedLineItems(shipment, charge);

        verify(chargeLineItemRepository).saveAll(List.of(line));
        verify(chargeRepository).saveAll(List.of(charge));
    }

    @Test
    void recalculateCifAffectedLineItems_shouldNoOp_whenInputsMissing() {
        service.recalculateCifAffectedLineItems(null, new Charge());
        service.recalculateCifAffectedLineItems(new Shipment(), null);
    }

    @Test
    void recalculateCifAffectedLineItems_shouldNoOp_whenEntityMissing() {
        Shipment shipment = shipment(CATEGORY_TYPE_HV);
        Charge charge = charge("ECD");

        service.recalculateCifAffectedLineItems(shipment, charge);

        verify(rateSheetLookupService, never()).resolveForAccount(any(), any(), any());
    }

    @Test
    void recalculateCifAffectedLineItems_shouldUseShipmentChargeResolver_forRevenueCharge() {
        Shipment shipment = shipment(CATEGORY_TYPE_HV);
        shipment.setOriginEntity("JED");
        Charge charge = charge("DTC");
        Account account = new Account();
        ChargeLineItem line = new ChargeLineItem();
        line.setServiceCode("DUTY");
        RateSheetItem cifItem = cifRateSheetItem("DUTY", "10");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem));

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTC")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForShipmentCharge(shipment, "DTC", CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(charge.getId())).thenReturn(List.of(line));
        when(accountRedisChargeDataService.getEntityServiceCodeMasters("RUH", "DTC"))
                .thenReturn(List.of());
        when(accountRedisChargeDataService.loadServiceChargesByMasterId(account, "RUH", "DTC"))
                .thenReturn(java.util.Map.of());
        when(rateAmountCalculator.calculateWithBasisRate(eq(cifItem), eq(shipment), eq("DTC")))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(new BigDecimal("10"), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        new BigDecimal("10"), new BigDecimal("10"), 1.0));
        ReflectionTestUtils.setField(shipment, "id", UUID.randomUUID());
        when(chargeRepository.findByShipment_IdWithEntity(any())).thenReturn(List.of(charge));

        service.recalculateCifAffectedLineItems(shipment, charge);

        verify(chargeLineItemRepository).saveAll(List.of(line));
    }

    @Test
    void recalculateCifAffectedLineItems_shouldSaveChargeDirectly_whenShipmentIdMissing() {
        Shipment shipment = shipment(CATEGORY_TYPE_HV);
        Charge charge = charge("ECD");
        Account account = new Account();
        ChargeLineItem line = new ChargeLineItem();
        line.setServiceCode("DUTY");
        RateSheetItem cifItem = cifRateSheetItem("DUTY", "10");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem));

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(charge.getId())).thenReturn(List.of(line));
        when(accountRedisChargeDataService.getEntityServiceCodeMasters(eq("RUH"), eq("ECD")))
                .thenReturn(List.of());
        when(accountRedisChargeDataService.loadServiceChargesByMasterId(eq(account), eq("RUH"), eq("ECD")))
                .thenReturn(java.util.Map.of());
        when(rateAmountCalculator.calculateWithBasisRate(eq(cifItem), eq(shipment), eq("ECD")))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(new BigDecimal("10"), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        new BigDecimal("10"), new BigDecimal("10"), 1.0));

        service.recalculateCifAffectedLineItems(shipment, charge);

        verify(chargeRepository).save(charge);
    }

    @Test
    void recalculateCifAffectedLineItems_shouldNoOp_whenAccountMissing() {
        Shipment shipment = shipment(CATEGORY_TYPE_HV);
        Charge charge = charge("ECD");
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.empty());

        service.recalculateCifAffectedLineItems(shipment, charge);

        verify(rateSheetLookupService, never()).resolveForAccount(any(), any(), any());
    }

    @Test
    void recalculateCifAffectedLineItems_shouldNoOp_whenBillingAccountMissing() {
        Shipment shipment = shipment(CATEGORY_TYPE_HV);
        Charge charge = charge("ECD");
        Account account = new Account();
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(serviceChargeAccountResolver.resolveBillingAccount(shipment, "RUH", "ECD", account))
                .thenReturn(Optional.empty());

        service.recalculateCifAffectedLineItems(shipment, charge);

        verify(rateSheetLookupService, never()).resolveForAccount(any(), any(), any());
    }

    @Test
    void recalculateCifAffectedLineItems_shouldNoOp_whenNoCifRateSheet() {
        Shipment shipment = shipment(CATEGORY_TYPE_HV);
        Charge charge = charge("ECD");
        Account account = new Account();
        RateSheetItem nonCifItem = cifRateSheetItem("DUTY", "10");
        nonCifItem.getRates().get(0).setPercentageOf(null);
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(nonCifItem));

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));

        service.recalculateCifAffectedLineItems(shipment, charge);

        verify(chargeLineItemRepository, never()).saveAll(any());
    }

    @Test
    void recalculateCifAffectedLineItems_shouldNoOp_whenNoLineItems() {
        Shipment shipment = shipment(CATEGORY_TYPE_HV);
        Charge charge = charge("ECD");
        Account account = new Account();
        RateSheetItem cifItem = cifRateSheetItem("DUTY", "10");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem));

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(charge.getId())).thenReturn(List.of());

        service.recalculateCifAffectedLineItems(shipment, charge);

        verify(chargeLineItemRepository, never()).saveAll(any());
    }

    @Test
    void recalculateCifAffectedLineItems_shouldSkipNonCifRateItems() {
        Shipment shipment = shipment(CATEGORY_TYPE_HV);
        Charge charge = charge("ECD");
        Account account = new Account();
        ChargeLineItem line = new ChargeLineItem();
        line.setServiceCode("TAX");
        RateSheetItem cifItem = cifRateSheetItem("DUTY", "10");
        RateSheetItem nonCifItem = cifRateSheetItem("TAX", "5");
        nonCifItem.getRates().get(0).setPercentageOf(null);
        ResolvedRateSheet resolved = new ResolvedRateSheet(
                account, new RateSheet(), List.of(cifItem, nonCifItem));

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(charge.getId())).thenReturn(List.of(line));
        when(accountRedisChargeDataService.getEntityServiceCodeMasters(eq("RUH"), eq("ECD")))
                .thenReturn(List.of());
        when(accountRedisChargeDataService.loadServiceChargesByMasterId(eq(account), eq("RUH"), eq("ECD")))
                .thenReturn(java.util.Map.of());

        service.recalculateCifAffectedLineItems(shipment, charge);

        verify(chargeLineItemRepository, never()).saveAll(any());
    }

    @Test
    void recalculateCifAffectedLineItems_shouldUseEmptyShipmentCharges_whenRepositoryReturnsEmpty() {
        Shipment shipment = shipment(CATEGORY_TYPE_HV);
        ReflectionTestUtils.setField(shipment, "id", UUID.randomUUID());
        Charge charge = charge("ECD");
        Account account = new Account();
        ChargeLineItem line = new ChargeLineItem();
        line.setServiceCode("DUTY");
        RateSheetItem cifItem = cifRateSheetItem("DUTY", "10");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem));

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(charge.getId())).thenReturn(List.of(line));
        when(accountRedisChargeDataService.getEntityServiceCodeMasters(eq("RUH"), eq("ECD")))
                .thenReturn(List.of());
        when(accountRedisChargeDataService.loadServiceChargesByMasterId(eq(account), eq("RUH"), eq("ECD")))
                .thenReturn(java.util.Map.of());
        when(rateAmountCalculator.calculateWithBasisRate(eq(cifItem), eq(shipment), eq("ECD")))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(new BigDecimal("10"), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        new BigDecimal("10"), new BigDecimal("10"), 1.0));
        when(chargeRepository.findByShipment_IdWithEntity(any())).thenReturn(List.of());

        service.recalculateCifAffectedLineItems(shipment, charge);

        verify(chargeRepository).saveAll(List.of(charge));
    }

    @Test
    void recalculateCifAffectedLineItems_shouldNoOp_whenOriginEntityMissing() {
        Shipment shipment = shipment(CATEGORY_TYPE_HV);
        Charge charge = charge("ECS");

        service.recalculateCifAffectedLineItems(shipment, charge);

        verify(rateSheetLookupService, never()).resolveForAccount(any(), any(), any());
    }

    @Test
    void recalculateCifAffectedLineItems_shouldResolveMasterFromAccountCharge() {
        Shipment shipment = shipment(CATEGORY_TYPE_HV);
        ReflectionTestUtils.setField(shipment, "id", UUID.randomUUID());
        Charge charge = charge("ECD");
        Account account = new Account();
        ChargeLineItem line = new ChargeLineItem();
        line.setServiceCode("DUTY");
        RateSheetItem cifItem = cifRateSheetItem("DUTY", "10");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem));
        ServiceCodes master = new ServiceCodes();
        master.setServiceCode("DUTY");
        UUID masterId = UUID.randomUUID();
        ReflectionTestUtils.setField(master, "id", masterId);
        ServiceCharge accountCharge = new ServiceCharge();
        accountCharge.setServiceCodes(master);

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(charge.getId())).thenReturn(List.of(line));
        when(accountRedisChargeDataService.getEntityServiceCodeMasters("RUH", "ECD"))
                .thenReturn(List.of(master));
        when(accountRedisChargeDataService.loadServiceChargesByMasterId(account, "RUH", "ECD"))
                .thenReturn(java.util.Map.of(masterId, accountCharge));
        when(rateAmountCalculator.calculateWithBasisRate(eq(cifItem), eq(shipment), eq("ECD")))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(new BigDecimal("10"), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        new BigDecimal("10"), new BigDecimal("10"), 1.0));
        when(chargeRepository.findByShipment_IdWithEntity(any())).thenReturn(List.of(charge));

        service.recalculateCifAffectedLineItems(shipment, charge);

        verify(chargeLineItemRepository).saveAll(List.of(line));
    }

    private static Shipment shipment(String categoryType) {
        Shipment shipment = new Shipment();
        shipment.setCategoryType(categoryType);
        shipment.setHAWBDestinationEntity("RUH");
        return shipment;
    }

    private static Charge charge(String code) {
        Charge charge = new Charge();
        ReflectionTestUtils.setField(charge, "id", UUID.randomUUID());
        charge.setChargeCode(code);
        return charge;
    }

    private static RateSheetItem cifRateSheetItem(String serviceCode, String cifPercent) {
        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateType(RateType.PERCENTAGE);
        rate.setPercentageOf(PercentageOf.CIF_VALUE);
        rate.setValue(new BigDecimal(cifPercent));
        RateSheetItem item = new RateSheetItem();
        item.setServiceCode(serviceCode);
        item.setShipmentType("DDU");
        item.setRates(List.of(rate));
        return item;
    }
}
