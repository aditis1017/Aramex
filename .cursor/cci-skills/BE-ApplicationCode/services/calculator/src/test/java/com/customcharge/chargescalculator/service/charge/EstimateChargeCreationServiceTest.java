package com.customcharge.chargescalculator.service.charge;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.constants.ChargeCreationConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.service.batch.RecordProcessingLookups;
import com.customcharge.chargescalculator.service.batch.RecordProcessingLookupsTestSupport;
import com.customcharge.chargescalculator.service.batch.RowChargeWriteBuffer;
import com.customcharge.chargescalculator.service.calculation.TaxValueAmountCalculator;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.chargescalculator.support.ChargeHeaderAmountTestSupport;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EstimateChargeCreationServiceTest {

    @Mock
    private ChargeLineSupport chargeLineSupport;
    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private ChargeLineItemRepository chargeLineItemRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private RecordProcessingLookups recordProcessingLookups;
    @Mock
    private DtaContextSupport dtaContextSupport;
    @Mock
    private ChargeLineCopySupport chargeLineCopySupport;

    private EstimateChargeCreationService service;
    private RowChargeWriteBuffer rowChargeWriteBuffer;

    private Shipment shipment;
    private ChargeCalculationContext context;
    private Account account;

    @BeforeEach
    void setUp() {
        shipment = CalculatorTestDataFactory.shipment();
        context = CalculatorTestDataFactory.chargeContext(shipment);
        account = CalculatorTestDataFactory.account(UUID.randomUUID(), "DTC");
        RecordProcessingLookupsTestSupport.delegateToRepositories(
                recordProcessingLookups, chargeRepository, chargeLineItemRepository, entityRepository);
        rowChargeWriteBuffer = new RowChargeWriteBuffer(chargeRepository, chargeLineItemRepository);
        rowChargeWriteBuffer.open();
        service = new EstimateChargeCreationService(
                chargeLineSupport,
                chargeRepository,
                chargeLineItemRepository,
                recordProcessingLookups,
                dtaContextSupport,
                chargeLineCopySupport,
                rowChargeWriteBuffer);
        lenient().when(chargeLineSupport.formatAmount(any(BigDecimal.class), any(Charge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doNothing().when(chargeLineSupport).applyChargeCurrency(any(), any(), any(), any(), any(Boolean.class));
        lenient().doNothing().when(chargeLineSupport).applyEstimateChargeCurrency(any(), any(), any(), any());
        lenient().doAnswer(invocation -> {
            ChargeLineItem line = invocation.getArgument(0);
            BigDecimal preTax = invocation.getArgument(1);
            BigDecimal taxPercent = invocation.getArgument(2);
            line.setAmount(preTax);
            line.setPreTaxableAmount(TaxValueAmountCalculator.apply(preTax, taxPercent));
            return null;
        }).when(chargeLineSupport).applyTaxedLineAmounts(any(), any(), any(), any());
        lenient().when(chargeLineSupport.convertEstimateLineAmount(
                any(), any(), any(), any(), any(), any(), any(), any(Boolean.class), any(Boolean.class), any(), any()))
                .thenAnswer(invocation -> {
                    BigDecimal amount = invocation.getArgument(1);
                    return new com.customcharge.persistencelayer.dto.CurrencyConversionResult(
                            amount == null ? 0.0 : amount.doubleValue(), 1.0);
                });
        lenient().when(dtaContextSupport.forCharge(any(), any(), any(), any(), any()))
                .thenReturn(new DtaContextSupport.DtaLineContext(Map.of(), false, false, null));
        ChargeHeaderAmountTestSupport.stubApplyChargeHeaderAmounts(chargeLineSupport);
    }

    @AfterEach
    void tearDown() {
        rowChargeWriteBuffer.close();
    }

    @Test
    void createOrUpdateEstimateCharge_shouldThrow_whenEntityCodeMissing() {
        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn(" ");

        assertThatThrownBy(() -> service.createOrUpdateEstimateCharge(context, "DTC"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(ChargeCreationConstants.MISSING_ENTITY_CODE);
    }

    @Test
    void createOrUpdateEstimateCharge_shouldThrow_whenNoAccounts() {
        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", "DTC")).thenReturn(List.of());

        assertThatThrownBy(() -> service.createOrUpdateEstimateCharge(context, "DTC"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(ChargeCreationConstants.NO_ACCOUNTS_FOR_CHARGE);
    }

    @Test
    void createOrUpdateEstimateCharge_shouldCreateChargeAndLines() {
        UUID chargeId = UUID.randomUUID();
        Charge charge = CalculatorTestDataFactory.charge(chargeId, shipment, "DTC");
        ServiceChargeResponse costLine = CalculatorTestDataFactory.serviceCharge("SC-C", "customs", "C", null, true);
        ServiceChargeResponse revenueLine = CalculatorTestDataFactory.serviceCharge("SC-R", "revenue", "C", BigDecimal.valueOf(2), true);

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", "DTC")).thenReturn(List.of(account));
        when(entityRepository.findByCodeIgnoreCase("DXB"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), "DTC")).thenReturn(Optional.empty());
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> {
            Charge saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", chargeId);
            }
            return saved;
        });
        when(chargeLineSupport.resolveServiceCharges("DXB", account, "DTC"))
                .thenReturn(List.of(costLine, revenueLine));
        when(chargeLineItemRepository.findByCharge_Id(chargeId)).thenReturn(List.of());

        service.createOrUpdateEstimateCharge(context, "DTC");

        rowChargeWriteBuffer.flush();
        verify(chargeLineItemRepository).saveAll(any());
        verify(chargeRepository, times(1)).save(any(Charge.class));
    }

    @Test
    void createOrUpdateEstimateCharge_shouldSetOutstandingEqualToEstimateAmount_forDtc() {
        UUID chargeId = UUID.randomUUID();
        Charge charge = CalculatorTestDataFactory.charge(chargeId, shipment, BatchProcessingConstants.CHARGE_CODE_DTC);
        ServiceChargeResponse costLine = CalculatorTestDataFactory.serviceCharge("SC-C", "customs", "X", null, true);
        ServiceChargeResponse revenueLine = CalculatorTestDataFactory.serviceCharge("SC-R", "revenue", "C", BigDecimal.valueOf(2), true);
        List<ChargeLineItem> savedLines = List.of(
                CalculatorTestDataFactory.chargeLineItem(charge, account, "SC-C", new BigDecimal("10.50")),
                CalculatorTestDataFactory.chargeLineItem(charge, account, "SC-R", BigDecimal.valueOf(2)));

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_DTC))
                .thenReturn(List.of(account));
        when(entityRepository.findByCodeIgnoreCase("DXB"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_DTC))
                .thenReturn(Optional.empty());
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> {
            Charge saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", chargeId);
            }
            return saved;
        });
        when(chargeLineSupport.resolveServiceCharges("DXB", account, BatchProcessingConstants.CHARGE_CODE_DTC))
                .thenReturn(List.of(costLine, revenueLine));
        when(chargeLineItemRepository.findByCharge_Id(chargeId)).thenReturn(List.of(), savedLines);

        service.createOrUpdateEstimateCharge(context, BatchProcessingConstants.CHARGE_CODE_DTC);

        Charge savedCharge = captureFlushedChargeUpdate();
        assertThat(savedCharge.getEstimateAmount()).isEqualByComparingTo("12.50");
        assertThat(savedCharge.getOutstandingAmount()).isEqualByComparingTo("12.50");
    }

    @Test
    void createOrUpdateEstimateCharge_shouldSetOutstandingEqualToEstimateAmount_forDts() {
        UUID chargeId = UUID.randomUUID();
        Charge charge = CalculatorTestDataFactory.charge(chargeId, shipment, BatchProcessingConstants.CHARGE_CODE_DTS);
        ServiceChargeResponse costLine = CalculatorTestDataFactory.serviceCharge("SC-C", "customs", "X", null, true);
        ServiceChargeResponse revenueLine = CalculatorTestDataFactory.serviceCharge("SC-R", "revenue", "C", BigDecimal.valueOf(2), true);
        List<ChargeLineItem> savedLines = List.of(
                CalculatorTestDataFactory.chargeLineItem(charge, account, "SC-C", new BigDecimal("10")),
                CalculatorTestDataFactory.chargeLineItem(charge, account, "SC-R", BigDecimal.valueOf(2)));
        context.getCalculatedCharges().put(
                BatchProcessingConstants.CALCULATED_CHARGES_KEY_DTA_REVENUE,
                Map.of("revenue", BigDecimal.valueOf(5)));

        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_DTS))
                .thenReturn(List.of(account));
        when(entityRepository.findByCodeIgnoreCase("DXB"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_DTS))
                .thenReturn(Optional.empty());
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> {
            Charge saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", chargeId);
            return saved;
        });
        when(chargeLineSupport.resolveServiceCharges("DXB", account, BatchProcessingConstants.CHARGE_CODE_DTS))
                .thenReturn(List.of(costLine, revenueLine));
        when(chargeLineItemRepository.findByCharge_Id(chargeId)).thenReturn(List.of(), savedLines);

        service.createOrUpdateEstimateCharge(context, BatchProcessingConstants.CHARGE_CODE_DTS, "DXB");

        Charge savedCharge = captureFlushedChargeUpdate();
        assertThat(savedCharge.getEstimateAmount()).isEqualByComparingTo("12");
        assertThat(savedCharge.getOutstandingAmount()).isEqualByComparingTo("12");
    }

    @Test
    void createOrUpdateEstimateCharge_shouldSetExemptedStatusAndZeroAmounts_whenDdoShipmentType() {
        shipment.setShipmentType(BatchProcessingConstants.SHIPMENT_TYPE_DDO);
        UUID chargeId = UUID.randomUUID();
        ServiceChargeResponse revenueLine = CalculatorTestDataFactory.serviceCharge(
                "SC-R", "revenue", "C", BigDecimal.TEN, true);

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", "DTC")).thenReturn(List.of(account));
        when(entityRepository.findByCodeIgnoreCase("DXB"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), "DTC")).thenReturn(Optional.empty());
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> {
            Charge saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", chargeId);
            return saved;
        });
        when(chargeLineSupport.resolveServiceCharges("DXB", account, "DTC"))
                .thenReturn(List.of(revenueLine));
        when(chargeLineItemRepository.findByCharge_Id(chargeId)).thenReturn(List.of());

        service.createOrUpdateEstimateCharge(context, "DTC");

        Charge savedCharge = captureFlushedChargeUpdate();
        assertThat(savedCharge.getStatus()).isEqualTo(BatchProcessingConstants.STATUS_EXEMPTED);
        assertThat(savedCharge.getEstimateAmount()).isEqualByComparingTo("0");
        assertThat(savedCharge.getOutstandingAmount()).isEqualByComparingTo("0");
    }

    @Test
    void createOrUpdateEstimateCharge_shouldSetExemptedStatusAndZeroAmounts_whenCategoryExempted() {
        shipment.setCategoryType(BatchProcessingConstants.CATEGORY_TYPE_EXEMPTED);
        UUID chargeId = UUID.randomUUID();
        ServiceChargeResponse revenueLine = CalculatorTestDataFactory.serviceCharge(
                "SC-R", "revenue", "C", BigDecimal.TEN, true);

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", "DTC")).thenReturn(List.of(account));
        when(entityRepository.findByCodeIgnoreCase("DXB"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), "DTC")).thenReturn(Optional.empty());
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> {
            Charge saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", chargeId);
            return saved;
        });
        when(chargeLineSupport.resolveServiceCharges("DXB", account, "DTC"))
                .thenReturn(List.of(revenueLine));
        when(chargeLineItemRepository.findByCharge_Id(chargeId)).thenReturn(List.of());

        service.createOrUpdateEstimateCharge(context, "DTC");

        Charge savedCharge = captureFlushedChargeUpdate();
        assertThat(savedCharge.getStatus()).isEqualTo(BatchProcessingConstants.STATUS_EXEMPTED);
        assertThat(savedCharge.getEstimateAmount()).isEqualByComparingTo("0");
        assertThat(savedCharge.getOutstandingAmount()).isEqualByComparingTo("0");
    }

    @Test
    void createOrUpdateEstimateCharge_shouldStoreDtaRevenueMap_forDtaCharge() {
        UUID chargeId = UUID.randomUUID();
        ServiceChargeResponse revenueLine = CalculatorTestDataFactory.serviceCharge("SC-R", "revenue fee", "C", BigDecimal.valueOf(3), true);

        when(chargeLineSupport.resolveAccounts(shipment, "DXB", "DTA")).thenReturn(List.of(account));
        when(entityRepository.findByCodeIgnoreCase("DXB"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), "DTA")).thenReturn(Optional.empty());
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> {
            Charge saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", chargeId);
            return saved;
        });
        when(chargeLineSupport.resolveServiceCharges("DXB", account, "DTA"))
                .thenReturn(List.of(revenueLine));
        when(chargeLineItemRepository.findByCharge_Id(chargeId)).thenReturn(List.of());
        when(dtaContextSupport.forCharge(context, shipment, "DTA", "DXB", chargeLineSupport))
                .thenReturn(new DtaContextSupport.DtaLineContext(Map.of(), false, false, new LinkedHashMap<>()));

        service.createOrUpdateEstimateCharge(context, "DTA", "DXB");

        verify(chargeLineItemRepository).saveAll(any());
        assertThat(context.getCalculatedCharges())
                .containsKey(BatchProcessingConstants.CALCULATED_CHARGES_KEY_DTA_REVENUE);
    }

    @Test
    void createOrUpdateEstimateCharge_shouldNotApplyTax_forEcdFileUpload() {
        UUID chargeId = UUID.randomUUID();
        Charge charge = CalculatorTestDataFactory.charge(chargeId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);
        ServiceChargeResponse costLine = CalculatorTestDataFactory.serviceCharge(
                "DUTY", "customs duty inbound", "X", BigDecimal.valueOf(15), true);
        Map<String, String> source = new LinkedHashMap<>();
        source.put("customs duty inbound", "100");
        context = CalculatorTestDataFactory.chargeContext(
                shipment, BatchProcessingConstants.PROCESS_FOR_ESTIMATE, source);

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_ECD))
                .thenReturn(List.of(account));
        when(entityRepository.findByCodeIgnoreCase("DXB"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));
        when(chargeRepository.findByShipment_IdAndChargeCode(
                shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD))
                .thenReturn(Optional.empty());
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> {
            Charge saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", chargeId);
            return saved;
        });
        when(chargeLineSupport.resolveServiceCharges("DXB", account, BatchProcessingConstants.CHARGE_CODE_ECD))
                .thenReturn(List.of(costLine));
        when(chargeLineItemRepository.findByCharge_Id(chargeId)).thenReturn(List.of());
        lenient().doAnswer(invocation -> {
            ChargeLineItem line = invocation.getArgument(0);
            BigDecimal amount = invocation.getArgument(1);
            line.setAmount(amount);
            line.setPreTaxableAmount(amount);
            return null;
        }).when(chargeLineSupport).applyContextDerivedLineAmounts(any(), any(), any());

        service.createOrUpdateEstimateCharge(context, BatchProcessingConstants.CHARGE_CODE_ECD);

        verify(chargeLineSupport).applyContextDerivedLineAmounts(any(), any(), any());
        verify(chargeLineSupport, never()).applyTaxedLineAmounts(any(), any(), any(), any());
    }

    @Test
    void createOrUpdateEstimateCharge_shouldApplyTax_forDtcFileUpload() {
        UUID chargeId = UUID.randomUUID();
        ServiceChargeResponse revenueLine = CalculatorTestDataFactory.serviceCharge(
                "SC-R", "revenue", "C", BigDecimal.valueOf(15), true);

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_DTC))
                .thenReturn(List.of(account));
        when(entityRepository.findByCodeIgnoreCase("DXB"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));
        when(chargeRepository.findByShipment_IdAndChargeCode(
                shipment.getId(), BatchProcessingConstants.CHARGE_CODE_DTC))
                .thenReturn(Optional.empty());
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> {
            Charge saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", chargeId);
            return saved;
        });
        when(chargeLineSupport.resolveServiceCharges("DXB", account, BatchProcessingConstants.CHARGE_CODE_DTC))
                .thenReturn(List.of(revenueLine));
        when(chargeLineItemRepository.findByCharge_Id(chargeId)).thenReturn(List.of());

        service.createOrUpdateEstimateCharge(context, BatchProcessingConstants.CHARGE_CODE_DTC);

        verify(chargeLineSupport, times(1)).applyTaxedLineAmounts(any(), any(), any(), any());
        verify(chargeLineSupport, never()).applyContextDerivedLineAmounts(any(), any(), any());
    }

    @Test
    void createOrUpdateEstimateCharge_shouldRemapDtaContext_forEcsWithDestinationAndOrigin() {
        UUID chargeId = UUID.randomUUID();
        ServiceChargeResponse costLine = CalculatorTestDataFactory.serviceCharge("SC-C", "customs", "X", null, true);
        context.getCalculatedCharges().put(
                BatchProcessingConstants.CALCULATED_CHARGES_KEY_DTA_REVENUE,
                Map.of("customs duty", new BigDecimal("10")));

        when(chargeLineSupport.resolveAccounts(shipment, "RUH", BatchProcessingConstants.CHARGE_CODE_ECS))
                .thenReturn(List.of(account));
        when(entityRepository.findByCodeIgnoreCase("RUH"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("RUH")));
        when(chargeRepository.findByShipment_IdAndChargeCode(
                shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECS))
                .thenReturn(Optional.empty());
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> {
            Charge saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", chargeId);
            return saved;
        });
        when(chargeLineSupport.resolveServiceCharges("RUH", account, BatchProcessingConstants.CHARGE_CODE_ECS))
                .thenReturn(List.of(costLine));
        when(chargeLineItemRepository.findByCharge_Id(chargeId)).thenReturn(List.of());

        service.createOrUpdateEstimateCharge(
                context, BatchProcessingConstants.CHARGE_CODE_ECS, "RUH");

        verify(dtaContextSupport).forCharge(
                context,
                shipment,
                BatchProcessingConstants.CHARGE_CODE_ECS,
                "RUH",
                chargeLineSupport);
    }

    @Test
    void mergeCopiedLine_shouldPreserveContractedRate_whenCopyCarriesCombinedAmountOnly() {
        ChargeLineItem existing = new ChargeLineItem();
        existing.setAmount(new BigDecimal("0.53"));
        existing.setContractedRate(new BigDecimal("10"));

        ChargeLineItem copied = new ChargeLineItem();
        copied.setAmount(new BigDecimal("0.53"));
        copied.setContractedRate(BigDecimal.ZERO);
        copied.setPreTaxableAmount(new BigDecimal("0.53"));

        EstimateChargeCreationService.mergeCopiedLine(existing, copied);

        assertThat(existing.getAmount()).isEqualByComparingTo("0.53");
        assertThat(existing.getContractedRate()).isEqualByComparingTo("10");
    }

    @Test
    void mergeCopiedLine_shouldUpdateContractedRate_whenCopyCarriesSplitComponent() {
        ChargeLineItem existing = new ChargeLineItem();
        existing.setAmount(new BigDecimal("26.67"));
        existing.setContractedRate(new BigDecimal("12"));

        ChargeLineItem copied = new ChargeLineItem();
        copied.setAmount(new BigDecimal("26.67"));
        copied.setContractedRate(new BigDecimal("45"));
        copied.setPreTaxableAmount(new BigDecimal("26.67"));

        EstimateChargeCreationService.mergeCopiedLine(existing, copied);

        assertThat(existing.getAmount()).isEqualByComparingTo("26.67");
        assertThat(existing.getContractedRate()).isEqualByComparingTo("45");
    }

    @Test
    void createOrUpdateEstimateCharge_shouldUseCopyPath_forDtc() {
        UUID chargeId = UUID.randomUUID();
        Charge charge = CalculatorTestDataFactory.charge(chargeId, shipment, BatchProcessingConstants.CHARGE_CODE_DTC);
        ChargeLineItem existing = CalculatorTestDataFactory.chargeLineItem(charge, account, "DUTY", BigDecimal.ONE);
        ChargeLineItem copied = CalculatorTestDataFactory.chargeLineItem(charge, account, "DUTY", BigDecimal.TEN);
        copied.setContractedRate(BigDecimal.valueOf(5));

        when(chargeLineCopySupport.isCopyDerivedCharge(BatchProcessingConstants.CHARGE_CODE_DTC)).thenReturn(true);
        when(entityRepository.findByCodeIgnoreCase("DXB"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));
        when(chargeRepository.findByShipment_IdAndChargeCode(
                shipment.getId(), BatchProcessingConstants.CHARGE_CODE_DTC))
                .thenReturn(Optional.of(charge));
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chargeLineItemRepository.findByCharge_Id(chargeId))
                .thenReturn(List.of(existing))
                .thenReturn(List.of(existing));
        when(chargeLineSupport.resolveAccountsForDtcProcessing(shipment, "DXB", List.of(existing)))
                .thenReturn(List.of(account));
        when(chargeLineCopySupport.copyToTargetCharge(
                any(), eq(shipment), eq(BatchProcessingConstants.CHARGE_CODE_DTC), eq("DXB"),
                eq(charge), eq(List.of(account)), any(), any()))
                .thenReturn(List.of(copied));

        service.createOrUpdateEstimateCharge(context, BatchProcessingConstants.CHARGE_CODE_DTC, "DXB");

        assertThat(existing.getAmount()).isEqualByComparingTo("10");
        assertThat(existing.getContractedRate()).isEqualByComparingTo("5");
        verify(chargeLineItemRepository).saveAll(any());
        verify(chargeLineCopySupport).copyToTargetCharge(
                any(), eq(shipment), eq(BatchProcessingConstants.CHARGE_CODE_DTC), eq("DXB"),
                eq(charge), eq(List.of(account)), any(), any());
    }

    @Test
    void createOrUpdateEstimateChargeFromCopy_shouldCreateChargeAndZeroAmounts_whenExempted() {
        shipment.setCategoryType(BatchProcessingConstants.CATEGORY_TYPE_EXEMPTED);
        when(chargeLineCopySupport.isCopyDerivedCharge(BatchProcessingConstants.CHARGE_CODE_DTS)).thenReturn(true);
        when(entityRepository.findByCodeIgnoreCase("DXB"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));
        when(chargeRepository.findByShipment_IdAndChargeCode(
                shipment.getId(), BatchProcessingConstants.CHARGE_CODE_DTS))
                .thenReturn(Optional.empty());
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> {
            Charge saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });
        when(chargeLineItemRepository.findByCharge_Id(any())).thenReturn(List.of());
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_DTS))
                .thenReturn(List.of(account));
        when(chargeLineCopySupport.copyToTargetCharge(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        service.createOrUpdateEstimateCharge(context, BatchProcessingConstants.CHARGE_CODE_DTS, "DXB");

        ArgumentCaptor<Charge> chargeCaptor = ArgumentCaptor.forClass(Charge.class);
        verify(chargeRepository, atLeast(1)).save(chargeCaptor.capture());
        Charge saved = chargeCaptor.getAllValues().get(chargeCaptor.getAllValues().size() - 1);
        assertThat(saved.getEstimateAmount()).isEqualByComparingTo("0");
        assertThat(saved.getOutstandingAmount()).isEqualByComparingTo("0");
    }

    @Test
    void createOrUpdateEstimateCharge_shouldDeleteOrphanLines() {
        UUID chargeId = UUID.randomUUID();
        Charge charge = CalculatorTestDataFactory.charge(chargeId, shipment, "DTC");
        ServiceChargeResponse active = CalculatorTestDataFactory.serviceCharge("SC-1", "active", "C", null, true);
        ChargeLineItem orphan = CalculatorTestDataFactory.chargeLineItem(charge, account, "OLD", BigDecimal.ONE);

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", "DTC")).thenReturn(List.of(account));
        when(entityRepository.findByCodeIgnoreCase("DXB"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), "DTC"))
                .thenReturn(Optional.of(charge));
        when(chargeLineItemRepository.findByCharge_Id(chargeId))
                .thenReturn(List.of(orphan))
                .thenReturn(List.of());
        when(chargeLineSupport.resolveServiceCharges("DXB", account, "DTC")).thenReturn(List.of(active));

        service.createOrUpdateEstimateCharge(context, "DTC");

        rowChargeWriteBuffer.flush();
        verify(chargeLineItemRepository).deleteAllById(List.of(orphan.getId()));
    }

    private Charge captureFlushedChargeUpdate() {
        rowChargeWriteBuffer.flush();
        ArgumentCaptor<List<Charge>> chargeCaptor = ArgumentCaptor.forClass(List.class);
        verify(chargeRepository).saveAll(chargeCaptor.capture());
        return chargeCaptor.getValue().get(0);
    }
}
