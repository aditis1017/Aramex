package com.customcharge.persistencelayer.messaging;

public interface MessagePublisher {
    void send(String destination, Object payload);
}