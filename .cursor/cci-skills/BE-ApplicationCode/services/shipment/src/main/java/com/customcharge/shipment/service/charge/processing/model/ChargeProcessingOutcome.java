package com.customcharge.shipment.service.charge.processing.model;

import com.customcharge.persistencelayer.entities.ChargeLineItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ChargeProcessingOutcome {

    Status status;
    List<ChargeLineItem> lineItems;
    String message;

    public enum Status {
        SUCCESS,
        SKIPPED_NO_RATE_SHEET,
        SKIPPED_NO_ITEMS,
        SKIPPED_UNSUPPORTED
    }

    public static ChargeProcessingOutcome success(List<ChargeLineItem> lineItems) {
        return new ChargeProcessingOutcome(
                Status.SUCCESS,
                List.copyOf(lineItems),
                null);
    }

    public static ChargeProcessingOutcome skippedNoRateSheet(String message) {
        return new ChargeProcessingOutcome(
                Status.SKIPPED_NO_RATE_SHEET,
                Collections.emptyList(),
                message);
    }

    public static ChargeProcessingOutcome skippedNoItems(String message) {
        return new ChargeProcessingOutcome(
                Status.SKIPPED_NO_ITEMS,
                Collections.emptyList(),
                message);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
