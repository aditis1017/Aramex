package com.customcharge.shipment.service;

import lombok.Getter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class ShipmentMessageConsumptionTracker {

    private final AtomicLong currentMinutePodCount = new AtomicLong();
    private final AtomicLong lifetimePodCount = new AtomicLong();

    @Getter
    private volatile long lastCompletedMinutePodCount = 0L;

    private volatile long lastCompletedMinuteBucket = 0L;

    private final ObjectProvider<ShipmentClusterMessageConsumptionStore> clusterStoreProvider;

    public ShipmentMessageConsumptionTracker(
            ObjectProvider<ShipmentClusterMessageConsumptionStore> clusterStoreProvider) {
        this.clusterStoreProvider = clusterStoreProvider;
    }

    public void recordConsumed() {
        currentMinutePodCount.incrementAndGet();
        lifetimePodCount.incrementAndGet();
        ShipmentClusterMessageConsumptionStore clusterStore = clusterStoreProvider.getIfAvailable();
        if (clusterStore != null) {
            clusterStore.incrementCurrentMinute();
        }
    }

    public long currentMinutePodCount() {
        return currentMinutePodCount.get();
    }

    public long lifetimePodCount() {
        return lifetimePodCount.get();
    }

    long currentMinuteClusterCount() {
        ShipmentClusterMessageConsumptionStore clusterStore = clusterStoreProvider.getIfAvailable();
        if (clusterStore == null) {
            return -1L;
        }
        return clusterStore.countForMinute(currentMinuteBucket());
    }

    public long lastCompletedMinuteClusterCount() {
        ShipmentClusterMessageConsumptionStore clusterStore = clusterStoreProvider.getIfAvailable();
        if (clusterStore == null) {
            return -1L;
        }
        return clusterStore.countForMinute(lastCompletedMinuteBucket);
    }

    public void completeMinuteWindow() {
        lastCompletedMinutePodCount = currentMinutePodCount.getAndSet(0L);
        lastCompletedMinuteBucket = currentMinuteBucket() - 1L;
    }

    private static long currentMinuteBucket() {
        return java.time.Instant.now().getEpochSecond() / 60L;
    }
}
