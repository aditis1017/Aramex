package com.customcharge.dashboard.dto.charge;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Value
@Builder
@Jacksonized
public class ShipmentGoodsSummaryDTO {

    String description;
    BigDecimal collectedValue;
    String collectedValueCurrency;
    String deliveryType;
    String ioss;
    String hsCode;
    String services;
}
