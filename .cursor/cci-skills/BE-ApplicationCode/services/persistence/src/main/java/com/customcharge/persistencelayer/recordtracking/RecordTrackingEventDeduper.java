package com.customcharge.persistencelayer.recordtracking;

import org.slf4j.MDC;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prevents duplicate tracking events (SQS retries, double invocation).
 */
public final class RecordTrackingEventDeduper {

    private static final int MAX_KEYS = 10_000;
    private static final long TTL_MS = 24L * 60L * 60L * 1000L;
    private static final ConcurrentHashMap<String, Long> EMITTED_AT = new ConcurrentHashMap<>();

    private RecordTrackingEventDeduper() {
    }

    public static boolean tryAcquire(RecordTrackingEvent event) {
        if (!event.oncePerChunk() && !event.oncePerRecord() && !event.oncePerFile()) {
            return true;
        }
        String key = eventKey(event);
        if (key == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long previous = EMITTED_AT.putIfAbsent(key, now);
        if (EMITTED_AT.size() > MAX_KEYS) {
            pruneExpired(now);
        }
        return previous == null;
    }

    private static String eventKey(RecordTrackingEvent event) {
        String traceId = MDC.get(RecordTrackingMdc.TRACE_ID);
        String fileId = MDC.get(RecordTrackingMdc.FILE_ID);
        if (traceId == null || fileId == null) {
            return null;
        }
        if (event.oncePerFile()) {
            return traceId + '|' + fileId + '|' + event.name();
        }
        if (event.oncePerRecord()) {
            String recordId = MDC.get(RecordTrackingMdc.RECORD_ID);
            if (recordId == null) {
                return null;
            }
            String chunkId = MDC.get(RecordTrackingMdc.CHUNK_ID);
            return traceId + '|' + fileId + '|' + chunkId + '|' + recordId + '|' + event.name();
        }
        String chunkId = MDC.get(RecordTrackingMdc.CHUNK_ID);
        if (chunkId == null) {
            return null;
        }
        return traceId + '|' + fileId + '|' + chunkId + '|' + event.name();
    }

    private static void pruneExpired(long now) {
        Iterator<Map.Entry<String, Long>> iterator = EMITTED_AT.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > TTL_MS) {
                iterator.remove();
            }
        }
    }
}
