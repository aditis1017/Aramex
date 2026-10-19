package com.customcharge.shipment.service.charge.processing.calculation;

import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.shipment.config.ShipmentProperties;
import com.customcharge.shipment.service.charge.ShipmentCurrencyContext;
import com.customcharge.shipment.service.charge.ShipmentCurrencyContextFactory;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Derives monetary bases used when a {@link com.customcharge.persistencelayer.entities.RateSheetItem}
 * is configured as a percentage rate.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentCostBasisCalculator {

    private static final double UNIT_EXCHANGE_RATE = 1.0;

    ShipmentProperties shipmentProperties;
    ShipmentCurrencyContextFactory currencyContextFactory;
    ObjectProvider<ExchangeRateService> exchangeRateService;

    public record ResolvedBasis(BigDecimal amount, Double exchangeRate) {

        static ResolvedBasis zero() {
            return new ResolvedBasis(BigDecimal.ZERO, null);
        }
    }

    public BigDecimal resolveBasis(Shipment shipment, PercentageOf percentageOf, String chargeCode) {
        return resolveBasisWithRate(shipment, percentageOf, chargeCode).amount();
    }

    public ResolvedBasis resolveBasisWithRate(Shipment shipment, PercentageOf percentageOf, String chargeCode) {
        if (shipment == null || percentageOf == null) {
            return ResolvedBasis.zero();
        }
        return switch (percentageOf) {
            case COST_OF_GOODS -> costOfGoodsInRateSheetCurrency(shipment, chargeCode);
            case CIF_VALUE -> cifBasisInRateSheetCurrency(shipment, chargeCode);
        };
    }

    private ResolvedBasis cifBasisInRateSheetCurrency(Shipment shipment, String chargeCode) {
        ResolvedBasis costOfGoods = costOfGoodsInRateSheetCurrency(shipment, chargeCode);
        BigDecimal cifContribution = cifContributionInRateSheetCurrency(shipment, chargeCode);
        if (cifContribution.compareTo(BigDecimal.ZERO) <= 0) {
            return costOfGoods;
        }
        BigDecimal total = costOfGoods.amount()
                .add(cifContribution)
                .setScale(shipmentProperties.getMonetaryScale(), RoundingMode.HALF_UP);
        return new ResolvedBasis(total, costOfGoods.exchangeRate());
    }

    private BigDecimal cifContributionInRateSheetCurrency(Shipment shipment, String chargeCode) {
        BigDecimal cifInCustomsCurrency = cifValueInCustomsCurrency(shipment);
        if (cifInCustomsCurrency.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return convertFromCustomsCurrency(cifInCustomsCurrency, shipment, chargeCode).amount();
    }

    private BigDecimal cifValueInCustomsCurrency(Shipment shipment) {
        BigDecimal cifValue = toBigDecimal(shipment.getCifValue());
        if (cifValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        ShipmentCurrencyContext currencyContext = currencyContextFactory.forShipment(shipment);
        String cifCurrency = normalizeCurrency(shipment.getCifValueCurrency());
        String customsCurrency = normalizeCurrency(currencyContext.customsCurrency());
        if (!StringUtils.hasText(cifCurrency)
                || !StringUtils.hasText(customsCurrency)
                || cifCurrency.equalsIgnoreCase(customsCurrency)) {
            return cifValue;
        }
        return convertAmount(cifValue, cifCurrency, customsCurrency).amount();
    }

    private ResolvedBasis costOfGoodsInRateSheetCurrency(Shipment shipment, String chargeCode) {
        BigDecimal costOfGoods = toBigDecimal(shipment.getCostOfGoods());
        return convertFromCustomsCurrency(costOfGoods, shipment, chargeCode);
    }

    private ResolvedBasis convertFromCustomsCurrency(
            BigDecimal amount, Shipment shipment, String chargeCode) {
        ShipmentCurrencyContext currencyContext = currencyContextFactory.forShipment(shipment);
        String customsCurrency = normalizeCurrency(currencyContext.customsCurrency());
        String targetCurrency = normalizeCurrency(currencyContext.rateSheetBasisCurrency(chargeCode));
        if (!StringUtils.hasText(customsCurrency)
                || !StringUtils.hasText(targetCurrency)
                || customsCurrency.equalsIgnoreCase(targetCurrency)) {
            return new ResolvedBasis(amount, UNIT_EXCHANGE_RATE);
        }
        return convertAmount(amount, customsCurrency, targetCurrency);
    }

    private ResolvedBasis convertAmount(BigDecimal amount, String fromCurrency, String toCurrency) {
        ExchangeRateService exchangeRate = exchangeRateService.getIfAvailable();
        if (exchangeRate == null) {
            return new ResolvedBasis(amount, null);
        }
        CurrencyConversionResult result =
                exchangeRate.convertAmount(amount.doubleValue(), fromCurrency, toCurrency);
        if (result == null || result.convertedAmount() == null) {
            return new ResolvedBasis(amount, null);
        }
        return new ResolvedBasis(
                BigDecimal.valueOf(result.convertedAmount())
                        .setScale(shipmentProperties.getMonetaryScale(), RoundingMode.HALF_UP),
                result.exchangeRate());
    }

    private static String normalizeCurrency(String currency) {
        return currency == null ? "" : currency.strip();
    }

    private BigDecimal toBigDecimal(Double value) {
        return Optional.ofNullable(value)
                .map(v -> BigDecimal.valueOf(v)
                        .setScale(shipmentProperties.getMonetaryScale(), RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
    }
}
