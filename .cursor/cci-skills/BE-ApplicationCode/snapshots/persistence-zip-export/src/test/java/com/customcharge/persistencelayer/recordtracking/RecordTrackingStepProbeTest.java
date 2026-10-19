package com.customcharge.persistencelayer.recordtracking;

import com.customcharge.persistencelayer.observability.CustomChargeMetricsProperties;
import com.customcharge.persistencelayer.observability.FileProcessingMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecordTrackingStepProbeTest {

    private SimpleMeterRegistry registry;
    private FileProcessingMetrics metrics;
    private RecordTrackingStepProbe probe;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new FileProcessingMetrics(registry);
        CustomChargeMetricsProperties properties = new CustomChargeMetricsProperties();
        properties.getFileProcessing().setEnabled(true);
        ObjectProvider<FileProcessingMetrics> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(metrics);
        probe = new RecordTrackingStepProbe(provider, properties);
        MDC.put(RecordTrackingMdc.PROCESS_FOR, "Actual_Template");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void calculationCompleted_shouldRecordPhaseTimer() {
        RecordTrackingStepProbe.PhaseTimer timer = probe.calculationStarted();
        probe.calculationCompleted(timer);

        assertThat(registry.find("customcharge.file.processing.records.processing.phase.duration")
                .tag("phase", "calculation")
                .tag("process_for", "Actual_Template")
                .timer()
                .count()).isEqualTo(1L);
    }

    @Test
    void calculationCompleted_shouldNoOpWhenTimerNull() {
        probe.calculationCompleted(null);
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    void disabledPhases_shouldReturnTimersWithoutRecordingWhenMetricsDisabled() {
        CustomChargeMetricsProperties properties = new CustomChargeMetricsProperties();
        properties.getFileProcessing().setEnabled(false);
        ObjectProvider<FileProcessingMetrics> provider = mock(ObjectProvider.class);
        RecordTrackingStepProbe disabledProbe = new RecordTrackingStepProbe(provider, properties);

        RecordTrackingStepProbe.PhaseTimer validationTimer = disabledProbe.validationStarted();
        disabledProbe.validationCompleted(validationTimer);

        RecordTrackingStepProbe.PhaseTimer dbTimer = disabledProbe.dbSaveStarted();
        disabledProbe.dbSaveCompleted(dbTimer);

        assertThat(validationTimer.elapsedMs()).isGreaterThanOrEqualTo(0L);
        assertThat(dbTimer.elapsedMs()).isGreaterThanOrEqualTo(0L);
        assertThat(registry.getMeters()).isEmpty();
    }
}
