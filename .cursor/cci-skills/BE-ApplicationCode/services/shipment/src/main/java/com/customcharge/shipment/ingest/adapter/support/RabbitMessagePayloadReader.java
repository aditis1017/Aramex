package com.customcharge.shipment.ingest.adapter.support;

import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;

public final class RabbitMessagePayloadReader {

    private RabbitMessagePayloadReader() {
    }

    public static String readUtf8Body(Message message) {
        if (message == null || message.getBody() == null) {
            return "";
        }
        return new String(message.getBody(), StandardCharsets.UTF_8);
    }
}
