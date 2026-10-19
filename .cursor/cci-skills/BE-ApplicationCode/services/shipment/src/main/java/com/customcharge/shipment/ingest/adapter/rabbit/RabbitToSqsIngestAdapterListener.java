package com.customcharge.shipment.ingest.adapter.rabbit;

import com.customcharge.shipment.ingest.adapter.publish.ShipmentInboundSqsPublisher;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Standalone Rabbit ingest adapter: receive XML, publish to SQS, ack Rabbit after successful send.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ConditionalOnProperty(prefix = "shipment.ingest.adapter", name = "enabled", havingValue = "true")
public class RabbitToSqsIngestAdapterListener {

    ShipmentInboundSqsPublisher inboundSqsPublisher;

    @RabbitListener(queues = "${queue.client-shipment}")
    public void onMessage(Message message) {

        inboundSqsPublisher.publishFromRabbitMessage(message);
    }
}
