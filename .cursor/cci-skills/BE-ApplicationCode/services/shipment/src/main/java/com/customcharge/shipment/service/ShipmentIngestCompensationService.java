package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import com.customcharge.persistencelayer.repository.ShipmentRuleAssignmentRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.customcharge.shipment.constants.LogMessages.SHIPMENT_INGEST_ROLLBACK;

/**
 * Removes a newly persisted shipment when charge creation fails, so ingest stays all-or-nothing.
 */
@Deprecated
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentIngestCompensationService {

    ShipmentRepository shipmentRepository;
    ShipmentRuleAssignmentRepository shipmentRuleAssignmentRepository;

    @Transactional
    public void rollbackNewShipmentAfterChargeFailure(Shipment shipment, String hawb) {
        if (shipment == null || shipment.getId() == null) {
            return;
        }
        UUID shipmentId = shipment.getId();
        shipmentRuleAssignmentRepository.deleteByHawbNumber(hawb);
        shipmentRepository.deleteById(shipmentId);
        log.warn(SHIPMENT_INGEST_ROLLBACK, hawb, shipmentId);
    }
}
