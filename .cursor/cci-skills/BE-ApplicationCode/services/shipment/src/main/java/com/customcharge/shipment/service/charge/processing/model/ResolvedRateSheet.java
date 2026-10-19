package com.customcharge.shipment.service.charge.processing.model;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;

import java.util.List;

/**
 * Rate sheet resolved for a charge together with billable items and owning account.
 */
public record ResolvedRateSheet(
        Account account,
        RateSheet rateSheet,
        List<RateSheetItem> items) {
}
