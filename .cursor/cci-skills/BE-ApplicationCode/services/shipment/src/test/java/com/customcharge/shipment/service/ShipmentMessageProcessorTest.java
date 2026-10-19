package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.shipment.converter.ShipmentXmlConverter;
import com.customcharge.shipment.mapper.XmlShipmentDtoMapper;
import com.customcharge.shipment.observability.ShipmentProcessingPhaseTimer;
import com.customcharge.shipment.service.cif.ShipmentCifUpdateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static com.customcharge.shipment.constants.QueueMessages.INCOMPLETE_SHIPMENT_IGNORED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentMessageProcessorTest {

    @Mock
    private ShipmentService shipmentService;
    @Mock
    private XmlShipmentDtoMapper xmlShipmentDtoMapper;
    @Mock
    private ShipmentCompletenessChecker shipmentCompletenessChecker;
    @Mock
    private ShipmentCifUpdateService shipmentCifUpdateService;
    @Mock
    private ShipmentMessageConsumptionTracker consumptionTracker;
    @Mock
    private ShipmentProcessingPhaseTimer phaseTimer;
    @Mock
    private ShipmentXmlConverter shipmentXmlConverter;

    @InjectMocks
    private ShipmentMessageProcessor processor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUpPhaseTimerPassthrough() {
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return null;
        }).when(phaseTimer).run(anyString(), anyString(), any(Runnable.class));
        lenient().doNothing().when(phaseTimer).logSummary(anyString());
        lenient().doNothing().when(phaseTimer).clear();
    }

    @Test
    void processFromRabbitMessage_shouldDelegateSh267ToCifUpdateService() throws Exception {
        JsonNode node = objectMapper.readTree("{\"hawb\":\"HAWB-SH267\"}");
        Message message = new Message("<xml/>".getBytes(), new MessageProperties());
        when(shipmentXmlConverter.fromMessage(message)).thenReturn(node);
        ShipmentDto dto = new ShipmentDto();
        dto.setHawb("HAWB-SH267");
        dto.setPiNumber("SH267");
        when(xmlShipmentDtoMapper.map(node)).thenReturn(dto);

        processor.processFromRabbitMessage(message);

        verify(shipmentCifUpdateService).handle(dto);
        verify(consumptionTracker).recordConsumed();
        verify(shipmentCompletenessChecker, org.mockito.Mockito.never()).applyShipmentFilterRules(dto);
        verify(shipmentService, org.mockito.Mockito.never()).processShipment(dto);
    }

    @Test
    void processFromRabbitMessage_shouldDelegateSh022ToShipmentService() throws Exception {
        JsonNode node = objectMapper.readTree("{\"hawb\":\"HAWB-SH022\"}");
        Message message = new Message("<xml/>".getBytes(), new MessageProperties());
        when(shipmentXmlConverter.fromMessage(message)).thenReturn(node);
        ShipmentDto dto = new ShipmentDto();
        dto.setHawb("HAWB-SH022");
        dto.setPiNumber("SH022");
        when(xmlShipmentDtoMapper.map(node)).thenReturn(dto);

        processor.processFromRabbitMessage(message);

        verify(shipmentService).handleSh022(dto);
        verify(consumptionTracker).recordConsumed();
        verify(shipmentCompletenessChecker, org.mockito.Mockito.never()).applyShipmentFilterRules(dto);
        verify(shipmentService, org.mockito.Mockito.never()).processShipment(dto);
    }

    @Test
    void processFromRabbitMessage_shouldProcessFullShipment() throws Exception {
        JsonNode node = objectMapper.readTree("{\"hawb\":\"HAWB-1\"}");
        Message message = new Message("<xml/>".getBytes(), new MessageProperties());
        when(shipmentXmlConverter.fromMessage(message)).thenReturn(node);
        ShipmentDto dto = new ShipmentDto();
        dto.setHawb("HAWB-1");
        dto.setIsShipmentFull(true);
        when(xmlShipmentDtoMapper.map(node)).thenReturn(dto);

        processor.processFromRabbitMessage(message);

        verify(shipmentCompletenessChecker).applyShipmentFilterRules(dto);
        verify(shipmentService).processShipment(dto);
        verify(consumptionTracker).recordConsumed();
    }

    @Test
    void processFromRabbitMessage_shouldRejectIncompleteShipment() throws Exception {
        JsonNode node = objectMapper.readTree("{\"hawb\":\"HAWB-2\"}");
        Message message = new Message("<xml/>".getBytes(), new MessageProperties());
        when(shipmentXmlConverter.fromMessage(message)).thenReturn(node);
        ShipmentDto dto = new ShipmentDto();
        dto.setHawb("HAWB-2");
        dto.setIsShipmentFull(false);
        when(xmlShipmentDtoMapper.map(node)).thenReturn(dto);
        doThrow(new IncompleteShipmentException(INCOMPLETE_SHIPMENT_IGNORED))
                .when(shipmentService).processShipment(dto);

        assertThatThrownBy(() -> processor.processFromRabbitMessage(message))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(consumptionTracker, org.mockito.Mockito.never()).recordConsumed();
    }

    @Test
    void processFromXmlPayload_shouldPropagateIncompleteShipmentForSqsPoisonHandling() throws Exception {
        JsonNode node = objectMapper.readTree("{\"hawb\":\"HAWB-2\"}");
        when(shipmentXmlConverter.fromXmlString("<xml/>")).thenReturn(node);
        ShipmentDto dto = new ShipmentDto();
        dto.setHawb("HAWB-2");
        when(xmlShipmentDtoMapper.map(node)).thenReturn(dto);
        doThrow(new IncompleteShipmentException(INCOMPLETE_SHIPMENT_IGNORED))
                .when(shipmentService).processShipment(dto);

        assertThatThrownBy(() -> processor.processFromXmlPayload("<xml/>"))
                .isInstanceOf(IncompleteShipmentException.class);
    }
}
