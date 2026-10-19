package com.customcharge.chargescalculator.service.batch;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.shipmentingest.CalculatorShipmentIngestionService;
import com.customcharge.chargescalculator.shipmentingest.coredata.CoreDataShipmentApi;
import com.customcharge.chargescalculator.shipmentingest.coredata.CoreDataShipmentProperties;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShipmentLookupService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentLookupService.class);

    private final ShipmentRepository shipmentRepository;
    private final CoreDataShipmentProperties coreDataShipmentProperties;
    private final ObjectProvider<CoreDataShipmentApi> coreDataShipmentApi;
    private final CalculatorShipmentIngestionService calculatorShipmentIngestionService;

    public ShipmentLookupService(
            ShipmentRepository shipmentRepository,
            CoreDataShipmentProperties coreDataShipmentProperties,
            ObjectProvider<CoreDataShipmentApi> coreDataShipmentApi,
            CalculatorShipmentIngestionService calculatorShipmentIngestionService) {
        this.shipmentRepository = shipmentRepository;
        this.coreDataShipmentProperties = coreDataShipmentProperties;
        this.coreDataShipmentApi = coreDataShipmentApi;
        this.calculatorShipmentIngestionService = calculatorShipmentIngestionService;
    }

    public Shipment resolveByHawb(String awb) {
        String normalizedAwb = awb == null ? "" : awb.strip();
        Optional<Shipment> prefetched = ChunkShipmentPrefetchScope.find(normalizedAwb);
        if (prefetched.isPresent()) {
            return prefetched.get();
        }
        Optional<Shipment> existing = shipmentRepository.findByHawbNumber(normalizedAwb);
        if (existing.isPresent()) {
            return existing.get();
        }

        throw new ResourceNotFoundException(
                BatchProcessingConstants.ERROR_SHIPMENT_NOT_FOUND + ": " + normalizedAwb);
//        return fetchFromCoreDataAndIngest(normalizedAwb)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        BatchProcessingConstants.ERROR_SHIPMENT_NOT_FOUND + ": " + normalizedAwb));
    }

    public Map<String, Shipment> prefetchByHawbNumbers(Collection<String> awbs) {
        if (awbs == null || awbs.isEmpty()) {
            return Map.of();
        }
        List<String> normalizedAwbs = awbs.stream()
                .filter(awb -> awb != null && !awb.isBlank())
                .map(String::strip)
                .distinct()
                .collect(Collectors.toList());
        if (normalizedAwbs.isEmpty()) {
            return Map.of();
        }
        Map<String, Shipment> shipmentsByHawb = new HashMap<>();
        for (Shipment shipment : shipmentRepository.findByHawbNumberIn(normalizedAwbs)) {
            if (shipment.getHawbNumber() != null && !shipment.getHawbNumber().isBlank()) {
                shipmentsByHawb.put(shipment.getHawbNumber().strip(), shipment);
            }
        }
        return shipmentsByHawb;
    }

    private Optional<Shipment> fetchFromCoreDataAndIngest(String awb) {
        if (!coreDataShipmentProperties.isEnabled()) {
            return Optional.empty();
        }

        CoreDataShipmentApi api = coreDataShipmentApi.getIfAvailable();
        if (api == null) {
            log.warn("Shipment details API enabled but bean is unavailable for awb={}", awb);
            return Optional.empty();
        }

        Optional<ShipmentDto> dto = api.fetchShipmentByAwb(awb);
        if (dto.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(calculatorShipmentIngestionService.ingest(dto.get()));
        } catch (DataIntegrityViolationException ex) {
            return shipmentRepository.findByHawbNumber(awb);
        }
    }
}
