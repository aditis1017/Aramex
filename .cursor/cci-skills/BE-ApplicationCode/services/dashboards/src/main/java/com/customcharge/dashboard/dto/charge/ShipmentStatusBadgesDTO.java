package com.customcharge.dashboard.dto.charge;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ShipmentStatusBadgesDTO {

    String operation;
    String charge;
    String category;
    String handler;
}
