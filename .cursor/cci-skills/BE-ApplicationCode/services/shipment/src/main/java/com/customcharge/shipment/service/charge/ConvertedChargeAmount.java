package com.customcharge.shipment.service.charge;

import java.math.BigDecimal;

public record ConvertedChargeAmount(BigDecimal amount, Double exchangeRate) {
}
