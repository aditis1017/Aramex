package com.customcharge.shipment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.core.env.Environment;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentQueueDepthResolverTest {

    @Mock
    private RabbitAdmin rabbitAdmin;
    @Mock
    private Environment environment;

    @InjectMocks
    private ShipmentQueueDepthResolver resolver;

    @Test
    void messageCount_shouldReturnQueueDepth() {
        Properties properties = new Properties();
        properties.put(RabbitAdmin.QUEUE_MESSAGE_COUNT, 12);
        when(environment.getProperty("queue.client-shipment")).thenReturn("cci.chargeCreation.prod.5");
        when(rabbitAdmin.getQueueProperties("cci.chargeCreation.prod.5")).thenReturn(properties);

        assertThat(resolver.messageCount()).isEqualTo(12L);
        assertThat(resolver.queueName()).isEqualTo("cci.chargeCreation.prod.5");
    }

    @Test
    void messageCount_shouldReturnMinusOneWhenQueueMissing() {
        when(environment.getProperty("queue.client-shipment")).thenReturn(null);

        assertThat(resolver.messageCount()).isEqualTo(-1L);
    }
}
