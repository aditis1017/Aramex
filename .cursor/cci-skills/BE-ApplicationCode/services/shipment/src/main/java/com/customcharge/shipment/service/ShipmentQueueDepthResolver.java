package com.customcharge.shipment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
public class ShipmentQueueDepthResolver {

    private final RabbitAdmin rabbitAdmin;
    private final Environment environment;

    public String queueName() {
        return environment.getProperty("queue.client-shipment");
    }

    public long messageCount() {
        String queueName = queueName();
        if (queueName == null || queueName.isBlank()) {
            return -1L;
        }
        Properties properties = rabbitAdmin.getQueueProperties(queueName);
        if (properties == null) {
            return -1L;
        }
        Object count = properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        if (count instanceof Integer integer) {
            return integer.longValue();
        }
        return -1L;
    }
}
