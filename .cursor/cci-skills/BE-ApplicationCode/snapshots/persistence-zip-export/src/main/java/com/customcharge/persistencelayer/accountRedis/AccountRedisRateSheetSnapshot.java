package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;

import java.util.List;

public record AccountRedisRateSheetSnapshot(RateSheet rateSheet, List<RateSheetItem> items) {
}
