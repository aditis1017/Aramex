package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import com.customcharge.persistencelayer.repository.ShipmentRuleAssignmentRepository;
import com.customcharge.shipment.service.rules.RuleResolverSupport;
import org.springframework.transaction.annotation.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.customcharge.shipment.constants.LogMessages.SHIPMENT_DUPLICATE_SKIPPED;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentPersistenceWriter {

    ShipmentRepository shipmentRepository;
    ShipmentRuleAssignmentRepository shipmentRuleAssignmentRepository;

    /**
     * Persists shipment and rule assignments in a short transaction.
     *
     * @return the saved shipment when created, or empty when the HAWB already exists
     */
    @Transactional
    public Optional<Shipment> saveNewShipment(
            Shipment shipment,
            String hawb,
            List<RuleResolverSupport.RuleAssignment> ruleAssignments) {
        try {
            shipmentRepository.save(shipment);
        } catch (DataIntegrityViolationException ex) {
            if (shipmentRepository.existsByHawbNumber(hawb)) {
                log.warn(SHIPMENT_DUPLICATE_SKIPPED, hawb);
                return Optional.empty();
            }
            throw ex;
        }

        RuleResolverSupport.saveAssignments(shipmentRuleAssignmentRepository, hawb, ruleAssignments);
        return Optional.of(shipment);
    }
}
