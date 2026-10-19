package com.customcharge.shipment.observability;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Records filter-pass metrics and tracking logs after business fullness rules run.
 */
@Aspect
@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentCompletenessCheckerObservabilityAspect {

    AsyncShipmentIngestMetrics ingestMetrics;
    ShipmentTrackingLogger trackingLogger;

    @AfterReturning(
            pointcut = "execution(* com.customcharge.shipment.service.ShipmentCompletenessChecker.applyShipmentFilterRules(..)) && args(shipment)")
    public void afterFilterRules(ShipmentDto shipment) {
        if (!Boolean.TRUE.equals(shipment.getIsShipmentFull())) {
            return;
        }
        String hawb = shipment.getHawb() != null ? shipment.getHawb() : "unknown";
        String destinationEntity = ShipmentDestinationEntitySupport.resolve(shipment);
        ingestMetrics.recordFilterPassed(destinationEntity);
        trackingLogger.logFilterPassed(hawb, destinationEntity);
    }
}
