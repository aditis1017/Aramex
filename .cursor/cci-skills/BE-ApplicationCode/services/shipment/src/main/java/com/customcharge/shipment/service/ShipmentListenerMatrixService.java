package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.config.datasource.DatasourceProperties;
import com.customcharge.shipment.dto.ShipmentListenerMatrixResponse;
import com.customcharge.shipment.service.ShipmentMessageConsumptionTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentListenerMatrixService {

    private final Environment environment;
    private final RabbitProperties rabbitProperties;
    private final DatasourceProperties datasourceProperties;
    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;
    private final ShipmentMessageConsumptionTracker consumptionTracker;

    public ShipmentListenerMatrixResponse captureMatrix() {
        RabbitProperties.SimpleContainer listener = rabbitProperties.getListener().getSimple();
        int minConcurrency = listener.getConcurrency() != null ? listener.getConcurrency() : 1;
        int maxConcurrency = listener.getMaxConcurrency() != null ? listener.getMaxConcurrency() : minConcurrency;
        int prefetch = listener.getPrefetch() != null ? listener.getPrefetch() : 1;
        boolean defaultRequeueRejected = Boolean.TRUE.equals(listener.getDefaultRequeueRejected());
        String annotationConcurrency = minConcurrency + "-" + maxConcurrency;

        List<ShipmentListenerMatrixResponse.ListenerRow> rows = new ArrayList<>();
        for (MessageListenerContainer container : rabbitListenerEndpointRegistry.getListenerContainers()) {
            if (container instanceof SimpleMessageListenerContainer simpleContainer) {
                rows.add(ShipmentListenerMatrixResponse.ListenerRow.builder()
                        .queues(Arrays.asList(simpleContainer.getQueueNames()))
                        .configuredMinConcurrency(minConcurrency)
                        .configuredMaxConcurrency(maxConcurrency)
                        .configuredPrefetch(prefetch)
                        .configuredDefaultRequeueRejected(defaultRequeueRejected)
                        .annotationConcurrency(annotationConcurrency)
                        .activeConsumerCount(simpleContainer.getActiveConsumerCount())
                        .running(simpleContainer.isRunning())
                        .build());
            }
        }

        long clusterCount = consumptionTracker.lastCompletedMinuteClusterCount();

        return ShipmentListenerMatrixResponse.builder()
                .capturedAt(Instant.now())
                .instanceId(resolveInstanceId())
                .queue(environment.getProperty("queue.client-shipment"))
                .rabbitHost(rabbitProperties.getHost())
                .rabbitVirtualHost(rabbitProperties.getVirtualHost())
                .datasourceMaxPoolSize(datasourceProperties.getMaximumPoolSize())
                .datasourceMinIdle(datasourceProperties.getMinimumIdle())
                .messagesConsumedCurrentMinutePod(consumptionTracker.currentMinutePodCount())
                .messagesConsumedLastMinutePod(consumptionTracker.getLastCompletedMinutePodCount())
                .messagesConsumedLifetimePod(consumptionTracker.lifetimePodCount())
                .messagesConsumedLastMinuteAllPods(clusterCount >= 0 ? clusterCount : null)
                .listeners(rows)
                .build();
    }

    private String resolveInstanceId() {
        String hostname = environment.getProperty("HOSTNAME");
        if (hostname != null && !hostname.isBlank()) {
            return hostname.trim();
        }
        String podName = environment.getProperty("POD_NAME");
        if (podName != null && !podName.isBlank()) {
            return podName.trim();
        }
        return environment.getProperty("spring.application.name", "shipment-service");
    }
}
