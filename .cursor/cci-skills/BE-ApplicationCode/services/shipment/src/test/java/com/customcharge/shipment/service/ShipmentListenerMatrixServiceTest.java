package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.config.datasource.DatasourceProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.core.env.Environment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentListenerMatrixServiceTest {

    @Mock
    private Environment environment;

    @Mock
    private RabbitProperties rabbitProperties;

    @Mock
    private DatasourceProperties datasourceProperties;

    @Mock
    private RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @Mock
    private ShipmentMessageConsumptionTracker consumptionTracker;

    @Mock
    private SimpleMessageListenerContainer simpleMessageListenerContainer;

    @InjectMocks
    private ShipmentListenerMatrixService service;

    @Test
    void captureMatrix_shouldIncludeListenerRow() {
        RabbitProperties.SimpleContainer simple = new RabbitProperties.SimpleContainer();
        simple.setConcurrency(20);
        simple.setMaxConcurrency(100);
        simple.setPrefetch(20);
        simple.setDefaultRequeueRejected(false);
        RabbitProperties.Listener listenerConfig = org.mockito.Mockito.mock(RabbitProperties.Listener.class);

        when(rabbitProperties.getListener()).thenReturn(listenerConfig);
        when(listenerConfig.getSimple()).thenReturn(simple);
        when(rabbitProperties.getHost()).thenReturn("rabbit.local");
        when(rabbitProperties.getVirtualHost()).thenReturn("/cci");
        when(environment.getProperty("queue.client-shipment")).thenReturn("HB_CCI_MSG5");
        when(environment.getProperty("HOSTNAME")).thenReturn("shipment-pod-1");
        when(datasourceProperties.getMaximumPoolSize()).thenReturn(50);
        when(datasourceProperties.getMinimumIdle()).thenReturn(10);
        when(simpleMessageListenerContainer.getQueueNames()).thenReturn(new String[] {"HB_CCI_MSG5"});
        when(simpleMessageListenerContainer.getActiveConsumerCount()).thenReturn(25);
        when(simpleMessageListenerContainer.isRunning()).thenReturn(true);
        when(rabbitListenerEndpointRegistry.getListenerContainers())
                .thenReturn(List.of((MessageListenerContainer) simpleMessageListenerContainer));
        when(consumptionTracker.currentMinutePodCount()).thenReturn(5L);
        when(consumptionTracker.getLastCompletedMinutePodCount()).thenReturn(42L);
        when(consumptionTracker.lifetimePodCount()).thenReturn(1000L);
        when(consumptionTracker.lastCompletedMinuteClusterCount()).thenReturn(120L);

        var matrix = service.captureMatrix();

        assertThat(matrix.getInstanceId()).isEqualTo("shipment-pod-1");
        assertThat(matrix.getListeners()).hasSize(1);
        assertThat(matrix.getListeners().get(0).getActiveConsumerCount()).isEqualTo(25);
        assertThat(matrix.getListeners().get(0).getAnnotationConcurrency()).isEqualTo("20-100");
    }
}
