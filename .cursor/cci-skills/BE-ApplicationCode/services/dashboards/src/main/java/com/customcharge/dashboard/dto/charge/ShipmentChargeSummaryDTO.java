package com.customcharge.dashboard.dto.charge;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ShipmentChargeSummaryDTO {

    String trackingNumber;
    String billingEntity;
    String billingCountry;
    String destinationCostStatus;
    Long mawbConsole;
    ShipmentStatusBadgesDTO statusBadges;
    ShipmentCustomsGoodsDTO customsAndGoods;
    ShipmentDeliveryInfoDTO deliveryInformation;
    ShipmentAdditionalDetailsDTO additionalDetails;
}
