package com.customcharge.persistencelayer.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileProcessingMetricsAutoConfigurationTest {

    private final FileProcessingMetricsAutoConfiguration configuration = new FileProcessingMetricsAutoConfiguration();

    @Test
    void meterRegistry_shouldCreateSimpleRegistryWhenMissing() {
        MeterRegistry registry = configuration.meterRegistry();

        assertThat(registry).isInstanceOf(SimpleMeterRegistry.class);
    }

    @Test
    void fileProcessingMetrics_shouldUseProvidedRegistry() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        FileProcessingMetrics metrics = configuration.fileProcessingMetrics(registry);

        assertThat(metrics).isNotNull();
        metrics.recordFileReceived("csv", "template");
        assertThat(registry.find("customcharge.file.processing.files.received").counter().count()).isEqualTo(1.0);
    }
}
