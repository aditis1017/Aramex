package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dao.KeywordRuleDao;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.shipment.observability.ShipmentProcessingPhaseTimer;
import com.customcharge.shipment.service.mapper.ShipmentEntityMapper;
import com.customcharge.shipment.service.rules.CategoryTypeRuleResolver;
import com.customcharge.shipment.service.rules.RuleResolverSupport;
import com.customcharge.shipment.service.rules.ShipmentTypeRuleResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentPersistenceServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private ShipmentEntityMapper shipmentEntityMapper;
    @Mock
    private ShipmentTypeRuleResolver shipmentTypeRuleResolver;
    @Mock
    private CategoryTypeRuleResolver categoryTypeRuleResolver;
    @Mock
    private KeywordRuleDao keywordRuleDao;
    @Mock
    private ShipmentPersistenceWriter shipmentPersistenceWriter;
    @Mock
    private ObjectProvider<ExchangeRateService> exchangeRateService;
    @Mock
    private ShipmentProcessingPhaseTimer phaseTimer;
    @Mock
    private ShipmentArAccountService shipmentArAccountService;
    @Mock
    private ShipmentBusinessTypeResolver shipmentBusinessTypeResolver;
    @Mock
    private ShipmentMawbLinkageService shipmentMawbLinkageService;

    @InjectMocks
    private ShipmentPersistenceService shipmentPersistenceService;

    @BeforeEach
    void setUpPhaseTimerPassthrough() {
        lenient().when(shipmentArAccountService.findArAccount(any())).thenReturn(Optional.empty());
        lenient().when(shipmentBusinessTypeResolver.resolve(any(Shipment.class), any(), any()))
                .thenReturn("B2C");
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return null;
        }).when(phaseTimer).run(anyString(), anyString(), any(Runnable.class));
        lenient().doAnswer(invocation -> invocation.getArgument(2, java.util.function.Supplier.class).get())
                .when(phaseTimer).supply(anyString(), anyString(), any(java.util.function.Supplier.class));
    }

    @Test
    void prepareNewShipment_shouldReturnEmpty_whenHawbMissing() {
        ShipmentDto message = new ShipmentDto();

        Optional<PreparedNewShipment> result = shipmentPersistenceService.prepareNewShipment(message);

        assertThat(result).isEmpty();
        verify(shipmentPersistenceWriter, never()).saveNewShipment(any(), anyString(), anyList());
    }

    @Test
    void prepareNewShipment_shouldReturnEmpty_whenDuplicateHawb() {
        ShipmentDto message = message("HAWB-1");
        when(shipmentRepository.existsByHawbNumber("HAWB-1")).thenReturn(true);

        Optional<PreparedNewShipment> result = shipmentPersistenceService.prepareNewShipment(message);

        assertThat(result).isEmpty();
        verify(shipmentPersistenceWriter, never()).saveNewShipment(any(), anyString(), anyList());
    }

    @Test
    void prepareNewShipment_shouldThrow_whenIncompleteAndNoExistingRecord() {
        ShipmentDto message = message("HAWB-2");
        message.setIsShipmentFull(false);
        when(shipmentRepository.existsByHawbNumber("HAWB-2")).thenReturn(false);

        assertThatThrownBy(() -> shipmentPersistenceService.prepareNewShipment(message))
                .isInstanceOf(IncompleteShipmentException.class);
    }

    @Test
    void prepareNewShipment_shouldReturnPrepared_whenNewHawb() {
        ShipmentDto message = message("HAWB-3");
        UUID ruleId = UUID.randomUUID();
        RuleResolverSupport.RuleAssignment assignment = new RuleResolverSupport.RuleAssignment(
                "DDU", ruleId, List.of(), null);

        when(shipmentRepository.existsByHawbNumber("HAWB-3")).thenReturn(false);
        when(shipmentTypeRuleResolver.resolveShipmentType(message, false))
                .thenReturn(Optional.of(new RuleResolverSupport.ResolvedRule("DDU", assignment)));
        when(categoryTypeRuleResolver.resolveCategoryType(message, false))
                .thenReturn(Optional.of(new RuleResolverSupport.ResolvedRule("LV", null)));
        doAnswer(invocation -> {
            ShipmentDto dto = invocation.getArgument(0);
            Shipment shipment = invocation.getArgument(1);
            shipment.setBusinessType("Retail");
            shipment.setConsigneeName(dto.getConsigneeName());
            return null;
        }).when(shipmentEntityMapper).map(any(ShipmentDto.class), any(Shipment.class));
        when(keywordRuleDao.evaluateB2BUsingFunction(any())).thenReturn(true);

        Optional<PreparedNewShipment> result = shipmentPersistenceService.prepareNewShipment(message);

        assertThat(result).isPresent();
        assertThat(result.get().hawb()).isEqualTo("HAWB-3");
        verify(shipmentPersistenceWriter, never()).saveNewShipment(any(), anyString(), anyList());
        verify(keywordRuleDao).evaluateB2BUsingFunction(any());
    }

    @Test
    void prepareNewShipment_shouldEvaluateB2bKeyword_whenConsigneeNamePresent() {
        ShipmentDto message = message("HAWB-4");
        when(shipmentRepository.existsByHawbNumber("HAWB-4")).thenReturn(false);
        when(shipmentTypeRuleResolver.resolveShipmentType(message, false)).thenReturn(Optional.empty());
        when(categoryTypeRuleResolver.resolveCategoryType(message, false)).thenReturn(Optional.empty());
        when(keywordRuleDao.evaluateB2BUsingFunction(any())).thenReturn(false);
        when(shipmentBusinessTypeResolver.resolve(any(Shipment.class), any(), any())).thenReturn("E-Commerce");
        doAnswer(invocation -> null).when(shipmentEntityMapper).map(any(ShipmentDto.class), any(Shipment.class));

        shipmentPersistenceService.prepareNewShipment(message);

        verify(keywordRuleDao).evaluateB2BUsingFunction(any());
        verify(shipmentPersistenceWriter, never()).saveNewShipment(any(), anyString(), anyList());
    }

    @Test
    void findShipmentNeedingCharges_shouldReturnShipment_whenNoCharges() {
        Shipment shipment = new Shipment();
        UUID shipmentId = UUID.randomUUID();
        ReflectionTestUtils.setField(shipment, "id", shipmentId);
        when(shipmentRepository.findByHawbNumber("HAWB-6")).thenReturn(Optional.of(shipment));
        when(chargeRepository.existsByShipment_Id(shipmentId)).thenReturn(false);

        Optional<Shipment> result = shipmentPersistenceService.findShipmentNeedingCharges("HAWB-6");

        assertThat(result).contains(shipment);
    }

    @Test
    void findShipmentNeedingCharges_shouldReturnEmpty_whenChargesExist() {
        Shipment shipment = new Shipment();
        UUID shipmentId = UUID.randomUUID();
        ReflectionTestUtils.setField(shipment, "id", shipmentId);
        when(shipmentRepository.findByHawbNumber("HAWB-7")).thenReturn(Optional.of(shipment));
        when(chargeRepository.existsByShipment_Id(shipmentId)).thenReturn(true);

        Optional<Shipment> result = shipmentPersistenceService.findShipmentNeedingCharges("HAWB-7");

        assertThat(result).isEmpty();
    }

    private static ShipmentDto message(String hawb) {
        ShipmentDto message = new ShipmentDto();
        message.setHawb(hawb);
        message.setConsigneeName("ACME");
        message.setIsShipmentFull(true);
        return message;
    }
}
