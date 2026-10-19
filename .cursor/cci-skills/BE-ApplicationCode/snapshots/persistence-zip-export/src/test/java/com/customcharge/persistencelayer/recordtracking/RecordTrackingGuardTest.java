package com.customcharge.persistencelayer.recordtracking;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class RecordTrackingGuardTest {

    @Test
    void runSafely_shouldExecuteAction() {
        AtomicBoolean executed = new AtomicBoolean(false);
        RecordTrackingGuard.runSafely(() -> executed.set(true));
        assertThat(executed).isTrue();
    }

    @Test
    void runSafely_shouldSwallowExceptions() {
        RecordTrackingGuard.runSafely(() -> {
            throw new RuntimeException("tracking failed");
        });
    }

    @Test
    void runSafely_shouldNoOpForNullAction() {
        RecordTrackingGuard.runSafely(null);
    }
}
