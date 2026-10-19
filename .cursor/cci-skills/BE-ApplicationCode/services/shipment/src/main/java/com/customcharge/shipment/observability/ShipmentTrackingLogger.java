package com.customcharge.shipment.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Structured tracking logs for OpenSearch drill-down (e.g. failed AWB table).
 * Emits {@code shipment_tracking} JSON lines consumed by Fluent Bit.
 */
@Component
public class ShipmentTrackingLogger {

    private static final Logger log = LoggerFactory.getLogger(ShipmentTrackingLogger.class);

    public static final String CHANNEL_RABBIT = "rabbit";
    public static final String CHANNEL_SQS = "sqs";

    public void logEntered(String hawb, String destinationEntity, String ingestChannel) {
        logEvent(hawb, destinationEntity, "SHIPMENT_ENTERED", null, null, ingestChannel);
    }

    public void logFilterPassed(String hawb, String destinationEntity) {
        logEvent(hawb, destinationEntity, "SHIPMENT_FILTER_PASSED", null, null, null);
    }

    public void logCreated(String hawb, String destinationEntity) {
        logEvent(hawb, destinationEntity, "SHIPMENT_CREATED", null, null, null);
    }

    public void logDuplicateSkipped(String hawb, String destinationEntity) {
        logEvent(hawb, destinationEntity, "SHIPMENT_DUPLICATE_SKIPPED", null, null, null);
    }

    /** @deprecated Use {@link #logCreated} for DB-aligned counts. */
    @Deprecated
    public void logPassed(String hawb, String destinationEntity) {
        logEvent(hawb, destinationEntity, "SHIPMENT_PASSED", null, null, null);
    }

    public void logFailedAwb(String hawb, String reason, String destinationEntity, String failureType) {
        logFailedAwb(hawb, reason, destinationEntity, failureType, null);
    }

    public void logFailedAwb(
            String hawb,
            String reason,
            String destinationEntity,
            String failureType,
            String ingestChannel) {
        logEvent(hawb, destinationEntity, "SHIPMENT_FAILED", failureType, reason, ingestChannel);
    }

    private void logEvent(
            String hawb,
            String destinationEntity,
            String event,
            String failureType,
            String reason,
            String ingestChannel) {
        safe(() -> {
            var builder = log.atInfo()
                    .addKeyValue("event", event)
                    .addKeyValue("hawb", ShipmentIngestMetrics.normalize(hawb))
                    .addKeyValue("destinationEntity", ShipmentIngestMetrics.normalize(destinationEntity));
            if (failureType != null) {
                builder.addKeyValue("failureType", ShipmentIngestMetrics.normalize(failureType));
            }
            if (reason != null) {
                builder.addKeyValue("reason", ShipmentIngestMetrics.normalize(reason));
            }
            if (ingestChannel != null && !ingestChannel.isBlank()) {
                builder.addKeyValue("ingestChannel", ShipmentIngestMetrics.normalize(ingestChannel));
            }
            builder.log("shipment_tracking");
        });
    }

    private static void safe(Runnable action) {
        try {
            action.run();
        } catch (Exception ignored) {
            // Tracking logs must never affect business flow.
        }
    }
}
