package com.customcharge.persistencelayer.messaging;

import com.customcharge.persistencelayer.config.rabbitmq.RabbitMQProperties;
import org.slf4j.MDC;
import org.springframework.amqp.core.AmqpTemplate;

public class RabbitMessagePublisher implements MessagePublisher {

    private final AmqpTemplate amqpTemplate;
    private final RabbitMQProperties properties;

    public RabbitMessagePublisher(AmqpTemplate amqpTemplate,
                                  RabbitMQProperties properties) {
        this.amqpTemplate = amqpTemplate;
        this.properties = properties;
    }

    @Override
    public void send(String destination, Object payload) {
        String traceId = MDC.get("traceId");

        amqpTemplate.convertAndSend(
                properties.getExchangeName(),
                destination,
                payload,
                message -> {
                    message.getMessageProperties()
                            .setHeader("traceId", traceId);
                    return message;
                }
        );
    }
}