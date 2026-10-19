package com.customcharge.persistencelayer.observability;

import com.customcharge.persistencelayer.messaging.MessagePublisher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsMessagePublisherDecoratorTest {

    @Mock
    private MessagePublisher delegate;
    @Mock
    private ObjectProvider<FileProcessingMetrics> metricsProvider;

    @Test
    void send_shouldDelegateWhenMetricsUnavailable() {
        when(metricsProvider.getIfAvailable()).thenReturn(null);
        MetricsMessagePublisherDecorator decorator =
                new MetricsMessagePublisherDecorator(delegate, metricsProvider, "queue");

        decorator.send("queue", "payload");

        verify(delegate).send("queue", "payload");
    }

    @Test
    void send_shouldRecordSuccessMetrics() {
        FileProcessingMetrics metrics = new FileProcessingMetrics(new SimpleMeterRegistry());
        when(metricsProvider.getIfAvailable()).thenReturn(metrics);
        MetricsMessagePublisherDecorator decorator =
                new MetricsMessagePublisherDecorator(delegate, metricsProvider, "queue");

        decorator.send("queue", "payload");

        verify(delegate).send("queue", "payload");
    }

    @Test
    void send_shouldRecordFailureMetricsAndRethrow() {
        FileProcessingMetrics metrics = new FileProcessingMetrics(new SimpleMeterRegistry());
        when(metricsProvider.getIfAvailable()).thenReturn(metrics);
        MetricsMessagePublisherDecorator decorator =
                new MetricsMessagePublisherDecorator(delegate, metricsProvider, "queue");
        doThrow(new RuntimeException("publish failed")).when(delegate).send("queue", "payload");

        assertThatThrownBy(() -> decorator.send("queue", "payload"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("publish failed");
    }
}
