package com.customcharge.persistencelayer.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JaversAuditMetricsTest {

    @Test
    void recordFailedCommit_shouldIncrementCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JaversAuditMetrics metrics = new JaversAuditMetrics(registry);

        metrics.recordFailedCommit();
        metrics.recordFailedCommit();

        assertThat(registry.find(JaversAuditMetrics.FAILED_COUNTER).counter().count()).isEqualTo(2.0d);
    }
}
