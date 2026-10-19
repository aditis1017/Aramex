package com.customcharge.persistencelayer.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration(after = FileProcessingMetricsAutoConfiguration.class)
@EnableScheduling
@ConditionalOnClass(MeterRegistry.class)
public class MetricSnapshotAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "customcharge.metrics.snapshot", name = "enabled", havingValue = "true")
    MetricSnapshotPublisher metricSnapshotPublisher(
            MeterRegistry registry,
            CustomChargeMetricsProperties properties,
            @Value("${spring.application.name:unknown}") String serviceName) {
        return new MetricSnapshotPublisher(registry, properties, serviceName);
    }
}
