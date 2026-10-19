package com.customcharge.shipment.ingest.adapter.publish;

import com.customcharge.persistencelayer.entities.ShipmentXmlData;
import com.customcharge.persistencelayer.messaging.MessagePublisher;
import com.customcharge.persistencelayer.repository.ShipmentXmlDataRepository;
import com.customcharge.shipment.ingest.adapter.config.IngestAdapterProperties;
import com.customcharge.shipment.ingest.adapter.constants.IngestAdapterLogMessages;
import com.customcharge.shipment.ingest.adapter.model.ShipmentInboundEnvelope;
import com.customcharge.shipment.ingest.adapter.support.RabbitMessagePayloadReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.MessagingOperationFailedException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ConditionalOnProperty(prefix = "shipment.ingest.adapter", name = "enabled", havingValue = "true")
public class ShipmentInboundSqsPublisher {

    @Qualifier("sqsPublisher")
    MessagePublisher sqsPublisher;
    IngestAdapterProperties adapterProperties;
    ObjectMapper objectMapper;
    ShipmentXmlDataRepository shipmentXmlDataRepository;

    public void publishFromRabbitMessage(Message rabbitMessage) {
        String payloadXml = RabbitMessagePayloadReader.readUtf8Body(rabbitMessage);
        ShipmentInboundEnvelope envelope = new ShipmentInboundEnvelope(payloadXml);
        String queue = adapterProperties.getTargetSqsQueue();
        try {
            String json = objectMapper.writeValueAsString(envelope);
            sqsPublisher.send(queue, json);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize shipment ingest envelope for queue={}", queue, ex);
            throw new ImmediateRequeueAmqpException("Failed to serialize shipment ingest envelope", ex);
        } catch (SdkException | MessagingOperationFailedException ex) {
            log.error(IngestAdapterLogMessages.ADAPTER_SQS_FORWARD_FAILED, queue, ex);
            saveFailedMessage(payloadXml, ex);
        } catch (RuntimeException ex) {
            log.error(IngestAdapterLogMessages.ADAPTER_SQS_FORWARD_FAILED, queue, ex);
            throw new ImmediateRequeueAmqpException("Failed to publish shipment message to SQS", ex);
        }
    }

    private void saveFailedMessage(String payloadXml, Exception ex) {
        try {
            ShipmentXmlData data = new ShipmentXmlData();
            data.setXmlFile(payloadXml);
            data.setErrorMessage(ex.getMessage());
            shipmentXmlDataRepository.save(data);
        } catch (Exception dbEx) {
            log.error("Failed to save message to DB", dbEx);
        }
    }
}
