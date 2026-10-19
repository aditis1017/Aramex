package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.shipment.observability.ShipmentProcessingPhaseTimer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.customcharge.shipment.constants.LogMessages.SHIPMENT_DUPLICATE_SKIPPED;

/**
 * Single database transaction for new-HAWB ingest: shipment persist and charge creation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentIngestTransactionService {

    ShipmentPersistenceWriter shipmentPersistenceWriter;
    ChargeService chargeService;
    ShipmentProcessingPhaseTimer phaseTimer;

    /**
     * @return saved shipment when persist and charges completed; empty when HAWB raced as duplicate on insert
     */
    @Transactional(rollbackFor = Exception.class)
    public Optional<Shipment> persistAndCreateCharges(PreparedNewShipment prepared, ShipmentDto message) {
        String hawb = prepared.hawb();
        Optional<Shipment> saved = shipmentPersistenceWriter.saveNewShipment(
                prepared.shipment(), hawb, prepared.ruleAssignments());
        if (saved.isEmpty()) {
            return Optional.empty();
        }
        phaseTimer.run(hawb, "chargeCreation", () -> chargeService.createCharges(saved.get()));
        return saved;
    }
}
