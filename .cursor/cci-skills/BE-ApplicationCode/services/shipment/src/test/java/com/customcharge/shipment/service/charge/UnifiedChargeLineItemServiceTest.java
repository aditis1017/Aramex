package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.dto.ServiceChargeQueryCriteria;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.enums.RateType;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.persistencelayer.util.ServiceChargeResolver;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContextFactory;
import com.customcharge.shipment.service.charge.processing.calculation.RateAmountCalculator;
import com.customcharge.shipment.service.charge.processing.model.ResolvedRateSheet;
import com.customcharge.shipment.service.charge.processing.ratesheet.RateSheetLookupService;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnifiedChargeLineItemServiceTest {

    @Mock
    private ServiceChargeAccountResolver serviceChargeAccountResolver;
    @Mock
    private ServiceChargeResolver serviceChargeResolver;
    @Mock
    private ServiceCodesRepository serviceCodesRepository;
    @Mock
    private RateSheetLookupService rateSheetLookupService;
    @Mock
    private RateAmountCalculator rateAmountCalculator;
    @Mock
    private ChargeLineItemTaxSupport chargeLineItemTaxSupport;
    @Mock
    private ShipmentChargeAmountSupport chargeAmountSupport;
    @Mock
    private ShipmentBillingEntitySupport shipmentBillingEntitySupport;
    @Mock
    private ShipmentChargeLineCopySupport chargeLineCopySupport;
    @Mock
    private ChargeLineItemRepository chargeLineItemRepository;
    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private ServiceChargeRepository serviceChargeRepository;
    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private UnifiedChargeLineItemService unifiedChargeLineItemService;

    private ShipmentProcessingContextFactory shipmentProcessingContextFactory;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        shipmentProcessingContextFactory = new ShipmentProcessingContextFactory(
                currencyRepository,
                entityRepository,
                serviceCodesRepository,
                serviceChargeRepository,
                null);
        ReflectionTestUtils.setField(
                unifiedChargeLineItemService, "shipmentProcessingContextFactory", shipmentProcessingContextFactory);

        lenient().doAnswer(invocation -> {
            ChargeLineItem item = invocation.getArgument(0);
            ChargeLineItemTaxSupport.TaxedLineAmount taxed = invocation.getArgument(1);
            item.setAmount(taxed.preTaxableAmount());
            item.setPreTaxableAmount(taxed.amount());
            item.setExchangeRate(taxed.exchangeRate());
            return null;
        }).when(chargeLineItemTaxSupport).applyToLineItem(any(), any());
        lenient().when(serviceChargeAccountResolver.resolveBillingAccount(any(), any(), any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(invocation.getArgument(3)));
    }

    @Test
    void recalculateLineItems_shouldSkipPaymentTypeIneligibleCharges() {
        Shipment shipment = shipment("3");
        Charge ecs = chargeWithId("ECS");

        unifiedChargeLineItemService.recalculateLineItems(shipment, List.of(ecs), "LV");

        verify(chargeLineItemRepository, never()).saveAll(any());
        verify(chargeRepository).saveAll(List.of(ecs));
    }

    @Test
    void createLineItems_shouldNoOp_whenInputsMissing() {
        unifiedChargeLineItemService.createLineItems(null, List.of(), "LV");
        unifiedChargeLineItemService.createLineItems(shipment("P"), null, "LV");
        unifiedChargeLineItemService.createLineItems(shipment("P"), List.of(), "LV");

        verify(chargeLineItemRepository, never()).saveAll(any());
    }

    @Test
    void createLineItems_shouldCreateResolverLines() {
        Shipment shipment = shipment("P");
        Charge ecd = chargeWithId("ECD");
        Account account = new Account();
        ServiceChargeResponse serviceCharge = new ServiceChargeResponse(
                null, "DUTY", "Duty", null, null, true, null, null, true, null, null, null, null, null, null, null);
        RateSheetItem rateItem = cifRateSheetItem("DUTY");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(rateItem));

        lenient().when(chargeLineCopySupport.isCopyDerived("ECD")).thenReturn(false);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(serviceChargeResolver.resolve(any(ServiceChargeQueryCriteria.class)))
                .thenReturn(List.of(serviceCharge));
        when(rateSheetLookupService.resolveForAccount(account, shipment, "LV")).thenReturn(Optional.of(resolved));
        when(rateAmountCalculator.calculateWithBasisRate(rateItem, shipment, "ECD"))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(BigDecimal.TEN, 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.TEN, BigDecimal.TEN, 1.0));

        unifiedChargeLineItemService.createLineItems(shipment, List.of(ecd), "LV");

        verify(chargeLineItemRepository).saveAll(anyList());
        verify(chargeRepository).saveAll(List.of(ecd));
    }

    @Test
    void createLineItems_shouldSkip_whenBillingAccountMissing() {
        Shipment shipment = shipment("P");
        Charge ecd = chargeWithId("ECD");
        Account account = new Account();
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(serviceChargeAccountResolver.resolveBillingAccount(shipment, "RUH", "ECD", account))
                .thenReturn(Optional.empty());

        unifiedChargeLineItemService.createLineItems(shipment, List.of(ecd), "LV");

        verify(chargeLineItemRepository, never()).saveAll(any());
        verify(chargeRepository).saveAll(List.of(ecd));
    }

    @Test
    void recalculateExemptedLineItems_shouldZeroExistingLines() {
        Shipment shipment = shipment("P");
        shipment.setCategoryType(CATEGORY_TYPE_EXEMPTED);
        Charge ecd = chargeWithId("ECD");
        ChargeLineItem line = lineItem("DUTY", "100");
        when(chargeLineItemRepository.findByCharge_Id(ecd.getId())).thenReturn(List.of(line));

        unifiedChargeLineItemService.recalculateExemptedLineItems(shipment, List.of(ecd));

        assertThat(line.getAmount()).isEqualByComparingTo("0");
        verify(chargeLineItemRepository).saveAll(List.of(line));
    }

    @Test
    void reapplyFullRateSheetAfterCategoryChange_shouldApplyRatesToResolverLines() {
        Shipment shipment = shipment("P");
        shipment.setCategoryType(CATEGORY_TYPE_HV);
        Charge ecd = chargeWithId("ECD");
        ChargeLineItem line = lineItem("DUTY", "0");
        Account account = new Account();
        RateSheetItem rateItem = cifRateSheetItem("DUTY");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(rateItem));

        lenient().when(chargeLineCopySupport.isCopyDerived("ECD")).thenReturn(false);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(ecd.getId())).thenReturn(List.of(line));
        when(rateAmountCalculator.calculateWithBasisRate(rateItem, shipment, "ECD"))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(BigDecimal.valueOf(200), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(200), BigDecimal.valueOf(200), 1.0));

        unifiedChargeLineItemService.reapplyFullRateSheetAfterCategoryChange(
                shipment, List.of(ecd), CATEGORY_TYPE_HV);

        verify(chargeLineItemRepository).saveAll(List.of(line));
    }

    @Test
    void recalculateActualizedCifOnlyLineItems_shouldUpdateEstimateLinesOnly() {
        Shipment shipment = shipment("P");
        Charge ecd = chargeWithId("ECD");
        ChargeLineItem estimate = lineItem("DUTY", "50");
        ChargeLineItem actual = lineItem("DUTY", "80");
        actual.setFileReferenceId(UUID.randomUUID());
        Account account = new Account();
        RateSheetItem cifItem = cifRateSheetItem("DUTY");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem));

        lenient().when(chargeLineCopySupport.isCopyDerived("ECD")).thenReturn(false);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, "LV")).thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(ecd.getId())).thenReturn(List.of(estimate, actual));
        when(rateAmountCalculator.calculateWithBasisRate(cifItem, shipment, "ECD"))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(BigDecimal.valueOf(120), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(120), BigDecimal.valueOf(120), 1.0));

        unifiedChargeLineItemService.recalculateActualizedCifOnlyLineItems(shipment, List.of(ecd), "LV");

        verify(chargeLineItemRepository).saveAll(List.of(estimate, actual));
    }

    @Test
    void computeChargeTotal_shouldDelegateToHeaderSupport() {
        ChargeLineItem line = lineItem("DUTY", "25");

        BigDecimal total = UnifiedChargeLineItemService.computeChargeTotal("ECD", List.of(line));

        assertThat(total).isNotNull();
    }

    @Test
    void createLineItems_shouldUseCopyDerivedPath() {
        Shipment shipment = shipment("P");
        Charge dtc = chargeWithId("DTC");
        Account account = new Account();
        ChargeLineItem copied = lineItem("DUTY", "75");
        RateSheetItem rateItem = cifRateSheetItem("DUTY");

        when(chargeLineCopySupport.isCopyDerived("DTC")).thenReturn(true);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTC")).thenReturn(Optional.of(account));
        when(chargeLineCopySupport.copyToTarget(shipment, "DTC", "RUH", dtc, List.of(account)))
                .thenReturn(List.of(copied));
        when(rateSheetLookupService.resolveForShipmentCharge(shipment, "DTC", "LV"))
                .thenReturn(Optional.of(new ResolvedRateSheet(account, new RateSheet(), List.of(rateItem))));
        when(rateAmountCalculator.calculate(rateItem, shipment, "DTC")).thenReturn(BigDecimal.TEN);
        when(chargeLineItemTaxSupport.computeContractedRate(any(), any(), any())).thenReturn(BigDecimal.ONE);

        unifiedChargeLineItemService.createLineItems(shipment, List.of(dtc), "LV");

        verify(chargeLineItemRepository).saveAll(anyList());
    }

    @Test
    void recalculateLineItems_shouldUpdateCifAffectedEcdLines() {
        Shipment shipment = shipment("P");
        Charge ecd = chargeWithId("ECD");
        ChargeLineItem dutyLine = lineItem("DUTY", "100");
        Account account = new Account();

        RateSheetItem cifItem = cifRateSheetItem("DUTY");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem));

        lenient().when(chargeLineCopySupport.isCopyDerived("ECD")).thenReturn(false);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, "LV")).thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(ecd.getId())).thenReturn(List.of(dutyLine));
        when(rateAmountCalculator.calculateWithBasisRate(cifItem, shipment, "ECD"))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(BigDecimal.valueOf(150), 4.7));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                eq(BigDecimal.valueOf(150)), eq(shipment), eq(ecd), eq(account), eq("RUH"), eq("DUTY"),
                eq(null), eq(false), eq(4.7)))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(150), BigDecimal.valueOf(172.5), 4.7));

        unifiedChargeLineItemService.recalculateLineItems(shipment, List.of(ecd), "LV");

        verify(chargeLineItemRepository).saveAll(List.of(dutyLine));
        verify(chargeRepository).saveAll(List.of(ecd));
    }

    @Test
    void recalculateLineItems_shouldRefreshCopyDerivedDtcAfterEcdUpdate() {
        Shipment shipment = shipment("P");
        Charge ecd = chargeWithId("ECD");
        Charge dtc = chargeWithId("DTC");
        Account account = new Account();
        ChargeLineItem ecdLine = lineItem("DUTY", "100");
        ChargeLineItem dtcLine = lineItem("DUTY", "100");
        RateSheetItem cifItem = cifRateSheetItem("DUTY");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem));

        when(chargeLineCopySupport.isCopyDerived("ECD")).thenReturn(false);
        when(chargeLineCopySupport.isCopyDerived("DTC")).thenReturn(true);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTC")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, "LV")).thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(ecd.getId())).thenReturn(List.of(ecdLine));
        when(chargeLineItemRepository.findByCharge_Id(dtc.getId())).thenReturn(List.of(dtcLine));
        when(rateAmountCalculator.calculateWithBasisRate(cifItem, shipment, "ECD"))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(BigDecimal.valueOf(150), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(150), BigDecimal.valueOf(150), 1.0));
        when(chargeLineCopySupport.copyToTarget(shipment, "DTC", "RUH", dtc, List.of(account)))
                .thenReturn(List.of(lineItem("DUTY", "150")));
        when(rateSheetLookupService.resolveForShipmentCharge(shipment, "DTC", "LV"))
                .thenReturn(Optional.of(new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem))));
        when(rateAmountCalculator.calculate(cifItem, shipment, "DTC")).thenReturn(BigDecimal.ONE);
        when(chargeLineItemTaxSupport.computeContractedRate(any(), any(), any())).thenReturn(BigDecimal.ONE);

        unifiedChargeLineItemService.recalculateLineItems(shipment, List.of(ecd, dtc), "LV");

        verify(chargeLineItemRepository).saveAll(anyList());
        verify(chargeRepository).saveAll(List.of(ecd, dtc));
    }

    @Test
    void recalculateActualizedLvToHvLineItems_shouldRefreshEcdEstimateLines() {
        Shipment shipment = shipment("P");
        shipment.setCategoryType(CATEGORY_TYPE_HV);
        Charge ecd = chargeWithId("ECD");
        ChargeLineItem estimate = lineItem("DUTY", "50");
        Account account = new Account();
        RateSheetItem cifItem = cifRateSheetItem("DUTY");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem));

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(ecd.getId())).thenReturn(List.of(estimate));
        when(rateAmountCalculator.calculateWithBasisRate(cifItem, shipment, "ECD"))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(BigDecimal.valueOf(90), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(90), BigDecimal.valueOf(90), 1.0));

        unifiedChargeLineItemService.recalculateActualizedLvToHvLineItems(
                shipment, List.of(ecd), CATEGORY_TYPE_HV);

        verify(chargeLineItemRepository).saveAll(List.of(estimate));
    }

    @Test
    void recalculateActualizedLvToHvLineItems_shouldRefreshDownstreamCopyChain() {
        Shipment shipment = shipment("P");
        shipment.setCategoryType(CATEGORY_TYPE_HV);
        shipment.setOriginEntity("JED");
        Charge ecd = chargeWithId("ECD");
        Charge dta = chargeWithId("DTA");
        Charge ecs = chargeWithId("ECS");
        Charge dts = chargeWithId("DTS");
        Account account = new Account();
        ChargeLineItem ecdLine = lineItem("DUTY", "50");
        ChargeLineItem dtaLine = lineItem("DUTY", "50");
        ChargeLineItem ecsLine = lineItem("DUTY", "50");
        ChargeLineItem dtsLine = lineItem("DUTY", "50");
        RateSheetItem cifItem = cifRateSheetItem("DUTY");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem));

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTA")).thenReturn(Optional.of(account));
        when(serviceChargeAccountResolver.resolve(shipment, "JED", "ECS")).thenReturn(Optional.of(account));
        when(serviceChargeAccountResolver.resolve(shipment, "JED", "DTS")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(ecd.getId())).thenReturn(List.of(ecdLine));
        when(chargeLineItemRepository.findByCharge_Id(dta.getId())).thenReturn(List.of(dtaLine));
        when(chargeLineItemRepository.findByCharge_Id(ecs.getId())).thenReturn(List.of(ecsLine));
        when(chargeLineItemRepository.findByCharge_Id(dts.getId())).thenReturn(List.of(dtsLine));
        when(rateAmountCalculator.calculateWithBasisRate(cifItem, shipment, "ECD"))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(BigDecimal.valueOf(90), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(90), BigDecimal.valueOf(90), 1.0));
        when(rateSheetLookupService.resolveForShipmentCharge(shipment, "DTA", CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(rateSheetLookupService.resolveForShipmentCharge(shipment, "DTS", CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(rateAmountCalculator.calculate(cifItem, shipment, "DTA")).thenReturn(BigDecimal.TEN);
        when(rateAmountCalculator.calculate(cifItem, shipment, "DTS")).thenReturn(BigDecimal.TEN);
        when(chargeLineItemTaxSupport.computeContractedRate(any(), any(), any())).thenReturn(BigDecimal.ONE);
        when(chargeLineCopySupport.copyToTarget(shipment, "ECS", "JED", ecs, List.of(account)))
                .thenReturn(List.of(lineItem("DUTY", "90")));
        when(chargeLineCopySupport.copyToTarget(shipment, "DTS", "JED", dts, List.of(account)))
                .thenReturn(List.of(lineItem("DUTY", "90")));

        unifiedChargeLineItemService.recalculateActualizedLvToHvLineItems(
                shipment, List.of(ecd, dta, ecs, dts), CATEGORY_TYPE_HV);

        verify(chargeLineItemRepository).saveAll(anyList());
    }

    @Test
    void createLineItems_shouldCreateEcsAmountOnlyCopy() {
        Shipment shipment = shipment("P");
        shipment.setOriginEntity("JED");
        Charge ecs = chargeWithId("ECS");
        Account account = new Account();
        ChargeLineItem copied = lineItem("DUTY", "80");

        when(chargeLineCopySupport.isCopyDerived("ECS")).thenReturn(true);
        when(serviceChargeAccountResolver.resolve(shipment, "JED", "ECS")).thenReturn(Optional.of(account));
        when(chargeLineCopySupport.copyToTarget(shipment, "ECS", "JED", ecs, List.of(account)))
                .thenReturn(List.of(copied));

        unifiedChargeLineItemService.createLineItems(shipment, List.of(ecs), "LV");

        verify(chargeLineItemRepository).saveAll(List.of(copied));
    }

    @Test
    void reapplyFullRateSheetAfterCategoryChange_shouldRefreshCopyDerivedCharges() {
        Shipment shipment = shipment("P");
        shipment.setCategoryType(CATEGORY_TYPE_HV);
        shipment.setOriginEntity("JED");
        Charge ecd = chargeWithId("ECD");
        Charge dts = chargeWithId("DTS");
        Account account = new Account();
        ChargeLineItem ecdLine = lineItem("DUTY", "0");
        ChargeLineItem dtsLine = lineItem("DUTY", "0");
        RateSheetItem rateItem = cifRateSheetItem("DUTY");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(rateItem));

        when(chargeLineCopySupport.isCopyDerived("ECD")).thenReturn(false);
        when(chargeLineCopySupport.isCopyDerived("DTS")).thenReturn(true);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(serviceChargeAccountResolver.resolve(shipment, "JED", "DTS")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(ecd.getId())).thenReturn(List.of(ecdLine));
        when(chargeLineItemRepository.findByCharge_Id(dts.getId())).thenReturn(List.of(dtsLine));
        when(rateAmountCalculator.calculateWithBasisRate(rateItem, shipment, "ECD"))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(BigDecimal.valueOf(200), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(200), BigDecimal.valueOf(200), 1.0));
        when(chargeLineCopySupport.copyToTarget(shipment, "DTS", "JED", dts, List.of(account)))
                .thenReturn(List.of(lineItem("DUTY", "200")));
        when(rateSheetLookupService.resolveForShipmentCharge(shipment, "DTS", CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(rateAmountCalculator.calculate(rateItem, shipment, "DTS")).thenReturn(BigDecimal.ONE);
        when(chargeLineItemTaxSupport.computeContractedRate(any(), any(), any())).thenReturn(BigDecimal.ONE);

        unifiedChargeLineItemService.reapplyFullRateSheetAfterCategoryChange(
                shipment, List.of(ecd, dts), CATEGORY_TYPE_HV);

        verify(chargeLineItemRepository).saveAll(anyList());
    }

    @Test
    void createLineItems_shouldZeroCopyDerivedLines_whenExempted() {
        Shipment shipment = shipment("P");
        shipment.setCategoryType(CATEGORY_TYPE_EXEMPTED);
        Charge dtc = chargeWithId("DTC");
        Account account = new Account();
        ChargeLineItem copied = lineItem("DUTY", "75");

        when(chargeLineCopySupport.isCopyDerived("DTC")).thenReturn(true);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTC")).thenReturn(Optional.of(account));
        when(chargeLineCopySupport.copyToTarget(shipment, "DTC", "RUH", dtc, List.of(account)))
                .thenReturn(List.of(copied));

        unifiedChargeLineItemService.createLineItems(shipment, List.of(dtc), "LV");

        assertThat(copied.getAmount()).isEqualByComparingTo("0");
        verify(chargeLineItemRepository).saveAll(List.of(copied));
    }

    @Test
    void recalculateLineItems_shouldCopyEcsAmountsAfterEcdUpdate() {
        Shipment shipment = shipment("P");
        shipment.setOriginEntity("JED");
        Charge ecd = chargeWithId("ECD");
        Charge ecs = chargeWithId("ECS");
        Account account = new Account();
        ChargeLineItem ecdLine = lineItem("DUTY", "100");
        ChargeLineItem ecsLine = lineItem("DUTY", "0");
        RateSheetItem cifItem = cifRateSheetItem("DUTY");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem));

        when(chargeLineCopySupport.isCopyDerived("ECD")).thenReturn(false);
        when(chargeLineCopySupport.isCopyDerived("ECS")).thenReturn(true);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(serviceChargeAccountResolver.resolve(shipment, "JED", "ECS")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, "LV")).thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(ecd.getId())).thenReturn(List.of(ecdLine));
        when(chargeLineItemRepository.findByCharge_Id(ecs.getId())).thenReturn(List.of(ecsLine));
        when(rateAmountCalculator.calculateWithBasisRate(cifItem, shipment, "ECD"))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(BigDecimal.valueOf(120), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(120), BigDecimal.valueOf(120), 1.0));
        when(chargeLineCopySupport.copyToTarget(shipment, "ECS", "JED", ecs, List.of(account)))
                .thenReturn(List.of(lineItem("DUTY", "120")));

        unifiedChargeLineItemService.recalculateLineItems(shipment, List.of(ecd, ecs), "LV");

        verify(chargeLineItemRepository).saveAll(anyList());
    }

    @Test
    void createLineItems_shouldSkipLineCreation_whenPaymentTypeIneligible() {
        Shipment shipment = shipment("3");
        Charge ecs = chargeWithId("ECS");

        unifiedChargeLineItemService.createLineItems(shipment, List.of(ecs), "LV");

        verify(chargeLineItemRepository, never()).saveAll(any());
        verify(chargeRepository).saveAll(List.of(ecs));
    }

    @Test
    void recalculateLineItems_shouldNoOp_whenInputsMissing() {
        unifiedChargeLineItemService.recalculateLineItems(null, List.of(chargeWithId("ECD")), "LV");
        unifiedChargeLineItemService.recalculateLineItems(shipment("P"), null, "LV");
        unifiedChargeLineItemService.recalculateLineItems(shipment("P"), List.of(), "LV");

        verify(chargeLineItemRepository, never()).saveAll(any());
    }

    @Test
    void createLineItems_shouldSkip_whenEntityCodeMissing() {
        Shipment shipment = shipment("P");
        Charge ecs = chargeWithId("ECS");

        unifiedChargeLineItemService.createLineItems(shipment, List.of(ecs), "LV");

        verify(chargeLineItemRepository, never()).saveAll(any());
    }

    @Test
    void createLineItems_shouldSkip_whenAccountMissing() {
        Shipment shipment = shipment("P");
        Charge ecd = chargeWithId("ECD");
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.empty());

        unifiedChargeLineItemService.createLineItems(shipment, List.of(ecd), "LV");

        verify(chargeLineItemRepository, never()).saveAll(any());
    }

    @Test
    void createLineItems_shouldSkip_whenNoServiceChargesResolved() {
        Shipment shipment = shipment("P");
        Charge ecd = chargeWithId("ECD");
        Account account = new Account();

        when(chargeLineCopySupport.isCopyDerived("ECD")).thenReturn(false);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(serviceChargeResolver.resolve(any(ServiceChargeQueryCriteria.class))).thenReturn(List.of());

        unifiedChargeLineItemService.createLineItems(shipment, List.of(ecd), "LV");

        verify(chargeLineItemRepository, never()).saveAll(any());
    }

    @Test
    void createLineItems_shouldRegisterLineItemsInProcessingContext() {
        Shipment shipment = shipment("P");
        Charge ecd = chargeWithId("ECD");
        Account account = new Account();
        ServiceChargeResponse serviceCharge = new ServiceChargeResponse(
                null, "DUTY", "Duty", null, null, true, null, null, true, null, null, null, null, null, null, null);
        RateSheetItem rateItem = cifRateSheetItem("DUTY");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(rateItem));

        lenient().when(chargeLineCopySupport.isCopyDerived("ECD")).thenReturn(false);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(serviceChargeResolver.resolve(any(ServiceChargeQueryCriteria.class)))
                .thenReturn(List.of(serviceCharge));
        when(rateSheetLookupService.resolveForAccount(account, shipment, "LV")).thenReturn(Optional.of(resolved));
        when(rateAmountCalculator.calculateWithBasisRate(rateItem, shipment, "ECD"))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(BigDecimal.TEN, 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.TEN, BigDecimal.TEN, 1.0));

        shipmentProcessingContextFactory.begin(shipment, List.of("ECD"));
        try {
            unifiedChargeLineItemService.createLineItems(shipment, List.of(ecd), "LV");
            assertThat(ShipmentProcessingContext.get().findLineItemsByChargeCode("ECD")).isPresent();
        } finally {
            shipmentProcessingContextFactory.end();
        }
    }

    @Test
    void recalculateLineItems_shouldRefreshDtcWhenOnlyDtcRateSheetHasCif() {
        Shipment shipment = shipment("P");
        Charge dtc = chargeWithId("DTC");
        Account account = new Account();
        ChargeLineItem dtcLine = lineItem("DUTY", "100");
        RateSheetItem cifItem = cifRateSheetItem("DUTY");

        when(chargeLineCopySupport.isCopyDerived("DTC")).thenReturn(true);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTC")).thenReturn(Optional.of(account));
        when(serviceChargeAccountResolver.resolveBillingAccount(shipment, "RUH", "DTC", account))
                .thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForShipmentCharge(shipment, "DTC", "LV"))
                .thenReturn(Optional.of(new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem))));
        when(chargeLineItemRepository.findByCharge_Id(dtc.getId())).thenReturn(List.of(dtcLine));
        when(chargeLineCopySupport.copyToTarget(shipment, "DTC", "RUH", dtc, List.of(account)))
                .thenReturn(List.of(lineItem("DUTY", "100")));
        when(rateAmountCalculator.calculate(cifItem, shipment, "DTC")).thenReturn(BigDecimal.valueOf(25));
        when(chargeLineItemTaxSupport.computeContractedRate(any(), any(), any())).thenReturn(BigDecimal.ONE);

        unifiedChargeLineItemService.recalculateLineItems(shipment, List.of(dtc), "LV");

        verify(rateSheetLookupService, org.mockito.Mockito.atLeastOnce())
                .resolveForShipmentCharge(shipment, "DTC", "LV");
        verify(chargeLineItemRepository).saveAll(anyList());
        verify(chargeRepository).saveAll(List.of(dtc));
    }

    @Test
    void recalculateLineItems_shouldSkipCopyDerivedBeforeCifLinesUpdated() {
        Shipment shipment = shipment("P");
        Charge dtc = chargeWithId("DTC");

        when(chargeLineCopySupport.isCopyDerived("DTC")).thenReturn(true);

        unifiedChargeLineItemService.recalculateLineItems(shipment, List.of(dtc), "LV");

        verify(chargeLineItemRepository, never()).saveAll(any());
    }

    @Test
    void recalculateExemptedLineItems_shouldSkip_whenChargeIdMissing() {
        Shipment shipment = shipment("P");
        shipment.setCategoryType(CATEGORY_TYPE_EXEMPTED);
        Charge charge = new Charge();
        charge.setChargeCode("ECD");

        unifiedChargeLineItemService.recalculateExemptedLineItems(shipment, List.of(charge));

        verify(chargeLineItemRepository, never()).saveAll(any());
    }

    @Test
    void recalculateExemptedLineItems_shouldSkip_whenNoExistingLineItems() {
        Shipment shipment = shipment("P");
        shipment.setCategoryType(CATEGORY_TYPE_EXEMPTED);
        Charge ecd = chargeWithId("ECD");
        when(chargeLineItemRepository.findByCharge_Id(ecd.getId())).thenReturn(List.of());

        unifiedChargeLineItemService.recalculateExemptedLineItems(shipment, List.of(ecd));

        verify(chargeLineItemRepository, never()).saveAll(any());
    }

    @Test
    void recalculateActualizedCifOnly_shouldSkipNonEcdCharges() {
        Shipment shipment = shipment("P");
        Charge dtc = chargeWithId("DTC");

        unifiedChargeLineItemService.recalculateActualizedCifOnlyLineItems(shipment, List.of(dtc), "LV");

        verify(chargeLineItemRepository, never()).saveAll(any());
    }

    @Test
    void recalculateActualizedLvToHv_shouldApplyContractedRateOnDtaAfterEcd() {
        Shipment shipment = shipment("P");
        shipment.setCategoryType(CATEGORY_TYPE_HV);
        Charge ecd = chargeWithId("ECD");
        Charge dta = chargeWithId("DTA");
        Account account = new Account();
        ChargeLineItem ecdLine = lineItem("DUTY", "50");
        ChargeLineItem dtaLine = lineItem("DUTY", "50");
        RateSheetItem cifItem = cifRateSheetItem("DUTY");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem));

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTA")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(ecd.getId())).thenReturn(List.of(ecdLine));
        when(chargeLineItemRepository.findByCharge_Id(dta.getId())).thenReturn(List.of(dtaLine));
        when(rateAmountCalculator.calculateWithBasisRate(cifItem, shipment, "ECD"))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(BigDecimal.valueOf(90), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(90), BigDecimal.valueOf(90), 1.0));
        when(rateSheetLookupService.resolveForShipmentCharge(shipment, "DTA", CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(rateAmountCalculator.calculate(cifItem, shipment, "DTA")).thenReturn(BigDecimal.valueOf(5));
        when(chargeLineItemTaxSupport.computeContractedRate(any(), any(), any())).thenReturn(BigDecimal.ONE);

        unifiedChargeLineItemService.recalculateActualizedLvToHvLineItems(
                shipment, List.of(ecd, dta), CATEGORY_TYPE_HV);

        verify(chargeLineItemRepository).saveAll(anyList());
    }

    @Test
    void recalculateActualizedLvToHv_shouldCopyEcsAmountsAfterEcd() {
        Shipment shipment = shipment("P");
        shipment.setCategoryType(CATEGORY_TYPE_HV);
        shipment.setOriginEntity("JED");
        Charge ecd = chargeWithId("ECD");
        Charge ecs = chargeWithId("ECS");
        Account account = new Account();
        ChargeLineItem ecdLine = lineItem("DUTY", "50");
        ChargeLineItem ecsLine = lineItem("DUTY", "0");
        RateSheetItem cifItem = cifRateSheetItem("DUTY");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(cifItem));

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(serviceChargeAccountResolver.resolve(shipment, "JED", "ECS")).thenReturn(Optional.of(account));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(ecd.getId())).thenReturn(List.of(ecdLine));
        when(chargeLineItemRepository.findByCharge_Id(ecs.getId())).thenReturn(List.of(ecsLine));
        when(rateAmountCalculator.calculateWithBasisRate(cifItem, shipment, "ECD"))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(BigDecimal.valueOf(90), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                any(), any(), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(90), BigDecimal.valueOf(90), 1.0));
        ChargeLineItem copied = lineItem("DUTY", "90");
        copied.setApAccountNumber("AP-1");
        when(chargeLineCopySupport.copyToTarget(shipment, "ECS", "JED", ecs, List.of(account)))
                .thenReturn(List.of(copied));

        unifiedChargeLineItemService.recalculateActualizedLvToHvLineItems(
                shipment, List.of(ecd, ecs), CATEGORY_TYPE_HV);

        assertThat(ecsLine.getAmount()).isEqualByComparingTo("90");
        assertThat(ecsLine.getApAccountNumber()).isEqualTo("AP-1");
        verify(chargeLineItemRepository).saveAll(anyList());
    }

    @Test
    void reapplyFullRateSheet_shouldSkipCopyDerivedUntilResolverUpdated() {
        Shipment shipment = shipment("P");
        shipment.setCategoryType(CATEGORY_TYPE_HV);
        shipment.setOriginEntity("JED");
        Charge dts = chargeWithId("DTS");

        when(chargeLineCopySupport.isCopyDerived("DTS")).thenReturn(true);

        unifiedChargeLineItemService.reapplyFullRateSheetAfterCategoryChange(
                shipment, List.of(dts), CATEGORY_TYPE_HV);

        verify(chargeLineItemRepository, never()).saveAll(any());
    }

    @Test
    void createLineItems_shouldSetZeroContractedRate_whenServiceCodeMissingFromRateSheet() {
        Shipment shipment = shipment("P");
        Charge dtc = chargeWithId("DTC");
        Account account = new Account();
        ChargeLineItem copied = lineItem("UNKNOWN", "75");

        when(chargeLineCopySupport.isCopyDerived("DTC")).thenReturn(true);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTC")).thenReturn(Optional.of(account));
        when(chargeLineCopySupport.copyToTarget(shipment, "DTC", "RUH", dtc, List.of(account)))
                .thenReturn(List.of(copied));
        when(rateSheetLookupService.resolveForShipmentCharge(shipment, "DTC", "LV"))
                .thenReturn(Optional.of(new ResolvedRateSheet(account, new RateSheet(), List.of())));

        unifiedChargeLineItemService.createLineItems(shipment, List.of(dtc), "LV");

        assertThat(copied.getContractedRate()).isEqualByComparingTo("0");
        verify(chargeLineItemRepository).saveAll(anyList());
    }

    @Test
    void reapplyFullRateSheet_shouldZeroLines_whenRateSheetMissing() {
        Shipment shipment = shipment("P");
        shipment.setCategoryType(CATEGORY_TYPE_HV);
        Charge ecd = chargeWithId("ECD");
        Account account = new Account();
        ChargeLineItem vatLine = lineItem("VAT", "200");

        when(chargeLineCopySupport.isCopyDerived("ECD")).thenReturn(false);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(serviceChargeAccountResolver.resolveBillingAccount(shipment, "RUH", "ECD", account))
                .thenReturn(Optional.of(account));
        when(chargeLineItemRepository.findByCharge_Id(ecd.getId())).thenReturn(List.of(vatLine));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.empty());

        unifiedChargeLineItemService.reapplyFullRateSheetAfterCategoryChange(
                shipment, List.of(ecd), CATEGORY_TYPE_HV);

        assertThat(vatLine.getAmount()).isEqualByComparingTo("0");
        verify(chargeLineItemRepository).saveAll(List.of(vatLine));
    }

    @Test
    void reapplyFullRateSheetAfterCategoryChange_shouldZeroLvVatAndApplyHvDuty() {
        Shipment shipment = shipment("P");
        shipment.setCategoryType(CATEGORY_TYPE_HV);
        Charge ecd = chargeWithId("ECD");
        Account account = new Account();
        ChargeLineItem vatLine = lineItem("VAT", "200");
        ChargeLineItem dutyLine = lineItem("DUTY", "0");
        RateSheetItem dutyItem = fixedRateSheetItem("DUTY", "500");
        ResolvedRateSheet resolved = new ResolvedRateSheet(account, new RateSheet(), List.of(dutyItem));

        when(chargeLineCopySupport.isCopyDerived("ECD")).thenReturn(false);
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "ECD")).thenReturn(Optional.of(account));
        when(serviceChargeAccountResolver.resolveBillingAccount(shipment, "RUH", "ECD", account))
                .thenReturn(Optional.of(account));
        when(chargeLineItemRepository.findByCharge_Id(ecd.getId())).thenReturn(List.of(vatLine, dutyLine));
        when(rateSheetLookupService.resolveForAccount(account, shipment, CATEGORY_TYPE_HV))
                .thenReturn(Optional.of(resolved));
        when(rateAmountCalculator.calculateWithBasisRate(dutyItem, shipment, "ECD"))
                .thenReturn(new RateAmountCalculator.CalculatedRateAmount(BigDecimal.valueOf(500), 1.0));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                eq(BigDecimal.valueOf(500)), eq(shipment), eq(ecd), eq(account), eq("RUH"), eq("DUTY"),
                any(), eq(false), eq(1.0)))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(500), BigDecimal.valueOf(500), 1.0));

        unifiedChargeLineItemService.reapplyFullRateSheetAfterCategoryChange(
                shipment, List.of(ecd), CATEGORY_TYPE_HV);

        assertThat(vatLine.getAmount()).isEqualByComparingTo("0");
        assertThat(dutyLine.getAmount()).isEqualByComparingTo("500");
        verify(chargeLineItemRepository).saveAll(List.of(vatLine, dutyLine));
    }

    private static RateSheetItem fixedRateSheetItem(String serviceCode, String amount) {
        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateType(RateType.FIXED);
        rate.setValue(new BigDecimal(amount));
        RateSheetItem item = new RateSheetItem();
        item.setServiceCode(serviceCode);
        item.setCategoryType(CATEGORY_TYPE_HV);
        item.setRates(List.of(rate));
        return item;
    }

    private static Shipment shipment(String paymentType) {
        Shipment shipment = new Shipment();
        shipment.setHawbNumber("HAWB-1");
        shipment.setCategoryType("LV");
        shipment.setPaymentType(paymentType);
        shipment.setHAWBDestinationEntity("RUH");
        return shipment;
    }

    private static Charge chargeWithId(String code) {
        Charge charge = new Charge();
        ReflectionTestUtils.setField(charge, "id", UUID.randomUUID());
        charge.setChargeCode(code);
        return charge;
    }

    private static ChargeLineItem lineItem(String serviceCode, String amount) {
        ChargeLineItem line = new ChargeLineItem();
        line.setServiceCode(serviceCode);
        line.setAmount(new BigDecimal(amount));
        return line;
    }

    private static RateSheetItem cifRateSheetItem(String serviceCode) {
        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateType(RateType.PERCENTAGE);
        rate.setValue(BigDecimal.TEN);
        rate.setPercentageOf(PercentageOf.CIF_VALUE);
        RateSheetItem item = new RateSheetItem();
        item.setServiceCode(serviceCode);
        item.setRates(List.of(rate));
        return item;
    }
}
