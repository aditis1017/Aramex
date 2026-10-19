package com.customcharge.persistencelayer.dto;

public record ExchangeRateResponse(
        Double amount,
        String currentCurrencyCode,
        String newCurrencyCode,
        Double convertedAmount) {
}
