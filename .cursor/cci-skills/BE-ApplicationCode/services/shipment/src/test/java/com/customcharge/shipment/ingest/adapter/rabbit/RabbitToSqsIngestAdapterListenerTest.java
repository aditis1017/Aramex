package com.customcharge.shipment.ingest.adapter.rabbit;

import com.customcharge.shipment.ingest.adapter.publish.ShipmentInboundSqsPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitToSqsIngestAdapterListenerTest {

    @Mock
    private ShipmentInboundSqsPublisher inboundSqsPublisher;
    @InjectMocks
    private RabbitToSqsIngestAdapterListener listener;

    @Test
    void onMessage_shouldPublishToSqs() {
        Message message = new Message("<xml/>".getBytes(), new MessageProperties());

        listener.onMessage(message);

        verify(inboundSqsPublisher).publishFromRabbitMessage(message);
    }
}
