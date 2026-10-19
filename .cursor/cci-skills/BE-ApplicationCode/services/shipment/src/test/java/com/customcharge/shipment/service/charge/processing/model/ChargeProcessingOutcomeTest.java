package com.customcharge.shipment.service.charge.processing.model;

import com.customcharge.persistencelayer.entities.ChargeLineItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChargeProcessingOutcomeTest {

    @Test
    void success_shouldCopyLineItemsAndMarkSuccess() {
        ChargeLineItem lineItem = new ChargeLineItem();

        ChargeProcessingOutcome outcome = ChargeProcessingOutcome.success(List.of(lineItem));

        assertThat(outcome.isSuccess()).isTrue();
        assertThat(outcome.getStatus()).isEqualTo(ChargeProcessingOutcome.Status.SUCCESS);
        assertThat(outcome.getLineItems()).containsExactly(lineItem);
        assertThat(outcome.getMessage()).isNull();
    }

    @Test
    void skippedFactories_shouldReturnEmptyLineItems() {
        ChargeProcessingOutcome noRateSheet = ChargeProcessingOutcome.skippedNoRateSheet("missing rate sheet");
        ChargeProcessingOutcome noItems = ChargeProcessingOutcome.skippedNoItems("no items");

        assertThat(noRateSheet.isSuccess()).isFalse();
        assertThat(noRateSheet.getStatus()).isEqualTo(ChargeProcessingOutcome.Status.SKIPPED_NO_RATE_SHEET);
        assertThat(noRateSheet.getMessage()).isEqualTo("missing rate sheet");
        assertThat(noRateSheet.getLineItems()).isEmpty();

        assertThat(noItems.getStatus()).isEqualTo(ChargeProcessingOutcome.Status.SKIPPED_NO_ITEMS);
    }
}
