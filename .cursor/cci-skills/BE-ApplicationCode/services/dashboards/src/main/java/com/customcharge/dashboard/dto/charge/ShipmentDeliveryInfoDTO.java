package com.customcharge.dashboard.dto.charge;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;

@Value
@Builder
@Jacksonized
public class ShipmentDeliveryInfoDTO {

    LocalDate createdDate;
    LocalDate pickupDate;
    String deliveryType;
    String paymentType;
}
