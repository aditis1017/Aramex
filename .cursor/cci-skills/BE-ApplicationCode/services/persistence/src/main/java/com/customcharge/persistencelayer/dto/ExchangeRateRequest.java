package com.customcharge.persistencelayer.dto;

public record ExchangeRateRequest(
        double amount,
        String currentCurrencyCode,
        String newCurrencyCode) {
}
