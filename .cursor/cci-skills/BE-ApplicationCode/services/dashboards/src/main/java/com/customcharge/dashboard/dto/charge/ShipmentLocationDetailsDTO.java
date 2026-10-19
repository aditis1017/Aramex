package com.customcharge.dashboard.dto.charge;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ShipmentLocationDetailsDTO {

    String country;
    String state;
    String city;
    String entity;
    String partyName;
    String accountNumber;
}
