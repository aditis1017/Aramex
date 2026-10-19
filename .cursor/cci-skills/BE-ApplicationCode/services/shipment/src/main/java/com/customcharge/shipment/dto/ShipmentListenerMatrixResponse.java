package com.customcharge.shipment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ShipmentListenerMatrixResponse {

    private final Instant capturedAt;
    private final String instanceId;
    private final String queue;
    private final String rabbitHost;
    private final String rabbitVirtualHost;
    private final int datasourceMaxPoolSize;
    private final int datasourceMinIdle;
    private final long messagesConsumedCurrentMinutePod;
    private final long messagesConsumedLastMinutePod;
    private final long messagesConsumedLifetimePod;
    private final Long messagesConsumedLastMinuteAllPods;
    private final List<ListenerRow> listeners;

    @Getter
    @Builder
    public static class ListenerRow {
        private final List<String> queues;
        private final int configuredMinConcurrency;
        private final int configuredMaxConcurrency;
        private final int configuredPrefetch;
        private final boolean configuredDefaultRequeueRejected;
        private final String annotationConcurrency;
        private final int activeConsumerCount;
        private final boolean running;
    }
}
