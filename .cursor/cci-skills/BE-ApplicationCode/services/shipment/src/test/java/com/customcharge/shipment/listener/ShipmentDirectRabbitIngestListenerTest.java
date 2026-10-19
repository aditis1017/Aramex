package com.customcharge.shipment.listener;

import com.customcharge.shipment.service.ShipmentMessageProcessor;
import com.customcharge.shipment.service.ShipmentQueueDepthResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentDirectRabbitIngestListenerTest {

    @Mock
    private ShipmentMessageProcessor shipmentMessageProcessor;
    @Mock
    private ShipmentQueueDepthResolver queueDepthResolver;

    @InjectMocks
    private ShipmentDirectRabbitIngestListener listener;

    @BeforeEach
    void setUp() {
        when(queueDepthResolver.queueName()).thenReturn("HB_CCI_MSG5");
        when(queueDepthResolver.messageCount()).thenReturn(0L);
    }

    @Test
    void onMessage_shouldDelegateToProcessor() {
        Message message = new Message("<xml/>".getBytes(), new MessageProperties());

        listener.onMessage(message);

        verify(shipmentMessageProcessor).processFromRabbitMessage(message);
    }
}
