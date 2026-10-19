package com.customcharge.shipment.service.cif;

import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
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

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ChargeCifValueSupport {

    ObjectProvider<ExchangeRateService> exchangeRateService;
    ShipmentCurrencyContextFactory currencyContextFactory;
    ShipmentProperties shipmentProperties;

    /**
     * SH013: store cost of goods converted into the charge CIF currency on each charge.
     */
    public void applySh013CogSnapshot(Shipment shipment, Charge charge) {
        if (shipment == null || charge == null) {
            return;
        }

        Double costOfGoods = shipment.getCostOfGoods();
        String sourceCurrency = normalizeCurrency(shipment.getCustomsCurrency());
        String cifCurrency = resolveCifCurrency(shipment, charge);

        if (!isPositiveAmount(costOfGoods) || !StringUtils.hasText(sourceCurrency) || !StringUtils.hasText(cifCurrency)) {
            charge.setCifValue(0.0);
            charge.setCifCurrency(cifCurrency);
            charge.setCifCurrencyRate(null);
            return;
        }

        Conversion conversion = convertAmount(costOfGoods, sourceCurrency, cifCurrency);
        charge.setCifValue(conversion.amount());
        charge.setCifCurrency(cifCurrency);
        charge.setCifCurrencyRate(conversion.rate());
    }

    /**
     * SH267: stores shipping charges converted into the charge CIF currency without updating {@code cifValue}.
     */
    public void applySh267ShippingChargesSnapshot(
            Shipment shipment,
            Charge charge,
            double originalCifAmount,
            String originalCifCurrency) {
        if (shipment == null || charge == null) {
            return;
        }
        String cifCurrency = resolveCifCurrency(shipment, charge);
        if (!StringUtils.hasText(cifCurrency)) {
            return;
        }
        charge.setCifCurrency(cifCurrency);
        applySh267ShippingCharges(charge, originalCifAmount, originalCifCurrency, cifCurrency);
    }

    /**
     * SH267: sum shipment cost of goods and CIF in customs currency, then convert into the charge CIF currency.
     * Also stores the original SH267 CIF amount converted directly into the charge CIF currency as shipping charges.
     */
    public void addSh267CifContribution(
            Shipment shipment,
            Charge charge,
            double originalCifAmount,
            String originalCifCurrency) {
        if (shipment == null || charge == null) {
            return;
        }

        String cifCurrency = resolveCifCurrency(shipment, charge);
        if (!StringUtils.hasText(cifCurrency)) {
            return;
        }

        ShipmentCurrencyContext currencyContext = currencyContextFactory.forShipment(shipment);
        String customsCurrency = normalizeCurrency(currencyContext.customsCurrency());

        charge.setCifCurrency(cifCurrency);
        applySh267ShippingCharges(charge, originalCifAmount, originalCifCurrency, cifCurrency);

        if (!StringUtils.hasText(customsCurrency)) {
            charge.setCifValue(0.0);
            charge.setCifCurrencyRate(null);
            return;
        }

        double costOfGoods = shipment.getCostOfGoods() != null ? shipment.getCostOfGoods() : 0.0;
        double cifInCustoms = cifAmountInCustomsCurrency(shipment, customsCurrency);
        double totalInCustoms = round(costOfGoods + cifInCustoms);

        if (totalInCustoms <= 0) {
            charge.setCifValue(0.0);
            charge.setCifCurrencyRate(null);
            return;
        }

        Conversion conversion = convertAmount(totalInCustoms, customsCurrency, cifCurrency);
        charge.setCifValue(conversion.amount());
        charge.setCifCurrencyRate(conversion.rate());
    }

    private void applySh267ShippingCharges(
            Charge charge,
            double originalCifAmount,
            String originalCifCurrency,
            String cifCurrency) {
        String sourceCurrency = normalizeCurrency(originalCifCurrency);
        if (!isPositiveAmount(originalCifAmount) || !StringUtils.hasText(sourceCurrency)) {
            charge.setShippingCharges(0.0);
            return;
        }

        Conversion conversion = convertAmount(originalCifAmount, sourceCurrency, cifCurrency);
        charge.setShippingCharges(conversion.amount());
    }

    private double cifAmountInCustomsCurrency(Shipment shipment, String customsCurrency) {
        if (!isPositiveAmount(shipment.getCifValue())) {
            return 0.0;
        }

        String cifCurrency = normalizeCurrency(shipment.getCifValueCurrency());
        if (!StringUtils.hasText(cifCurrency) || cifCurrency.equalsIgnoreCase(customsCurrency)) {
            return shipment.getCifValue();
        }

        return convertAmount(shipment.getCifValue(), cifCurrency, customsCurrency).amount();
    }

    private String resolveCifCurrency(Shipment shipment, Charge charge) {
        ShipmentCurrencyContext currencyContext = currencyContextFactory.forShipment(shipment);
        return normalizeCurrency(currencyContext.cifTargetCurrency(charge.getChargeCode()));
    }

    private Conversion convertAmount(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return new Conversion(round(amount), 1.0);
        }

        ExchangeRateService exchangeRate = exchangeRateService.getIfAvailable();
        if (exchangeRate == null) {
            return new Conversion(0.0, null);
        }

        CurrencyConversionResult result = exchangeRate.convertAmount(amount, fromCurrency, toCurrency);
        if (result == null || result.convertedAmount() == null) {
            return new Conversion(0.0, null);
        }

        return new Conversion(round(result.convertedAmount()), result.exchangeRate());
    }

    private static boolean isPositiveAmount(Double amount) {
        return amount != null && amount > 0;
    }

    private double round(double amount) {
        return BigDecimal.valueOf(amount)
                .setScale(shipmentProperties.getMonetaryScale(), RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static String normalizeCurrency(String currency) {
        return currency == null ? "" : currency.strip();
    }

    private record Conversion(double amount, Double rate) {}
}
