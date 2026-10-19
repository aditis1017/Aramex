package com.customcharge.shipment.config;

import com.customcharge.shipment.dto.ShipmentListenerMatrixResponse;
import com.customcharge.shipment.service.ShipmentListenerMatrixService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.customcharge.shipment.constants.LogMessages.SHIPMENT_LISTENER_MATRIX_TRACE;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "shipment.listener-matrix", name = "trace-enabled", havingValue = "true", matchIfMissing = true)
public class ShipmentListenerMatrixTracer {

    private final ShipmentListenerMatrixService shipmentListenerMatrixService;

    @EventListener(ApplicationReadyEvent.class)
    public void traceOnStartup() {
        traceMatrix();
    }

    @Scheduled(fixedDelayString = "${shipment.listener-matrix.trace-interval-ms:60000}")
    public void traceMatrix() {
        ShipmentListenerMatrixResponse matrix = shipmentListenerMatrixService.captureMatrix();
        for (ShipmentListenerMatrixResponse.ListenerRow row : matrix.getListeners()) {
            log.debug(
                    SHIPMENT_LISTENER_MATRIX_TRACE,
                    matrix.getInstanceId(),
                    matrix.getQueue(),
                    matrix.getRabbitHost(),
                    matrix.getRabbitVirtualHost(),
                    matrix.getDatasourceMaxPoolSize(),
                    matrix.getDatasourceMinIdle(),
                    String.join(",", row.getQueues()),
                    row.getConfiguredMinConcurrency(),
                    row.getConfiguredMaxConcurrency(),
                    row.getAnnotationConcurrency(),
                    row.getConfiguredPrefetch(),
                    row.isConfiguredDefaultRequeueRejected(),
                    row.getActiveConsumerCount(),
                    row.isRunning());
        }
        if (matrix.getListeners().isEmpty()) {
            log.warn("event=shipment_listener_matrix_empty instanceId={} queue={}", matrix.getInstanceId(), matrix.getQueue());
        }
    }
}
