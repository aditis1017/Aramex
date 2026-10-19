package com.customcharge.persistencelayer.security.entra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Small bounded retry helper to avoid adding Spring Retry as a dependency.
 */
public final class ResilientExecutor {

    private static final Logger log = LoggerFactory.getLogger(ResilientExecutor.class);

    private ResilientExecutor() {
    }

    public static <T> T executeWithRetry(
            String operation,
            int maxAttempts,
            Duration backoff,
            Supplier<T> supplier) {

        Exception last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return supplier.get();
            } catch (Exception ex) {
                last = ex;
                log.warn("{} failed (attempt {}/{}): {}", operation, attempt, maxAttempts, ex.toString());
                if (attempt < maxAttempts) {
                    sleepQuietly(backoff.toMillis());
                }
            }
        }
        throw new IllegalStateException(operation + " failed after " + maxAttempts + " attempts", last);
    }

    public static void executeVoidWithRetry(
            String operation,
            int maxAttempts,
            Duration backoff,
            Runnable runnable) {

        executeWithRetry(operation, maxAttempts, backoff, () -> {
            runnable.run();
            return null;
        });
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during retry backoff", ie);
        }
    }
}
