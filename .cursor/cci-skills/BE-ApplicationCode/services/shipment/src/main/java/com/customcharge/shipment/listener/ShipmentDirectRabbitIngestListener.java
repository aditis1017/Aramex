package com.customcharge.shipment.listener;

import com.customcharge.shipment.service.ShipmentMessageProcessor;
import com.customcharge.shipment.service.ShipmentQueueDepthResolver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import static com.customcharge.shipment.constants.LogMessages.SHIPMENT_LISTENER_QUEUE_DEPTH;

/**
 * Legacy path: process shipment messages directly from Rabbit (adapter disabled).
 */
@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ConditionalOnProperty(prefix = "shipment.ingest.adapter", name = "enabled", havingValue = "false", matchIfMissing = true)
public class ShipmentDirectRabbitIngestListener {

    ShipmentMessageProcessor shipmentMessageProcessor;
    ShipmentQueueDepthResolver queueDepthResolver;

//    @RabbitListener(queues = "${queue.client-shipment}")
//    public void onMessage(Message message) {
//        String queueName = queueDepthResolver.queueName();
//        log.info(SHIPMENT_LISTENER_QUEUE_DEPTH, queueName, queueDepthResolver.messageCount());
//        shipmentMessageProcessor.processFromRabbitMessage(message);
//    }
}
