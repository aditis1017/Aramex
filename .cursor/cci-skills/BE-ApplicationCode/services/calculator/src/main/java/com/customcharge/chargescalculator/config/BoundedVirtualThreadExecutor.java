package com.customcharge.chargescalculator.config;

import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Caps concurrent row work while using virtual threads for each submitted task.
 * Semantics match a fixed-size platform thread pool: at most {@code maxConcurrency} tasks run at once.
 */
final class BoundedVirtualThreadExecutor implements Executor, DisposableBean {

    private final Semaphore permits;
    private final ExecutorService delegate;

    BoundedVirtualThreadExecutor(int maxConcurrency) {
        int size = Math.max(1, maxConcurrency);
        this.permits = new Semaphore(size);
        this.delegate = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("chunk-row-", 0).factory());
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(() -> runWithPermit(command));
    }

    private void runWithPermit(Runnable command) {
        try {
            permits.acquire();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            command.run();
        } finally {
            permits.release();
        }
    }

    @Override
    public void destroy() {
        delegate.shutdown();
    }
}
