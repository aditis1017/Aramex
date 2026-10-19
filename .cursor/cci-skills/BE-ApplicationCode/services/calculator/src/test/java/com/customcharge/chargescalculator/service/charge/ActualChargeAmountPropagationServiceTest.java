package com.customcharge.chargescalculator.service.charge;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.service.batch.RecordProcessingLookups;
import com.customcharge.chargescalculator.service.batch.RecordProcessingLookupsTestSupport;
import com.customcharge.chargescalculator.service.batch.RowChargeWriteBuffer;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.chargescalculator.support.ChargeHeaderAmountTestSupport;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService.CopyAmountFxResult;
import com.customcharge.persistencelayer.dto.ServiceChargeDescriptionMappingDto;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActualChargeAmountPropagationServiceTest {

    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private ChargeLineItemRepository chargeLineItemRepository;
    @Mock
    private RecordProcessingLookups recordProcessingLookups;
    @Mock
    private ChargeLineSupport chargeLineSupport;
    @Mock
    private ServiceChargeMappingRepository serviceChargeMappingRepository;
    @Mock
    private ChargeLineCopyService chargeLineCopyService;

    private ActualChargeAmountPropagationService service;
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
                recordProcessingLookups, chargeRepository, chargeLineItemRepository, null);
        rowChargeWriteBuffer = new RowChargeWriteBuffer(chargeRepository, chargeLineItemRepository);
        rowChargeWriteBuffer.open();
        service = new ActualChargeAmountPropagationService(
                chargeRepository,
                chargeLineItemRepository,
                recordProcessingLookups,
                chargeLineSupport,
                serviceChargeMappingRepository,
                chargeLineCopyService,
                rowChargeWriteBuffer);
        lenient().when(chargeLineSupport.formatAmount(any(BigDecimal.class), any(Charge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(chargeLineSupport.matchesPaymentType(shipment, BatchProcessingConstants.PAYMENT_TYPE_PREPAID))
                .thenReturn(true);
        lenient().when(chargeLineSupport.matchesPaymentType(shipment, BatchProcessingConstants.PAYMENT_TYPE_THIRD_PARTY))
                .thenReturn(false);
        lenient().when(chargeLineCopyService.convertCopyAmount(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Boolean.class)))
                .thenAnswer(invocation -> new CopyAmountFxResult(invocation.getArgument(0), 1.0));
        lenient().when(chargeLineSupport.resolveTaxPercentForServiceCode(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().doAnswer(invocation -> {
            ChargeLineItem line = invocation.getArgument(0);
            BigDecimal preTax = invocation.getArgument(1);
            line.setAmount(preTax);
            line.setPreTaxableAmount(preTax);
            return null;
        }).when(chargeLineSupport).applyTaxedLineAmounts(any(), any(), any(), any());
        ChargeHeaderAmountTestSupport.stubApplyChargeHeaderAmounts(chargeLineSupport);
    }

    @AfterEach
    void tearDown() {
        rowChargeWriteBuffer.close();
    }

    @Test
    void propagateAfterEcdActualUpload_shouldUpdateDtaFromEcdActualAmounts() {
        UUID ecdId = UUID.randomUUID();
        UUID dtaId = UUID.randomUUID();
        Charge ecdCharge = CalculatorTestDataFactory.charge(ecdId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);
        Charge dtaCharge = CalculatorTestDataFactory.charge(dtaId, shipment, BatchProcessingConstants.CHARGE_CODE_DTA);
        ServiceChargeResponse activeService = serviceCharge("SC1", "Import Customs");

        ChargeLineItem ecdActual = lineWithPaymentRef(ecdCharge, account, "SC1", "Import Customs", BigDecimal.valueOf(7));
        ChargeLineItem ecdEstimate = CalculatorTestDataFactory.chargeLineItem(
                ecdCharge, account, "SC1", BigDecimal.valueOf(3));
        ChargeLineItem dtaLine = CalculatorTestDataFactory.chargeLineItem(
                dtaCharge, account, "SC1", BigDecimal.ZERO);
        dtaLine.setServiceCodeDescription("Import Customs");

        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD))
                .thenReturn(Optional.of(ecdCharge));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(Optional.of(dtaCharge));
        when(chargeLineItemRepository.findByCharge_Id(ecdId)).thenReturn(List.of(ecdActual, ecdEstimate));
        when(chargeLineItemRepository.findByCharge_Id(dtaId)).thenReturn(List.of(dtaLine));
        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(List.of(account));
        when(chargeLineSupport.resolveServiceCharges("DXB", account, BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(List.of(activeService));
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.propagateAfterEcdActualUpload(context);

        assertThat(dtaLine.getAmount()).isEqualByComparingTo("7");
        assertThat(dtaLine.getPreTaxableAmount()).isEqualByComparingTo("7");
        assertThat(dtaCharge.getEstimateAmount()).isEqualByComparingTo("7");
        assertThat(dtaCharge.getActualAmount()).isEqualByComparingTo("0");
        assertThat(dtaCharge.getOutstandingAmount()).isEqualByComparingTo("7");
        verify(chargeLineItemRepository).saveAll(List.of(dtaLine));
        verify(chargeLineItemRepository, never()).deleteAllById(any());
    }

    @Test
    void propagateAfterEcdActualUpload_shouldRemoveInactiveServiceCodeFromDta() {
        UUID ecdId = UUID.randomUUID();
        UUID dtaId = UUID.randomUUID();
        Charge ecdCharge = CalculatorTestDataFactory.charge(ecdId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);
        Charge dtaCharge = CalculatorTestDataFactory.charge(dtaId, shipment, BatchProcessingConstants.CHARGE_CODE_DTA);
        ServiceChargeResponse activeService = serviceCharge("SC1", "Import Customs");

        ChargeLineItem ecdActual = lineWithPaymentRef(ecdCharge, account, "SC1", "Import Customs", BigDecimal.valueOf(7));
        ChargeLineItem activeDtaLine = CalculatorTestDataFactory.chargeLineItem(
                dtaCharge, account, "SC1", BigDecimal.ZERO);
        activeDtaLine.setServiceCodeDescription("Import Customs");
        ChargeLineItem inactiveDtaLine = CalculatorTestDataFactory.chargeLineItem(
                dtaCharge, account, "SC2", BigDecimal.valueOf(4));
        inactiveDtaLine.setServiceCodeDescription("Handling Fee");

        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD))
                .thenReturn(Optional.of(ecdCharge));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(Optional.of(dtaCharge));
        when(chargeLineItemRepository.findByCharge_Id(ecdId)).thenReturn(List.of(ecdActual));
        when(chargeLineItemRepository.findByCharge_Id(dtaId))
                .thenReturn(List.of(activeDtaLine, inactiveDtaLine))
                .thenReturn(List.of(activeDtaLine));
        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(List.of(account));
        when(chargeLineSupport.resolveServiceCharges("DXB", account, BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(List.of(activeService));
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.propagateAfterEcdActualUpload(context);

        rowChargeWriteBuffer.flush();
        verify(chargeLineItemRepository).deleteAllById(List.of(inactiveDtaLine.getId()));
        assertThat(activeDtaLine.getAmount()).isEqualByComparingTo("7");
    }

    @Test
    void propagateAfterEcdActualUpload_shouldCreateNewActiveServiceCodeOnDta() {
        UUID ecdId = UUID.randomUUID();
        UUID dtaId = UUID.randomUUID();
        Charge ecdCharge = CalculatorTestDataFactory.charge(ecdId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);
        Charge dtaCharge = CalculatorTestDataFactory.charge(dtaId, shipment, BatchProcessingConstants.CHARGE_CODE_DTA);
        ServiceChargeResponse newActiveService = serviceCharge("SC3", "New Charge");

        ChargeLineItem ecdActual = lineWithPaymentRef(ecdCharge, account, "SC3", "New Charge", BigDecimal.valueOf(9));

        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD))
                .thenReturn(Optional.of(ecdCharge));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(Optional.of(dtaCharge));
        when(chargeLineItemRepository.findByCharge_Id(ecdId)).thenReturn(List.of(ecdActual));
        when(chargeLineItemRepository.findByCharge_Id(dtaId)).thenReturn(List.of()).thenReturn(List.of());
        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(List.of(account));
        when(chargeLineSupport.resolveServiceCharges("DXB", account, BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(List.of(newActiveService));
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChargeLineItem>> savedCaptor = ArgumentCaptor.forClass(List.class);
        service.propagateAfterEcdActualUpload(context);

        verify(chargeLineItemRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).hasSize(1);
        ChargeLineItem createdLine = savedCaptor.getValue().get(0);
        assertThat(createdLine.getServiceCode()).isEqualTo("SC3");
        assertThat(createdLine.getServiceCodeDescription()).isEqualTo("New Charge");
        assertThat(createdLine.getAmount()).isEqualByComparingTo("9");
        assertThat(createdLine.getCharge()).isSameAs(dtaCharge);
    }

    @Test
    void propagateAfterEcdActualUpload_shouldUpdateEcsFromDtaEffectiveAmount() {
        UUID ecdId = UUID.randomUUID();
        UUID dtaId = UUID.randomUUID();
        UUID ecsId = UUID.randomUUID();
        Charge ecdCharge = CalculatorTestDataFactory.charge(ecdId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);
        Charge dtaCharge = CalculatorTestDataFactory.charge(dtaId, shipment, BatchProcessingConstants.CHARGE_CODE_DTA);
        Charge ecsCharge = CalculatorTestDataFactory.charge(ecsId, shipment, BatchProcessingConstants.CHARGE_CODE_ECS);
        ServiceChargeResponse ecsService = serviceCharge("ECS1", "ECS Import");

        ChargeLineItem dtaLine = CalculatorTestDataFactory.chargeLineItem(dtaCharge, account, "SC1", BigDecimal.valueOf(2));
        dtaLine.setServiceCodeDescription("Import Customs");
        dtaLine.setContractedRate(BigDecimal.valueOf(5));
        ChargeLineItem ecsLine = CalculatorTestDataFactory.chargeLineItem(ecsCharge, account, "ECS1", BigDecimal.ZERO);
        ecsLine.setServiceCode("ECS1");
        ecsLine.setServiceCodeDescription("ECS Import");

        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD))
                .thenReturn(Optional.of(ecdCharge));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(Optional.of(dtaCharge));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECS))
                .thenReturn(Optional.of(ecsCharge));
        when(chargeLineItemRepository.findByCharge_Id(ecdId)).thenReturn(List.of());
        when(chargeLineItemRepository.findByCharge_Id(dtaId)).thenReturn(List.of(dtaLine));
        when(chargeLineItemRepository.findByCharge_Id(ecsId)).thenReturn(List.of(ecsLine));
        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveOriginEntityCode(shipment)).thenReturn("RUH");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(List.of(account));
        when(chargeLineSupport.resolveAccounts(shipment, "RUH", BatchProcessingConstants.CHARGE_CODE_ECS))
                .thenReturn(List.of(account));
        when(chargeLineSupport.resolveServiceCharges("DXB", account, BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(List.of(serviceCharge("SC1", "Import Customs")));
        when(chargeLineSupport.resolveServiceCharges("RUH", account, BatchProcessingConstants.CHARGE_CODE_ECS))
                .thenReturn(List.of(ecsService));
        when(serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                eq("DXB"), eq("RUH"), eq(BatchProcessingConstants.CHARGE_CODE_ECS)))
                .thenReturn(List.of(new ServiceChargeDescriptionMappingDto(
                        "Import Customs", "SC1", "ECS Import", "ECS1")));
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.propagateAfterEcdActualUpload(context);

        assertThat(ecsLine.getAmount()).isEqualByComparingTo("5");
        assertThat(ecsCharge.getEstimateAmount()).isEqualByComparingTo("5");
        assertThat(ecsCharge.getActualAmount()).isEqualByComparingTo("0");
        assertThat(ecsCharge.getOutstandingAmount()).isEqualByComparingTo("0");
    }

    @Test
    void propagateAfterEcdActualUpload_shouldPropagateToDtc_forDduShipment() {
        shipment.setShipmentType(BatchProcessingConstants.SHIPMENT_TYPE_DDU);
        UUID ecdId = UUID.randomUUID();
        UUID dtcId = UUID.randomUUID();
        Charge ecdCharge = CalculatorTestDataFactory.charge(ecdId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);
        Charge dtcCharge = CalculatorTestDataFactory.charge(dtcId, shipment, BatchProcessingConstants.CHARGE_CODE_DTC);
        ServiceChargeResponse activeService = serviceCharge("SC1", "Import Customs");
        ChargeLineItem ecdActual = lineWithPaymentRef(ecdCharge, account, "SC1", "Import Customs", BigDecimal.valueOf(8));
        ChargeLineItem dtcLine = CalculatorTestDataFactory.chargeLineItem(dtcCharge, account, "SC1", BigDecimal.ZERO);
        dtcLine.setServiceCodeDescription("Import Customs");

        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD))
                .thenReturn(Optional.of(ecdCharge));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_DTC))
                .thenReturn(Optional.of(dtcCharge));
        when(chargeLineItemRepository.findByCharge_Id(ecdId)).thenReturn(List.of(ecdActual));
        when(chargeLineItemRepository.findByCharge_Id(dtcId)).thenReturn(List.of(dtcLine));
        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveAccountsForDtcProcessing(shipment, "DXB", List.of(dtcLine)))
                .thenReturn(List.of(account));
        when(chargeLineSupport.resolveServiceCharges("DXB", account, BatchProcessingConstants.CHARGE_CODE_DTC))
                .thenReturn(List.of(activeService));
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.propagateAfterEcdActualUpload(context);

        assertThat(dtcLine.getAmount()).isEqualByComparingTo("8");
    }

    @Test
    void propagateAfterEcdActualUpload_shouldPropagateDtsFromEcs_forDdpPrepaid() {
        shipment.setShipmentType(BatchProcessingConstants.SHIPMENT_TYPE_DDP);
        shipment.setPaymentType(BatchProcessingConstants.PAYMENT_TYPE_PREPAID);
        UUID ecdId = UUID.randomUUID();
        UUID dtaId = UUID.randomUUID();
        UUID ecsId = UUID.randomUUID();
        UUID dtsId = UUID.randomUUID();
        Charge ecdCharge = CalculatorTestDataFactory.charge(ecdId, shipment, BatchProcessingConstants.CHARGE_CODE_ECD);
        Charge dtaCharge = CalculatorTestDataFactory.charge(dtaId, shipment, BatchProcessingConstants.CHARGE_CODE_DTA);
        Charge ecsCharge = CalculatorTestDataFactory.charge(ecsId, shipment, BatchProcessingConstants.CHARGE_CODE_ECS);
        Charge dtsCharge = CalculatorTestDataFactory.charge(dtsId, shipment, BatchProcessingConstants.CHARGE_CODE_DTS);
        ChargeLineItem ecdActual = lineWithPaymentRef(ecdCharge, account, "SC1", "Import Customs", BigDecimal.ONE);
        ChargeLineItem dtaLine = CalculatorTestDataFactory.chargeLineItem(dtaCharge, account, "SC1", BigDecimal.ONE);
        dtaLine.setServiceCodeDescription("Import Customs");
        ChargeLineItem ecsLine = CalculatorTestDataFactory.chargeLineItem(ecsCharge, account, "ECS1", BigDecimal.valueOf(9));
        ecsLine.setServiceCodeDescription("Freight Cost");
        ChargeLineItem dtsLine = CalculatorTestDataFactory.chargeLineItem(dtsCharge, account, "DTS1", BigDecimal.ZERO);
        dtsLine.setServiceCodeDescription("Freight Cost");

        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD))
                .thenReturn(Optional.of(ecdCharge));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(Optional.of(dtaCharge));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECS))
                .thenReturn(Optional.of(ecsCharge));
        when(chargeRepository.findByShipment_IdAndChargeCode(shipment.getId(), BatchProcessingConstants.CHARGE_CODE_DTS))
                .thenReturn(Optional.of(dtsCharge));
        when(chargeLineItemRepository.findByCharge_Id(ecdId)).thenReturn(List.of(ecdActual));
        when(chargeLineItemRepository.findByCharge_Id(dtaId)).thenReturn(List.of(dtaLine));
        when(chargeLineItemRepository.findByCharge_Id(ecsId)).thenReturn(List.of(ecsLine));
        when(chargeLineItemRepository.findByCharge_Id(dtsId)).thenReturn(List.of(dtsLine));
        when(chargeLineSupport.resolveDestinationEntityCode(shipment)).thenReturn("DXB");
        when(chargeLineSupport.resolveOriginEntityCode(shipment)).thenReturn("RUH");
        when(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(List.of(account));
        when(chargeLineSupport.resolveAccounts(shipment, "RUH", BatchProcessingConstants.CHARGE_CODE_ECS))
                .thenReturn(List.of(account));
        when(chargeLineSupport.resolveAccounts(shipment, "RUH", BatchProcessingConstants.CHARGE_CODE_DTS))
                .thenReturn(List.of(account));
        when(chargeLineSupport.resolveServiceCharges("DXB", account, BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(List.of(serviceCharge("SC1", "Import Customs")));
        when(chargeLineSupport.resolveServiceCharges("RUH", account, BatchProcessingConstants.CHARGE_CODE_ECS))
                .thenReturn(List.of(serviceCharge("ECS1", "Freight Cost")));
        when(chargeLineSupport.resolveServiceCharges("RUH", account, BatchProcessingConstants.CHARGE_CODE_DTS))
                .thenReturn(List.of(serviceCharge("DTS1", "Freight Cost")));
        when(serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                eq("DXB"), eq("RUH"), eq(BatchProcessingConstants.CHARGE_CODE_ECS)))
                .thenReturn(List.of(new ServiceChargeDescriptionMappingDto(
                        "Import Customs", "SC1", "Freight Cost", "ECS1")));
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.propagateAfterEcdActualUpload(context);

        assertThat(dtsLine.getAmount()).isEqualByComparingTo("1");
    }

    private static ServiceChargeResponse serviceCharge(String serviceCode, String description) {
        return CalculatorTestDataFactory.serviceCharge(serviceCode, description, "TYPE", BigDecimal.ZERO, true);
    }

    private static ChargeLineItem lineWithPaymentRef(
            Charge charge, Account account, String serviceCode, String description, BigDecimal amount) {
        ChargeLineItem line = CalculatorTestDataFactory.chargeLineItem(charge, account, serviceCode, amount);
        line.setServiceCodeDescription(description);
        line.setPaymentReference("SAD-001");
        return line;
    }
}
