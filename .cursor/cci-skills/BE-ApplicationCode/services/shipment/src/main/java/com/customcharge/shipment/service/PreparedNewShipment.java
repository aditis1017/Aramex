package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.shipment.service.rules.RuleResolverSupport;

import java.util.List;

/**
 * In-memory shipment and rule assignments ready for a single transactional persist + charge ingest.
 */
public record PreparedNewShipment(
        Shipment shipment,
        String hawb,
        List<RuleResolverSupport.RuleAssignment> ruleAssignments) {
}
