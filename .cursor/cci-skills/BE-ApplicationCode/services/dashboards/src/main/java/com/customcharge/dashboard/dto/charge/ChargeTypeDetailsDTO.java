package com.customcharge.dashboard.dto.charge;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
@Jacksonized
public class ChargeTypeDetailsDTO {

    String chargeType;
    boolean isEstimated;
    List<String> availableChargeTypes;
    ShipmentChargeSummaryDTO shipment;
    String chargeGroup;
    String billingEntity;
    String billingCountry;
    String destinationCostStatus;
    String status;
    BigDecimal estimateAmount;
    BigDecimal actualAmount;
    BigDecimal outstandingAmount;
    String currency;
    List<String> boeList;
    List<String> paymentReferenceList;
    ShipmentAccountInfoDTO accountInformation;
    List<ChargeLineItemRowDTO> lineItems;
    ChargeLineItemTotalsDTO totals;
}
