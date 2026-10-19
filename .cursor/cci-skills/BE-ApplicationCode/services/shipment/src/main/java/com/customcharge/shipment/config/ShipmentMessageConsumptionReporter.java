package com.customcharge.shipment.config;

import com.customcharge.shipment.service.ShipmentMessageConsumptionTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "shipment.listener-matrix", name = "trace-enabled", havingValue = "true", matchIfMissing = true)
public class ShipmentMessageConsumptionReporter {

    private final ShipmentMessageConsumptionTracker consumptionTracker;

    @Scheduled(fixedRateString = "${shipment.listener-matrix.consumption-report-interval-ms:60000}")
    public void reportPerMinuteConsumption() {
        consumptionTracker.completeMinuteWindow();
    }
}
