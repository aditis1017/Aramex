package com.customcharge.dashboard.dto.charge;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ShipmentSellerInfoDTO {

    String country;
    String entity;
    String accountNumber;
    String accountName;
    String collectionReference;
}
