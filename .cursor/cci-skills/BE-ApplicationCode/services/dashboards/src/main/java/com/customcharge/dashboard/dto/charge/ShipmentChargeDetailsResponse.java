package com.customcharge.dashboard.dto.charge;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class ShipmentChargeDetailsResponse {

    String hawbNumber;
    String requestedChargeType;
    List<ChargeTypeDetailsDTO> charges;
}
