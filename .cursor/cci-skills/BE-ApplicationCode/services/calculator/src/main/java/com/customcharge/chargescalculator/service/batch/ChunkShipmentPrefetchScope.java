package com.customcharge.chargescalculator.service.batch;

import com.customcharge.persistencelayer.entities.Shipment;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared chunk-scoped shipment cache for parallel row processing within one chunk.
 */
public final class ChunkShipmentPrefetchScope {

    private static final AtomicReference<Map<String, Shipment>> ACTIVE = new AtomicReference<>();

    private ChunkShipmentPrefetchScope() {
    }

    public static void open(Map<String, Shipment> shipmentsByHawb) {
        ACTIVE.set(shipmentsByHawb);
    }

    public static Optional<Shipment> find(String hawb) {
        Map<String, Shipment> shipments = ACTIVE.get();
        if (shipments == null || hawb == null || hawb.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(shipments.get(hawb.strip()));
    }

    public static void close() {
        ACTIVE.set(null);
    }
}
