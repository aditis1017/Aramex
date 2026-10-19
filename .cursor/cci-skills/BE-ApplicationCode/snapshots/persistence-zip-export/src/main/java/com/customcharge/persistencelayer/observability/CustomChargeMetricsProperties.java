package com.customcharge.persistencelayer.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "customcharge.metrics")
public class CustomChargeMetricsProperties {

    private final FileProcessing fileProcessing = new FileProcessing();
    private final ShipmentIngest shipmentIngest = new ShipmentIngest();
    private final Snapshot snapshot = new Snapshot();

    public FileProcessing getFileProcessing() {
        return fileProcessing;
    }

    public ShipmentIngest getShipmentIngest() {
        return shipmentIngest;
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public static class FileProcessing {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class ShipmentIngest {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Snapshot {
        private boolean enabled = false;
        private long intervalMs = 60_000L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getIntervalMs() {
            return intervalMs;
        }

        public void setIntervalMs(long intervalMs) {
            this.intervalMs = intervalMs;
        }
    }
}
