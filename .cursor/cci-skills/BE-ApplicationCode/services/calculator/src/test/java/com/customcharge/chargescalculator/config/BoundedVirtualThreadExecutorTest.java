package com.customcharge.chargescalculator.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BoundedVirtualThreadExecutorTest {

    private BoundedVirtualThreadExecutor executor;

    @AfterEach
    void tearDown() throws Exception {
        if (executor != null) {
            executor.destroy();
        }
    }

    @Test
    void shouldLimitConcurrentTasksToMaxConcurrency() throws InterruptedException {
        executor = new BoundedVirtualThreadExecutor(2);
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        int taskCount = 6;
        CountDownLatch done = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            executor.execute(() -> {
                int current = inFlight.incrementAndGet();
                maxInFlight.updateAndGet(max -> Math.max(max, current));
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    inFlight.decrementAndGet();
                    done.countDown();
                }
            });
        }

        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(maxInFlight.get()).isLessThanOrEqualTo(2);
    }
}
