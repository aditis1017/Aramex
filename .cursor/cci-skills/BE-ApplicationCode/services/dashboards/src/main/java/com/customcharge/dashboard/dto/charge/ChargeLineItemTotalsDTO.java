package com.customcharge.dashboard.dto.charge;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Value
@Builder
@Jacksonized
public class ChargeLineItemTotalsDTO {

    BigDecimal estimateAmount;
    BigDecimal actualOne;
    BigDecimal actualTwo;
    BigDecimal actualThree;
    BigDecimal actualFour;
    BigDecimal totalActuals;
    BigDecimal eligibleCost;
    BigDecimal invoiceOne;
    BigDecimal invoiceTwo;
    BigDecimal invoiceThree;
    BigDecimal invoiceFour;
    BigDecimal outstanding;
    BigDecimal totalInvoices;
    BigDecimal contractRateTotal;
}
