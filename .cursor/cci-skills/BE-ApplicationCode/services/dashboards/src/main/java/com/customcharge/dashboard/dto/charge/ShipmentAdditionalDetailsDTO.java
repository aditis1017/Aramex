package com.customcharge.dashboard.dto.charge;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ShipmentAdditionalDetailsDTO {

    ShipmentGoodsSummaryDTO goodsSummary;
    ShipmentLocationDetailsDTO origin;
    ShipmentLocationDetailsDTO destination;
    ShipmentLocationDetailsDTO thirdParty;
    ShipmentSellerInfoDTO sellerInformation;
}
