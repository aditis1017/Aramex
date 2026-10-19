package com.customcharge.persistencelayer.recordtracking;

import com.customcharge.persistencelayer.observability.CustomChargeMetricsProperties;
import com.customcharge.persistencelayer.observability.FileProcessingMetrics;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Optional phase timers for record processing. Emits Micrometer phase timers; all calls are safe for business flow.
 */
@Component
@ConditionalOnProperty(prefix = "customcharge.record-tracking", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RecordTrackingStepProbe {

    private final ObjectProvider<FileProcessingMetrics> fileProcessingMetrics;
    private final CustomChargeMetricsProperties metricsProperties;

    public RecordTrackingStepProbe(
            ObjectProvider<FileProcessingMetrics> fileProcessingMetrics,
            CustomChargeMetricsProperties metricsProperties) {
        this.fileProcessingMetrics = fileProcessingMetrics;
        this.metricsProperties = metricsProperties;
    }

    public PhaseTimer validationStarted() {
        return new PhaseTimer();
    }

    public void validationCompleted(PhaseTimer timer) {
        recordPhase("validation", timer);
    }

    public PhaseTimer calculationStarted() {
        return new PhaseTimer();
    }

    public void calculationCompleted(PhaseTimer timer) {
        recordPhase("calculation", timer);
    }

    public PhaseTimer dbSaveStarted() {
        return new PhaseTimer();
    }

    public void dbSaveCompleted(PhaseTimer timer) {
        recordPhase("db_save", timer);
    }

    private void recordPhase(String phase, PhaseTimer timer) {
        if (timer == null || !metricsProperties.getFileProcessing().isEnabled()) {
            return;
        }
        FileProcessingMetrics metrics = fileProcessingMetrics.getIfAvailable();
        if (metrics == null) {
            return;
        }
        String processFor = FileProcessingMetrics.normalize(MDC.get(RecordTrackingMdc.PROCESS_FOR));
        metrics.recordRecordPhaseDuration(timer.elapsedMs(), phase, processFor);
    }

    public static final class PhaseTimer {
        private final long startedAtMs = System.currentTimeMillis();

        public long elapsedMs() {
            return RecordTrackingTimeline.durationSince(startedAtMs);
        }
    }
}
