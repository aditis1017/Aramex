package com.customcharge.chargescalculator.service.calculation;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.persistencelayer.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ShipmentTypeChargeCalculatorRouter {

    private static final Logger log = LoggerFactory.getLogger(ShipmentTypeChargeCalculatorRouter.class);

    private final Map<String, ChargeCalculation> calculatorsByCode;
    private final Map<String, Map<String, List<String>>> processForShipmentTypeMatrix;

    public ShipmentTypeChargeCalculatorRouter(List<ChargeCalculation> calculators) {
        this.calculatorsByCode = calculators.stream()
                .collect(java.util.stream.Collectors.toMap(ChargeCalculation::getChargeCode, c -> c));
        this.processForShipmentTypeMatrix = Map.of(
                BatchProcessingConstants.PROCESS_FOR_ESTIMATE, Map.of(
                        BatchProcessingConstants.SHIPMENT_TYPE_DDU, List.of("ECD", "DTC"),
                        BatchProcessingConstants.SHIPMENT_TYPE_DDO, List.of("ECD", "DTC"),
                        BatchProcessingConstants.SHIPMENT_TYPE_DDP, List.of("ECD", "DTA","ECS","EC3", "DTS","DT3")
                ),
                BatchProcessingConstants.PROCESS_FOR_ACTUAL, Map.of(
                        BatchProcessingConstants.SHIPMENT_TYPE_DDU, List.of("ECD"),
                        BatchProcessingConstants.SHIPMENT_TYPE_DDO, List.of("ECD"),
                        BatchProcessingConstants.SHIPMENT_TYPE_DDP, List.of("ECD")
                )
        );
    }

    public List<ChargeCalculation> getCalculators(String processFor, String shipmentType) {
        if (processFor == null || processFor.isBlank()) {
            throw new ValidationException(BatchProcessingConstants.ERROR_UNSUPPORTED_PROCESS_FOR + ": " + processFor);
        }
        Map<String, List<String>> shipmentTypeMatrix = processForShipmentTypeMatrix.get(processFor);
        if (shipmentTypeMatrix == null) {
            throw new ValidationException(BatchProcessingConstants.ERROR_UNSUPPORTED_PROCESS_FOR + ": " + processFor);
        }

        if (shipmentType == null || shipmentType.isBlank()) {
            throw new ValidationException(BatchProcessingConstants.ERROR_MISSING_SHIPMENT_TYPE);
        }
        List<String> chargeCodes = shipmentTypeMatrix.get(shipmentType);
        if (chargeCodes == null) {
            throw new ValidationException(BatchProcessingConstants.ERROR_UNSUPPORTED_SHIPMENT_TYPE + ": " + shipmentType);
        }
        log.debug("Resolved charge calculators processFor={} shipmentType={} chargeCodes={}", processFor, shipmentType, chargeCodes);
        return chargeCodes.stream()
                .map(calculatorsByCode::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
