package com.customcharge.persistencelayer.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class MetricSnapshotPublisherTest {

    private SimpleMeterRegistry registry;
    private CustomChargeMetricsProperties properties;
    private MetricSnapshotPublisher publisher;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        properties = new CustomChargeMetricsProperties();
        properties.getSnapshot().setEnabled(true);
        publisher = new MetricSnapshotPublisher(registry, properties, "chargesCalculator");

        Logger logger = (Logger) LoggerFactory.getLogger(MetricSnapshotPublisher.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @Test
    void publishSnapshots_shouldEmitCustomchargeMetricForCounterDelta() {
        FileProcessingMetrics metrics = new FileProcessingMetrics(registry);
        metrics.recordRecordProcessed("dtc", FileProcessingMetrics.outcome(true), "AE");

        publisher.publishSnapshots();

        assertThat(appender.list)
                .extracting(ILoggingEvent::getMessage)
                .contains("customcharge_metric");
        assertThat(appender.list).isNotEmpty();
    }

    @Test
    void publishSnapshots_shouldEmitSeparateDeltasPerTagCombination() {
        FileProcessingMetrics metrics = new FileProcessingMetrics(registry);
        metrics.recordRecordProcessed("dtc", FileProcessingMetrics.outcome(true), "AE");
        metrics.recordRecordProcessed("dtc", FileProcessingMetrics.outcome(true), "RUH");

        publisher.publishSnapshots();

        assertThat(appender.list)
                .extracting(ILoggingEvent::getMessage)
                .filteredOn(message -> "customcharge_metric".equals(message))
                .hasSize(2);
    }

    @Test
    void publishSnapshots_shouldSkipWhenDisabled() {
        properties.getSnapshot().setEnabled(false);
        FileProcessingMetrics metrics = new FileProcessingMetrics(registry);
        metrics.recordRecordProcessed("dtc", FileProcessingMetrics.outcome(true), "AE");

        publisher.publishSnapshots();

        assertThat(appender.list).isEmpty();
    }
}
