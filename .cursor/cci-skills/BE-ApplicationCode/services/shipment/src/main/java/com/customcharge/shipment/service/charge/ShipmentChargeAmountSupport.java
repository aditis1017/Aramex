package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.calculatorhelper.ChargeHeaderAmountSupport;
import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.persistencelayer.util.FixedDecimalUtil;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

import static com.customcharge.shipment.constants.LogMessages.EXCHANGE_RATE_SERVICE_UNAVAILABLE;
import static com.customcharge.shipment.constants.LogMessages.FX_CONVERSION_SKIPPED;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentChargeAmountSupport {

    private static final double UNIT_EXCHANGE_RATE = 1.0;

    FixedDecimalUtil fixedDecimalUtil;
    ObjectProvider<ExchangeRateService> exchangeRateService;
    ShipmentCurrencyContextFactory currencyContextFactory;

    /**
     * Converts rate-sheet amount to charge target currency, then applies entity decimal places.
     */
    public BigDecimal convertAndFormat(BigDecimal amount, Shipment shipment, Charge charge) {
        return convertAndFormatWithRate(amount, shipment, charge).amount();
    }

    /**
     * Converts rate-sheet amount to charge target currency, applies entity decimal places,
     * and returns the exchange rate used for the conversion when FX was applied.
     */
    public ConvertedChargeAmount convertAndFormatWithRate(
            BigDecimal amount, Shipment shipment, Charge charge) {
        AmountConversion conversion = convertAmount(amount, shipment, charge);
        BigDecimal formatted = formatAmount(conversion.amount(), shipment, charge);
        return new ConvertedChargeAmount(formatted, conversion.exchangeRate());
    }

    public BigDecimal formatAmount(BigDecimal amount, Shipment shipment, Charge charge) {
        if (charge == null) {
            return formatAmount(amount, "");
        }
        return formatAmount(amount, shipment, charge.getChargeCode());
    }

    /**
     * Decimal places are resolved from entity-specific config ({@code DECIMAL_{entityCode}}).
     * ECD, DTC, and DTA use destination entity; ECS and DTS use origin; EC3 and DT3 use third-party entity.
     */
    public BigDecimal formatAmount(BigDecimal amount, Shipment shipment, String chargeCode) {
        return formatAmount(amount, decimalEntityCode(shipment, chargeCode));
    }

    public BigDecimal formatAmount(BigDecimal amount, String entityCode) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        Double formatted = fixedDecimalUtil.convertToFixedDecimal(amount.doubleValue(), entityCode);
        return formatted == null ? BigDecimal.ZERO : BigDecimal.valueOf(formatted);
    }

    public void applyChargeHeaderAmounts(Shipment shipment, Charge charge, List<ChargeLineItem> lineItems) {
        ChargeHeaderAmountSupport.applyToCharge(
                charge, lineItems, amount -> formatAmount(amount, shipment, charge));
    }

    AmountConversion convertAmount(BigDecimal amount, Shipment shipment, Charge charge) {
        if (amount == null) {
            return AmountConversion.unchanged(BigDecimal.ZERO);
        }
        if (charge == null || !StringUtils.hasText(charge.getChargeCode())) {
            return AmountConversion.unchanged(amount);
        }

        String chargeCode = charge.getChargeCode();

        ShipmentCurrencyContext currencyContext = currencyContextFactory.forShipment(shipment);
        String fromCurrency = currencyContext.rateSheetFromCurrency(chargeCode);
        String toCurrency = currencyContext.chargeTargetCurrency(chargeCode);
        if (!StringUtils.hasText(fromCurrency) || !StringUtils.hasText(toCurrency)) {
            log.warn(
                    FX_CONVERSION_SKIPPED,
                    chargeCode,
                    shipment != null ? shipment.getHawbNumber() : null,
                    fromCurrency,
                    toCurrency);
            return AmountConversion.unchanged(amount);
        }

        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return new AmountConversion(amount, UNIT_EXCHANGE_RATE);
        }

        ExchangeRateService exchangeRate = exchangeRateService.getIfAvailable();
        if (exchangeRate == null) {
            log.debug(
                    EXCHANGE_RATE_SERVICE_UNAVAILABLE,
                    fromCurrency,
                    toCurrency);
            return AmountConversion.unchanged(amount);
        }

        CurrencyConversionResult result =
                exchangeRate.convertAmount(amount.doubleValue(), fromCurrency, toCurrency);
        if (result == null || result.convertedAmount() == null) {
            return AmountConversion.unchanged(amount);
        }
        return new AmountConversion(
                BigDecimal.valueOf(result.convertedAmount()), result.exchangeRate());
    }

    record AmountConversion(BigDecimal amount, Double exchangeRate) {
        static AmountConversion unchanged(BigDecimal amount) {
            return new AmountConversion(amount, null);
        }
    }

    static String decimalEntityCode(Shipment shipment, String chargeCode) {
        return ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, chargeCode);
    }
}
