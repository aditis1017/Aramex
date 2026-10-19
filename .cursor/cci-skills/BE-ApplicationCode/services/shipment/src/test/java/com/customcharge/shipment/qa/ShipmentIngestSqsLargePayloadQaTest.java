package com.customcharge.shipment.qa;

import com.customcharge.persistencelayer.messaging.MessagePublisher;
import com.customcharge.shipment.ingest.adapter.config.IngestAdapterProperties;
import com.customcharge.shipment.ingest.adapter.model.ShipmentInboundEnvelope;
import com.customcharge.shipment.ingest.adapter.publish.ShipmentInboundSqsPublisher;
import com.customcharge.shipment.ingest.worker.sqs.ShipmentSqsIngestWorkerListener;
import com.customcharge.shipment.service.ShipmentMessageProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * QA: TC-SQS large payload (simulated ~2MB XML through envelope JSON).
 */
@ExtendWith(MockitoExtension.class)
class ShipmentIngestSqsLargePayloadQaTest {

    private static final int TWO_MB = 2 * 1024 * 1024;

    @Mock
    private MessagePublisher sqsPublisher;
    @Mock
    private IngestAdapterProperties adapterProperties;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private ShipmentInboundSqsPublisher publisher;

    @Test
    @DisplayName("TC-SQS-001: ~2MB XML envelope serializes and exceeds SQS 256KiB raw limit")
    void tcSqs001_envelopeExceedsSqsDirectLimit() throws Exception {
        String largeXml = "<shipment>" + "x".repeat(TWO_MB) + "</shipment>";
        ShipmentInboundEnvelope envelope = new ShipmentInboundEnvelope(largeXml);
        String json = objectMapper.writeValueAsString(envelope);

        assertThat(largeXml.length()).isGreaterThan(262_144);
        assertThat(json.length()).isGreaterThan(262_144);
    }

    @Test
    @DisplayName("TC-SQS-002: Publisher forwards full JSON to SQS")
    void tcSqs002_publisherSendsLargeJson() {
        String largeXml = "<root>" + "y".repeat(TWO_MB) + "</root>";
        whenQueueConfigured();
        Message rabbit = new Message(largeXml.getBytes(), new MessageProperties());

        publisher.publishFromRabbitMessage(rabbit);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(sqsPublisher).send(eq("custom-charges-shipment-ingest"), body.capture());
        assertThat(body.getValue().length()).isGreaterThan(TWO_MB);
        assertThat(body.getValue()).contains("\"payloadXml\"");
    }

    @Test
    @DisplayName("TC-SQS-003: Worker consumes large JSON and delegates XML to processor")
    void tcSqs003_workerProcessesLargePayload() throws Exception {
        String largeXml = "<ship>" + "z".repeat(TWO_MB) + "</ship>";
        String json = objectMapper.writeValueAsString(new ShipmentInboundEnvelope(largeXml));
        ShipmentMessageProcessor processor = org.mockito.Mockito.mock(ShipmentMessageProcessor.class);
        ShipmentSqsIngestWorkerListener worker =
                new ShipmentSqsIngestWorkerListener(objectMapper, processor);

        worker.onMessage(json);

        verify(processor).processFromXmlPayload(largeXml);
    }

    private void whenQueueConfigured() {
        org.mockito.Mockito.when(adapterProperties.getTargetSqsQueue()).thenReturn("custom-charges-shipment-ingest");
    }
}
