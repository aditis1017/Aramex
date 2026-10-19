package com.customcharge.shipment.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipmentMessageConsumptionTrackerTest {

    @Test
    void recordConsumed_shouldIncrementPodAndClusterCounters() {
        ShipmentClusterMessageConsumptionStore clusterStore = mock(ShipmentClusterMessageConsumptionStore.class);
        ObjectProvider<ShipmentClusterMessageConsumptionStore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(clusterStore);

        ShipmentMessageConsumptionTracker tracker = new ShipmentMessageConsumptionTracker(provider);
        tracker.recordConsumed();
        tracker.recordConsumed();

        assertThat(tracker.currentMinutePodCount()).isEqualTo(2L);
        assertThat(tracker.lifetimePodCount()).isEqualTo(2L);
        verify(clusterStore, times(2)).incrementCurrentMinute();
    }

    @Test
    void completeMinuteWindow_shouldResetCurrentMinuteCounter() {
        ObjectProvider<ShipmentClusterMessageConsumptionStore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        ShipmentMessageConsumptionTracker tracker = new ShipmentMessageConsumptionTracker(provider);
        tracker.recordConsumed();
        tracker.recordConsumed();
        tracker.recordConsumed();
        tracker.completeMinuteWindow();

        assertThat(tracker.getLastCompletedMinutePodCount()).isEqualTo(3L);
        assertThat(tracker.currentMinutePodCount()).isZero();
    }
}
