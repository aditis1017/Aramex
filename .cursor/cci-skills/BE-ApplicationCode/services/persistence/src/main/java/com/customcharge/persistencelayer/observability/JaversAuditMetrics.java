package com.customcharge.persistencelayer.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class JaversAuditMetrics {

    static final String FAILED_COUNTER = "customcharge.javers.async.commit.failed";

    private final Counter failedCommits;

    public JaversAuditMetrics(MeterRegistry registry) {
        this.failedCommits = Counter.builder(FAILED_COUNTER)
                .description("Failed asynchronous Javers audit commits")
                .register(registry);
    }

    public void recordFailedCommit() {
        failedCommits.increment();
    }
}
