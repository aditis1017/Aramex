package com.customcharge.chargescalculator.service.batch;

/**
 * Thread-local scope for per-row lookup caches during batch upload processing.
 */
public final class RecordProcessingLookupScope {

    private static final ThreadLocal<RecordProcessingLookupCache> ACTIVE = new ThreadLocal<>();

    private RecordProcessingLookupScope() {
    }

    public static RecordProcessingLookupCache get() {
        return ACTIVE.get();
    }

    public static void open(RecordProcessingLookupCache cache) {
        ACTIVE.set(cache);
    }

    public static void close() {
        ACTIVE.remove();
    }
}
