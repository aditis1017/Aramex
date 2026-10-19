package com.customcharge.shipment.ingest.worker.sqs;

import com.customcharge.shipment.ingest.adapter.model.ShipmentInboundEnvelope;
import com.customcharge.shipment.service.ShipmentMessageProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.support.converter.MessageConversionException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ShipmentSqsIngestWorkerListenerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ShipmentMessageProcessor shipmentMessageProcessor;

    @InjectMocks
    private ShipmentSqsIngestWorkerListener listener;

    @Test
    void onMessage_shouldDelegateWhenEnvelopeValid() throws Exception {
        String payload = objectMapper.writeValueAsString(new ShipmentInboundEnvelope("<shipment/>"));

        listener.onMessage(payload);

        verify(shipmentMessageProcessor).processFromXmlPayload("<shipment/>");
    }

    @Test
    void onMessage_shouldAckPoisonWhenXmlConversionFails() {
        doThrow(new MessageConversionException("bad xml"))
                .when(shipmentMessageProcessor)
                .processFromXmlPayload(anyString());
        String payload = "{\"payloadXml\":\"<not-xml\"}";

        listener.onMessage(payload);

        verify(shipmentMessageProcessor).processFromXmlPayload("<not-xml");
    }

    @Test
    void onMessage_shouldAckPoisonWhenEnvelopeJsonInvalid() {
        listener.onMessage("{not-json");

        verifyNoInteractions(shipmentMessageProcessor);
    }

    @Test
    void onMessage_shouldAckPoisonWhenBodyEmpty() {
        listener.onMessage("   ");

        verifyNoInteractions(shipmentMessageProcessor);
    }

    @Test
    void onMessage_shouldRethrowProcessingFailuresForRetry() {
        doThrow(new IllegalStateException("db down"))
                .when(shipmentMessageProcessor)
                .processFromXmlPayload(anyString());
        String payload = "{\"payloadXml\":\"<shipment/>\"}";

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> listener.onMessage(payload))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db down");
        verify(shipmentMessageProcessor).processFromXmlPayload("<shipment/>");
        verifyNoMoreInteractions(shipmentMessageProcessor);
    }

}
