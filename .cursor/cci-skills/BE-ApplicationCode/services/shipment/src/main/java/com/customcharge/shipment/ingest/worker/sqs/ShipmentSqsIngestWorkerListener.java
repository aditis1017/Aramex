package com.customcharge.shipment.ingest.worker.sqs;

import com.customcharge.shipment.ingest.adapter.model.ShipmentInboundEnvelope;
import com.customcharge.shipment.ingest.worker.constants.IngestWorkerLogMessages;
import com.customcharge.shipment.service.IncompleteShipmentException;
import com.customcharge.shipment.service.ShipmentMessageProcessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.amqp.support.converter.MessageConversionException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ConditionalOnProperty(prefix = "shipment.ingest.adapter", name = "enabledsqs", havingValue = "true")
public class ShipmentSqsIngestWorkerListener {

    ObjectMapper objectMapper;
    ShipmentMessageProcessor shipmentMessageProcessor;

    @SqsListener(
            value = "${shipment.ingest.worker.sqs-queue}",
            maxConcurrentMessages = "${shipment.ingest.worker.listener-concurrency}",
            maxMessagesPerPoll = "${shipment.ingest.worker.listener-concurrency}")
    public void onMessage(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn(IngestWorkerLogMessages.WORKER_SQS_POISON, "empty body");
            return;
        }
        try {
            ShipmentInboundEnvelope envelope = objectMapper.readValue(payload, ShipmentInboundEnvelope.class);
            if (envelope.payloadXml() == null || envelope.payloadXml().isBlank()) {
                log.warn(IngestWorkerLogMessages.WORKER_SQS_POISON, "missing payloadXml");
                return;
            }
            shipmentMessageProcessor.processFromXmlPayload(envelope.payloadXml());
        } catch (IncompleteShipmentException | IllegalArgumentException | MessageConversionException ex) {
            log.warn(IngestWorkerLogMessages.WORKER_SQS_POISON, ex.getMessage());
        } catch (JsonProcessingException ex) {
            log.warn(IngestWorkerLogMessages.WORKER_SQS_POISON, ex.getMessage());
        }
    }
}
