package com.customcharge.shipment.observability;

import com.customcharge.persistencelayer.observability.CustomChargeMetricsProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Non-blocking facade for {@link ShipmentIngestMetrics}. Recording runs on a dedicated
 * daemon thread so ingest threads are never blocked by Micrometer registry work.
 */
@Component
public class AsyncShipmentIngestMetrics {

    private final CustomChargeMetricsProperties properties;
    private final ObjectProvider<ShipmentIngestMetrics> metrics;
    private final ExecutorService executor;

    public AsyncShipmentIngestMetrics(
            CustomChargeMetricsProperties properties,
            ObjectProvider<ShipmentIngestMetrics> metrics) {
        this.properties = properties;
        this.metrics = metrics;
        this.executor = Executors.newSingleThreadExecutor(daemonFactory("shipment-metrics"));
    }

    public void recordRabbitReceived(String path) {
        run(metrics -> metrics.recordRabbitReceived(path));
    }

    public void recordMessageEntered(String destinationEntity) {
        run(metrics -> metrics.recordMessageEntered(destinationEntity));
    }

    public void recordValidationFailed(String reason, String destinationEntity) {
        run(metrics -> metrics.recordValidationFailed(reason, destinationEntity));
    }

    public void recordFilterPassed(String destinationEntity) {
        run(metrics -> metrics.recordFilterPassed(destinationEntity));
    }

    public void recordEntityFailed(String destinationEntity, String reason) {
        run(metrics -> metrics.recordEntityFailed(destinationEntity, reason));
    }

    public void recordEntityCreated(String destinationEntity) {
        run(metrics -> metrics.recordEntityCreated(destinationEntity));
    }

    public void recordEntityPassed(String destinationEntity) {
        run(metrics -> metrics.recordEntityPassed(destinationEntity));
    }

    private void run(java.util.function.Consumer<ShipmentIngestMetrics> action) {
        if (!properties.getShipmentIngest().isEnabled()) {
            return;
        }
        ShipmentIngestMetrics delegate = metrics.getIfAvailable();
        if (delegate == null) {
            return;
        }
        executor.execute(() -> action.accept(delegate));
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }
}
