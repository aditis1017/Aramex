package com.customcharge.shipment.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Micrometer counters for shipment ingest observability.
 * All recording is wrapped in {@link #safe} so metrics never affect business flow.
 */
public class ShipmentIngestMetrics {

    public static final String PREFIX = "customcharge.shipment.ingest";

    private final MeterRegistry registry;

    public ShipmentIngestMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRabbitReceived(String path) {
        safe(() -> counter("rabbit.received", "path", path).increment());
    }

    public void recordMessageEntered(String destinationEntity) {
        safe(() -> {
            counter("messages.entered", "destination_entity", destinationEntity).increment();
            counter("entity.outcome", "destination_entity", destinationEntity, "outcome", "entered").increment();
        });
    }

    public void recordValidationFailed(String reason, String destinationEntity) {
        safe(() -> counter("validation.failed",
                "reason", reason,
                "destination_entity", destinationEntity).increment());
    }

    public void recordFilterPassed(String destinationEntity) {
        safe(() -> counter("filter.passed", "destination_entity", destinationEntity).increment());
    }

    public void recordEntityFailed(String destinationEntity, String reason) {
        safe(() -> {
            counter("entity.outcome",
                    "destination_entity", destinationEntity,
                    "outcome", "failed").increment();
            if ("validation".equals(reason)) {
                counter("validation.failed",
                        "reason", reason,
                        "destination_entity", destinationEntity).increment();
            }
        });
    }

    public void recordEntityCreated(String destinationEntity) {
        safe(() -> counter("entity.outcome",
                "destination_entity", destinationEntity,
                "outcome", "created").increment());
    }

    /** @deprecated Use {@link #recordEntityCreated} — passed counted duplicate/skipped HAWBs. */
    @Deprecated
    public void recordEntityPassed(String destinationEntity) {
        safe(() -> counter("entity.outcome",
                "destination_entity", destinationEntity,
                "outcome", "passed").increment());
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(PREFIX + "." + name).tags(normalizeTags(tags)).register(registry);
    }

    private String[] normalizeTags(String... tags) {
        if (tags == null || tags.length == 0) {
            return tags;
        }
        String[] normalized = new String[tags.length];
        for (int i = 0; i < tags.length; i++) {
            normalized[i] = i % 2 == 1 ? normalize(tags[i]) : tags[i];
        }
        return normalized;
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }

    private static void safe(Runnable action) {
        try {
            action.run();
        } catch (Exception ignored) {
            // Metrics must never affect business flow.
        }
    }
}
