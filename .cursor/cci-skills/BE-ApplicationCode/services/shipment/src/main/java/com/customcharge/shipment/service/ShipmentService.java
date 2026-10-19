package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentService {

    ShipmentPersistenceService shipmentPersistenceService;
    ShipmentIngestTransactionService shipmentIngestTransactionService;
    ShipmentCompletenessChecker shipmentCompletenessChecker;
    ShipmentMawbLinkageService shipmentMawbLinkageService;

    public void processShipment(ShipmentDto message) {
        Optional<PreparedNewShipment> prepared = shipmentPersistenceService.prepareNewShipment(message);
        prepared.ifPresent(preparedNewShipment -> shipmentIngestTransactionService.persistAndCreateCharges(preparedNewShipment, message));
    }
    public void handleSh022(ShipmentDto message) {
        String hawb = message != null && message.getHawb() != null ? message.getHawb() : "unknown";
        try {
            shipmentCompletenessChecker.applyMawbFilterRules(message);
            if (Boolean.TRUE.equals(message.getIsMawbShipment())) {
                shipmentMawbLinkageService.applyLinkage(message);
            } else {
                log.info("SH022 hawb={}: MAWB filter did not match", hawb);
            }
        } catch (RuntimeException ex) {
            log.warn("SH022 MAWB processing failed for hawb={}: {}", hawb, ex.getMessage());
            throw ex;
        }
    }
}


