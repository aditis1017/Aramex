package com.customcharge.persistencelayer.observability;

import com.customcharge.persistencelayer.messaging.MessagePublisher;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.ObjectProvider;


public class MetricsMessagePublisherDecorator implements MessagePublisher {

    private final MessagePublisher delegate;
    private final ObjectProvider<FileProcessingMetrics> metrics;
    private final String queueName;

    public MetricsMessagePublisherDecorator(
            MessagePublisher delegate,
            ObjectProvider<FileProcessingMetrics> metrics,
            String queueName) {
        this.delegate = delegate;
        this.metrics = metrics;
        this.queueName = queueName;
    }

    @Override
    public void send(String destination, Object payload) {
        FileProcessingMetrics fileProcessingMetrics = metrics.getIfAvailable();
        if (fileProcessingMetrics == null) {
            delegate.send(destination, payload);
            return;
        }
        Timer.Sample sample = fileProcessingMetrics.startTimer();
        try {
            delegate.send(destination, payload);
            fileProcessingMetrics.recordChunkPublishDuration(sample, FileProcessingMetrics.outcome(true));
        } catch (RuntimeException ex) {
            fileProcessingMetrics.recordSqsPublishFailed(queueName);
            fileProcessingMetrics.recordChunkPublishDuration(sample, FileProcessingMetrics.outcome(false));
            throw ex;
        }
    }
}
