package com.customcharge.persistencelayer.messaging;

import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ChannelInterceptor;

import java.util.UUID;

public class TraceSqsInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, org.springframework.messaging.MessageChannel channel) {

        String traceId = (String) message.getHeaders().get("traceId");

        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put("traceId", traceId);

        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message,
                                    org.springframework.messaging.MessageChannel channel,
                                    boolean sent,
                                    Exception ex) {
        MDC.clear();
    }
}
