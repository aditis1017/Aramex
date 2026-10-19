package com.customcharge.shipment.observability;

import com.customcharge.shipment.config.ShipmentProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.customcharge.shipment.constants.LogMessages.SHIPMENT_PHASE_SUMMARY;
import static com.customcharge.shipment.constants.LogMessages.SHIPMENT_PHASE_TIMING;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentProcessingPhaseTimer {

    ShipmentProperties shipmentProperties;

    private static final ThreadLocal<Map<String, Long>> PHASE_DURATIONS = ThreadLocal.withInitial(LinkedHashMap::new);

    public void run(String hawb, String phase, Runnable action) {
        if (!isEnabled()) {
            action.run();
            return;
        }
        long startedAtNanos = System.nanoTime();
        try {
            action.run();
        } finally {
            record(hawb, phase, elapsedMillis(startedAtNanos));
        }
    }

    public <T> T supply(String hawb, String phase, Supplier<T> action) {
        if (!isEnabled()) {
            return action.get();
        }
        long startedAtNanos = System.nanoTime();
        try {
            return action.get();
        } finally {
            record(hawb, phase, elapsedMillis(startedAtNanos));
        }
    }

    public void record(String hawb, String phase, long durationMs) {
        if (!isEnabled()) {
            return;
        }
        log.debug(SHIPMENT_PHASE_TIMING, hawb, phase, durationMs);
        PHASE_DURATIONS.get().put(phase, durationMs);
    }

    public void logSummary(String hawb) {
        if (!isEnabled()) {
            return;
        }
        Map<String, Long> phases = PHASE_DURATIONS.get();
        if (phases.isEmpty()) {
            return;
        }
        long totalMs = phases.values().stream().mapToLong(Long::longValue).sum();
        log.debug(SHIPMENT_PHASE_SUMMARY, hawb, phases, totalMs);
        phases.clear();
    }

    public void clear() {
        PHASE_DURATIONS.remove();
    }

    private boolean isEnabled() {
        return shipmentProperties.getObservability().isPhaseTimingEnabled();
    }

    private static long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
