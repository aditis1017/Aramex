package com.customcharge.chargescalculator.service.charge;



import com.customcharge.chargescalculator.constants.BatchProcessingConstants;

import com.customcharge.chargescalculator.constants.ChargeCreationConstants;

import com.customcharge.chargescalculator.model.ChargeCalculationContext;

import com.customcharge.chargescalculator.service.batch.RecordProcessingLookups;
import com.customcharge.chargescalculator.service.batch.RecordProcessingLookupsTestSupport;
import com.customcharge.chargescalculator.service.batch.RowChargeWriteBuffer;

import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.chargescalculator.support.ChargeHeaderAmountTestSupport;

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

import org.mockito.InjectMocks;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;



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

import static org.mockito.Mockito.lenient;

import static org.mockito.Mockito.never;

import static org.mockito.Mockito.times;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;



@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActualChargeCreationServiceTest {

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
    private ActualChargeAmountPropagationService actualChargeAmountPropagationService;

    private ActualChargeCreationService service;
    private RowChargeWriteBuffer rowChargeWriteBuffer;

    private Shipment shipment;

    private ChargeCalculationContext context;

    private Account account;



    @BeforeEach

    void setUp() {

        shipment = CalculatorTestDataFactory.shipment();

        context = CalculatorTestDataFactory.chargeContext(

                shipment,

                BatchProcessingConstants.PROCESS_FOR_ACTUAL,

                CalculatorTestDataFactory.actualRecordMap());

        account = CalculatorTestDataFactory.account(UUID.randomUUID(), BatchProcessingConstants.CHARGE_CODE_ECD);

        RecordProcessingLookupsTestSupport.delegateToRepositories(
                recordProcessingLookups, chargeRepository, chargeLineItemRepository, entityRepository);
        rowChargeWriteBuffer = new RowChargeWriteBuffer(chargeRepository, chargeLineItemRepository);
        rowChargeWriteBuffer.open();
        service = new ActualChargeCreationService(
                chargeLineSupport,
                chargeRepository,
                chargeLineItemRepository,
                recordProcessingLookups,
                dtaContextSupport,
                actualChargeAmountPropagationService,
                rowChargeWriteBuffer);

        lenient().when(chargeLineSupport.formatAmount(any(BigDecimal.class), any(Charge.class)))

                .thenAnswer(invocation -> invocation.getArgument(0));

        lenient().when(dtaContextSupport.forCharge(any(), any(), any(), any(), any()))

                .thenReturn(new DtaContextSupport.DtaLineContext(Map.of(), false, false, null));
        lenient().doNothing().when(chargeLineSupport).applyChargeCurrency(any(), any(), any(), any(), any(Boolean.class));
        lenient().doNothing().when(actualChargeAmountPropagationService).propagateAfterEcdActualUpload(any());
        ChargeHeaderAmountTestSupport.stubApplyChargeHeaderAmounts(chargeLineSupport);
    }

    @AfterEach
    void tearDown() {
        rowChargeWriteBuffer.close();

    }



    @Test

    void createOrUpdateActualCharge_shouldThrow_whenSaddadMissing() {

        ChargeCalculationContext missingSaddad = CalculatorTestDataFactory.chargeContext(

                BatchProcessingConstants.PROCESS_FOR_ACTUAL,

                CalculatorTestDataFactory.recordMap());



        assertThatThrownBy(() -> service.createOrUpdateActualCharge(missingSaddad, BatchProcessingConstants.CHARGE_CODE_ECD))

                .isInstanceOf(ValidationException.class)

                .hasMessageContaining(BatchProcessingConstants.paymentReferenceLabel());

    }



    @Test

    void createOrUpdateActualCharge_shouldSaveLinesAndUpdateTotals() {

        UUID chargeId = UUID.randomUUID();

        Charge charge = CalculatorTestDataFactory.charge(chargeId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);

        charge.setEstimateAmount(BigDecimal.TEN);

        ChargeLineItem newLine = CalculatorTestDataFactory.chargeLineItem(charge, account, "SC2", BigDecimal.valueOf(4));
        newLine.setPaymentReference("SAD-001");

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");

        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_ECD)).thenReturn(List.of(account));

        when(entityRepository.findByCodeIgnoreCase("DXB"))

                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));

        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD)).thenReturn(Optional.of(charge));

        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(chargeRepository.findByIdForUpdate(chargeId)).thenReturn(Optional.of(charge));

        when(chargeLineItemRepository.findByChargeIdAndPaymentReferenceNormalized(chargeId, "SAD-001"))

                .thenReturn(List.of());

        when(chargeLineSupport.buildLineItemsForCharge(
                any(), any(), any(), any(), any(), any(), any(), any(), eq(true), any()))
                .thenReturn(List.of(newLine));

        when(chargeLineItemRepository.findByCharge_Id(chargeId))
                .thenReturn(List.of())
                .thenReturn(List.of());

        service.createOrUpdateActualCharge(context, BatchProcessingConstants.CHARGE_CODE_ECD);



        verify(chargeLineItemRepository).saveAll(List.of(newLine));

        verify(chargeRepository, never()).save(charge);

        assertThat(charge.getActualAmount()).isEqualByComparingTo(BigDecimal.valueOf(4));
        assertThat(charge.getOutstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(charge.getEstimateAmount()).isEqualByComparingTo(BigDecimal.valueOf(4));

    }



    @Test

    void createOrUpdateActualCharge_shouldDelegateToFourArgOverload() {

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");

        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_ECD)).thenReturn(List.of());

        assertThatThrownBy(() -> service.createOrUpdateActualCharge(context, BatchProcessingConstants.CHARGE_CODE_ECD))

                .isInstanceOf(ValidationException.class)

                .hasMessageContaining(ChargeCreationConstants.NO_ACCOUNTS_FOR_CHARGE);

        verify(chargeLineSupport).resolveDestinationEntityCode(shipment);

    }



    @Test

    void createOrUpdateActualCharge_shouldThrow_whenEntityCodeMissing() {

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn(" ");

        assertThatThrownBy(() -> service.createOrUpdateActualCharge(context, BatchProcessingConstants.CHARGE_CODE_ECD))

                .isInstanceOf(ValidationException.class)

                .hasMessage(ChargeCreationConstants.MISSING_ENTITY_CODE);

    }



    @Test

    void createOrUpdateActualCharge_shouldThrow_whenNoAccounts() {

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");

        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_ECD)).thenReturn(List.of());

        assertThatThrownBy(() -> service.createOrUpdateActualCharge(context, BatchProcessingConstants.CHARGE_CODE_ECD))

                .isInstanceOf(ValidationException.class)

                .hasMessageContaining(ChargeCreationConstants.NO_ACCOUNTS_FOR_CHARGE);

    }



    @Test

    void createOrUpdateActualCharge_shouldCreateCharge_whenMissing() {

        UUID chargeId = UUID.randomUUID();

        Charge savedCharge = CalculatorTestDataFactory.charge(chargeId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);

        ChargeLineItem newLine = CalculatorTestDataFactory.chargeLineItem(savedCharge, account, "SC1", BigDecimal.ONE);



        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");

        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_ECD)).thenReturn(List.of(account));

        when(entityRepository.findByCodeIgnoreCase("DXB"))

                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));

        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD)).thenReturn(Optional.empty());

        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> {

            Charge created = invocation.getArgument(0);

            ReflectionTestUtils.setField(created, "id", chargeId);

            return created;

        });

        lenient().when(chargeRepository.findByIdForUpdate(chargeId)).thenReturn(Optional.of(savedCharge));

        when(chargeLineItemRepository.findByChargeIdAndPaymentReferenceNormalized(chargeId, "SAD-001"))

                .thenReturn(List.of());

        when(dtaContextSupport.forCharge(any(), any(), any(), any(), any()))

                .thenReturn(new DtaContextSupport.DtaLineContext(Map.of(), false, false, null));

        when(chargeLineSupport.buildLineItemsForCharge(

                any(), any(), any(), any(), any(), any(), any(), any(), eq(true), any()))

                .thenReturn(List.of(newLine));

        when(chargeLineItemRepository.findByCharge_Id(chargeId))
                .thenReturn(List.of())
                .thenReturn(List.of());

        service.createOrUpdateActualCharge(context, BatchProcessingConstants.CHARGE_CODE_ECD);



        verify(chargeRepository, times(1)).save(any(Charge.class));

        verify(chargeLineItemRepository).saveAll(List.of(newLine));

    }



    @Test

    void createOrUpdateActualCharge_shouldRejectDuplicatePaymentReference() {

        UUID chargeId = UUID.randomUUID();

        Charge charge = CalculatorTestDataFactory.charge(chargeId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);



        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");

        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_ECD)).thenReturn(List.of(account));

        when(entityRepository.findByCodeIgnoreCase("DXB"))

                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));

        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD)).thenReturn(Optional.of(charge));

        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(chargeRepository.findByIdForUpdate(chargeId)).thenReturn(Optional.of(charge));

        when(chargeLineItemRepository.findByCharge_Id(chargeId))
                .thenReturn(List.of(existingLineWithPaymentRef(charge, account, "SAD-001")));

        assertThatThrownBy(() -> service.createOrUpdateActualCharge(context, BatchProcessingConstants.CHARGE_CODE_ECD))

                .isInstanceOf(ValidationException.class)

                .hasMessageContaining(BatchProcessingConstants.ERROR_ACTUAL_PAYMENT_REFERENCE_ALREADY_EXISTS);

    }



    @Test

    void createOrUpdateActualCharge_shouldRejectDuplicateDeclarationNumberGlobally() {

        UUID chargeId = UUID.randomUUID();

        Charge charge = CalculatorTestDataFactory.charge(chargeId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);

        Map<String, String> source = CalculatorTestDataFactory.actualRecordMap();
        source.put(BatchProcessingConstants.SOURCE_KEY_DECLARATION_NUMBER, "BOE-100");
        context = CalculatorTestDataFactory.chargeContext(
                shipment, BatchProcessingConstants.PROCESS_FOR_ACTUAL, source);

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");

        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_ECD)).thenReturn(List.of(account));

        when(entityRepository.findByCodeIgnoreCase("DXB"))

                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));

        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD)).thenReturn(Optional.of(charge));

        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(chargeRepository.findByIdForUpdate(chargeId)).thenReturn(Optional.of(charge));

        when(chargeLineItemRepository.findByChargeIdAndPaymentReferenceNormalized(chargeId, "SAD-001"))

                .thenReturn(List.of());

        when(chargeLineItemRepository.existsByDeclarationNumberNormalized("BOE-100")).thenReturn(true);

        assertThatThrownBy(() -> service.createOrUpdateActualCharge(context, BatchProcessingConstants.CHARGE_CODE_ECD))

                .isInstanceOf(ValidationException.class)

                .hasMessage(BatchProcessingConstants.ERROR_ACTUAL_DECLARATION_NUMBER_ALREADY_EXISTS);

    }



    @Test

    void createOrUpdateActualCharge_shouldSkipDeclarationDuplicateCheck_whenDeclarationBlank() {

        UUID chargeId = UUID.randomUUID();

        Charge charge = CalculatorTestDataFactory.charge(chargeId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);

        ChargeLineItem newLine = CalculatorTestDataFactory.chargeLineItem(charge, account, "SC1", BigDecimal.ONE);

        Map<String, String> source = CalculatorTestDataFactory.actualRecordMap();
        source.put(BatchProcessingConstants.SOURCE_KEY_DECLARATION_NUMBER, "  ");
        context = CalculatorTestDataFactory.chargeContext(
                shipment, BatchProcessingConstants.PROCESS_FOR_ACTUAL, source);

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");

        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_ECD)).thenReturn(List.of(account));

        when(entityRepository.findByCodeIgnoreCase("DXB"))

                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));

        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD)).thenReturn(Optional.of(charge));

        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(chargeRepository.findByIdForUpdate(chargeId)).thenReturn(Optional.of(charge));

        when(chargeLineItemRepository.findByChargeIdAndPaymentReferenceNormalized(chargeId, "SAD-001"))

                .thenReturn(List.of());

        when(chargeLineSupport.buildLineItemsForCharge(

                any(), any(), any(), any(), any(), any(), any(), any(), eq(true), any()))

                .thenReturn(List.of(newLine));

        when(chargeLineItemRepository.findByCharge_Id(chargeId)).thenReturn(List.of(newLine));

        service.createOrUpdateActualCharge(context, BatchProcessingConstants.CHARGE_CODE_ECD);

        verify(chargeLineItemRepository, never()).existsByDeclarationNumberNormalized(any());
        verify(chargeLineItemRepository).saveAll(List.of(newLine));

    }



    @Test

    void createOrUpdateActualCharge_shouldUseZeroTotals_whenExemptedCategory() {

        UUID chargeId = UUID.randomUUID();

        shipment.setCategoryType(BatchProcessingConstants.CATEGORY_TYPE_EXEMPTED);

        Charge charge = CalculatorTestDataFactory.charge(chargeId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);

        ChargeLineItem line = CalculatorTestDataFactory.chargeLineItem(charge, account, "SC1", BigDecimal.ZERO);



        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");

        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_ECD)).thenReturn(List.of(account));

        when(entityRepository.findByCodeIgnoreCase("DXB"))

                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));

        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD)).thenReturn(Optional.of(charge));

        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(chargeRepository.findByIdForUpdate(chargeId)).thenReturn(Optional.of(charge));

        when(chargeLineItemRepository.findByChargeIdAndPaymentReferenceNormalized(chargeId, "SAD-001"))

                .thenReturn(List.of());

        when(dtaContextSupport.forCharge(any(), any(), any(), any(), any()))

                .thenReturn(new DtaContextSupport.DtaLineContext(Map.of(), false, false, null));

        when(chargeLineSupport.buildLineItemsForCharge(

                any(), any(), any(), any(), any(), any(), any(), any(), eq(true), any()))

                .thenReturn(List.of(line));

        when(chargeLineItemRepository.findByCharge_Id(chargeId))
                .thenReturn(List.of())
                .thenReturn(List.of());

        service.createOrUpdateActualCharge(context, BatchProcessingConstants.CHARGE_CODE_ECD);



        verify(chargeLineItemRepository, times(2)).findByCharge_Id(chargeId);

        verify(chargeRepository, never()).save(charge);

    }



    @Test
    void createOrUpdateActualCharge_shouldPropagateAmountsAfterEcdUpload() {
        UUID chargeId = UUID.randomUUID();
        Charge charge = CalculatorTestDataFactory.charge(chargeId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);
        ChargeLineItem line = CalculatorTestDataFactory.chargeLineItem(charge, account, "SC1", BigDecimal.ONE);

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_ECD))
                .thenReturn(List.of(account));
        when(entityRepository.findByCodeIgnoreCase("DXB"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD))
                .thenReturn(Optional.of(charge));
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chargeRepository.findByIdForUpdate(chargeId)).thenReturn(Optional.of(charge));
        when(chargeLineItemRepository.findByChargeIdAndPaymentReferenceNormalized(chargeId, "SAD-001"))
                .thenReturn(List.of());
        when(chargeLineSupport.buildLineItemsForCharge(
                any(), any(), any(), any(), any(), any(), any(), any(), eq(true), any()))
                .thenReturn(List.of(line));
        when(chargeLineItemRepository.findByCharge_Id(chargeId))
                .thenReturn(List.of())
                .thenReturn(List.of());

        service.createOrUpdateActualCharge(context, BatchProcessingConstants.CHARGE_CODE_ECD);

        verify(actualChargeAmountPropagationService).propagateAfterEcdActualUpload(context);
    }

    @Test
    void createOrUpdateActualCharge_shouldSetEcdOutstandingToZero() {
        UUID chargeId = UUID.randomUUID();
        Charge charge = CalculatorTestDataFactory.charge(chargeId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);
        charge.setEstimateAmount(new BigDecimal("20"));
        ChargeLineItem line = CalculatorTestDataFactory.chargeLineItem(charge, account, "SC1", new BigDecimal("8"));
        line.setPaymentReference("SAD-001");

        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_ECD))
                .thenReturn(List.of(account));
        when(entityRepository.findByCodeIgnoreCase("DXB"))
                .thenReturn(Optional.of(CalculatorTestDataFactory.billingEntity("DXB")));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD))
                .thenReturn(Optional.of(charge));
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chargeRepository.findByIdForUpdate(chargeId)).thenReturn(Optional.of(charge));
        when(chargeLineItemRepository.findByChargeIdAndPaymentReferenceNormalized(chargeId, "SAD-001"))
                .thenReturn(List.of());
        when(dtaContextSupport.forCharge(any(), any(), any(), any(), any()))
                .thenReturn(new DtaContextSupport.DtaLineContext(Map.of(), false, false, null));
        when(chargeLineSupport.buildLineItemsForCharge(
                any(), any(), any(), any(), any(), any(), any(), any(), eq(true), any()))
                .thenReturn(List.of(line));
        when(chargeLineItemRepository.findByCharge_Id(chargeId))
                .thenReturn(List.of())
                .thenReturn(List.of());

        service.createOrUpdateActualCharge(context, BatchProcessingConstants.CHARGE_CODE_ECD);

        assertThat(charge.getActualAmount()).isEqualByComparingTo("8");
        assertThat(charge.getOutstandingAmount()).isEqualByComparingTo("0");
        assertThat(charge.getEstimateAmount()).isEqualByComparingTo("8");
    }

    private static ChargeLineItem existingLineWithPaymentRef(Charge charge, Account account, String paymentReference) {
        ChargeLineItem line = CalculatorTestDataFactory.chargeLineItem(charge, account, "SC1", BigDecimal.ONE);
        line.setPaymentReference(paymentReference);
        return line;
    }

}


