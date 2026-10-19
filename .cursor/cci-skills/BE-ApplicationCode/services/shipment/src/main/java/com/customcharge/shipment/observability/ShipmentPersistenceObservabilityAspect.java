package com.customcharge.shipment.observability;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import com.customcharge.shipment.service.PreparedNewShipment;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Records DB-aligned tracking when a shipment row is created or skipped as duplicate.
 */
@Aspect
@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentPersistenceObservabilityAspect {

    AsyncShipmentIngestMetrics ingestMetrics;
    ShipmentTrackingLogger trackingLogger;
    ShipmentRepository shipmentRepository;

    @AfterReturning(
            pointcut = "execution(* com.customcharge.shipment.service.ShipmentIngestTransactionService.persistAndCreateCharges(..)) && args(prepared, shipment)",
            returning = "result")
    public void afterIngestTransaction(
            PreparedNewShipment prepared,
            ShipmentDto shipment,
            Optional<Shipment> result) {
        recordPersistOutcome(shipment, result);
    }

    private void recordPersistOutcome(ShipmentDto shipment, Optional<Shipment> result) {
        if (result != null && result.isPresent()) {
            String hawb = shipment.getHawb() != null ? shipment.getHawb() : "unknown";
            String destinationEntity = ShipmentDestinationEntitySupport.resolve(shipment);
            ingestMetrics.recordEntityCreated(destinationEntity);
            trackingLogger.logCreated(hawb, destinationEntity);
            return;
        }
        String hawb = shipment.getHawb();
        if (!StringUtils.hasText(hawb)) {
            return;
        }
        if (shipmentRepository.existsByHawbNumber(hawb.trim())) {
            String destinationEntity = ShipmentDestinationEntitySupport.resolve(shipment);
            trackingLogger.logDuplicateSkipped(hawb, destinationEntity);
        }
    }
}
