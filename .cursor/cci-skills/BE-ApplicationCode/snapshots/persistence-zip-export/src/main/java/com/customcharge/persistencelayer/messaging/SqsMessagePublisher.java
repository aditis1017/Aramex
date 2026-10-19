package com.customcharge.persistencelayer.messaging;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class SqsMessagePublisher implements MessagePublisher {

    private final SqsTemplate sqsTemplate;

    public SqsMessagePublisher(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    @Override
    public void send(String destination, Object payload) {
        String traceId = MDC.get("traceId");

        Message<Object> message = MessageBuilder
                .withPayload(payload)
                .setHeader("traceId", traceId)
                .build();

        sqsTemplate.send(destination, message);
    }
}