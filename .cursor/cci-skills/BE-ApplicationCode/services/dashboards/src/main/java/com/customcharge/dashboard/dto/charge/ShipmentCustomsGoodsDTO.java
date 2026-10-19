package com.customcharge.dashboard.dto.charge;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Value
@Builder
@Jacksonized
public class ShipmentCustomsGoodsDTO {

    String customsType;
    String source;
    String shipmentBusinessType;
    String accountBusinessType;
    BigDecimal goodsValue;
    String goodsValueCurrency;
    BigDecimal cifValue;
    String cifValueCurrency;
    BigDecimal shippingCharges;
    String shipmentChargeCurrency;
    Integer numberOfPieces;
    String productGroup;
    BigDecimal insuranceValue;
}
