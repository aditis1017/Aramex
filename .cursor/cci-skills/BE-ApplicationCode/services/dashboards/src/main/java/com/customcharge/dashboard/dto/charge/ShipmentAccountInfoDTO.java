package com.customcharge.dashboard.dto.charge;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ShipmentAccountInfoDTO {

    String accountType;
    String accountNumber;
    String accountName;
    String businessType;
}
