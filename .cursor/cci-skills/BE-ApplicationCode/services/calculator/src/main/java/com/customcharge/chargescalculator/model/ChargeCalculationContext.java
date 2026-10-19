package com.customcharge.chargescalculator.model;

import com.customcharge.persistencelayer.entities.Shipment;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ChargeCalculationContext {
    private final Shipment shipment;
    private final Map<String, String> sourceRecord;
    private final String rawUploadFileId;
    private final String processFor;
    private final Integer chunkIndex;
    private final Map<String, Object> calculatedCharges = new HashMap<>();

    public ChargeCalculationContext(
            Shipment shipment,
            Map<String, String> sourceRecord,
            String rawUploadFileId,
            String processFor, Integer chunkIndex) {
        this.shipment = shipment;
        this.sourceRecord = sourceRecord;
        this.rawUploadFileId = rawUploadFileId;
        this.processFor = processFor;
        this.chunkIndex = chunkIndex;
    }
}
