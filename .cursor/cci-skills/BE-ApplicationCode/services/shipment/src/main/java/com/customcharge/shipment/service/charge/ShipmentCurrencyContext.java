package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves shipment country currencies once per charge-processing run.
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class ShipmentCurrencyContext {

    static final String USD = "USD";

    final Shipment shipment;
    final CurrencyRepository currencyRepository;
    final Map<String, Optional<String>> currencyByCountry = new HashMap<>();

    public ShipmentCurrencyContext(Shipment shipment, CurrencyRepository currencyRepository) {
        this.shipment = shipment;
        this.currencyRepository = currencyRepository;
    }

    public String destinationCurrency() {
        return currencyForCountry(shipment != null ? shipment.getConsigneeCountryCode() : null).orElse(null);
    }

    public String shipperCurrency() {
        return currencyForCountry(shipment != null ? shipment.getShipperCountryCode() : null).orElse(null);
    }

    public String thirdPartyCurrency() {
        return currencyForCountry(shipment != null ? shipment.getThirdPartyCountry() : null).orElse(null);
    }

    public String customsCurrency() {
        if (shipment == null || !StringUtils.hasText(shipment.getCustomsCurrency())) {
            return null;
        }
        return ExchangeRateService.normalizeCurrency(shipment.getCustomsCurrency());
    }

    /**
     * Currency that rate-sheet percentage bases (e.g. cost of goods) should be expressed in.
     */
    public String rateSheetBasisCurrency(String chargeCode) {
        String normalized = ShipmentChargeTypeSupport.normalize(chargeCode);
        if (ShipmentChargeTypeSupport.usesDestinationCountryCurrency(normalized)) {
            return destinationCurrency();
        }
        if (ShipmentChargeTypeSupport.EC3.equals(normalized)) {
            return USD;
        }
        if (ShipmentChargeTypeSupport.usesThirdPartyEntity(normalized)) {
            return thirdPartyCurrency();
        }
        if (ShipmentChargeTypeSupport.usesOriginEntity(normalized)) {
            return shipperCurrency();
        }
        return destinationCurrency();
    }

    /**
     * Currency rate-sheet amounts are assumed to be in before applying charge-level FX.
     */
    public String rateSheetFromCurrency(String chargeCode) {
        return rateSheetBasisCurrency(chargeCode);
    }

    public String chargeTargetCurrency(String chargeCode) {
        String normalized = ShipmentChargeTypeSupport.normalize(chargeCode);
        if (ShipmentChargeTypeSupport.usesDestinationCountryCurrency(normalized)) {
            return destinationCurrency();
        }
        if (ShipmentChargeTypeSupport.usesUsdCurrency(normalized)) {
            return USD;
        }
        if (ShipmentChargeTypeSupport.usesShipperCountryCurrency(normalized)) {
            return shipperCurrency();
        }
        if (ShipmentChargeTypeSupport.usesThirdPartyCountryCurrency(normalized)) {
            return thirdPartyCurrency();
        }
        return destinationCurrency();
    }

    /**
     * Currency used when storing CIF snapshots on a charge (SH013 / SH267).
     */
    public String cifTargetCurrency(String chargeCode) {
        String normalized = ShipmentChargeTypeSupport.normalize(chargeCode);
        if (ShipmentChargeTypeSupport.usesCifDestinationCountryCurrency(normalized)) {
            return destinationCurrency();
        }
        if (ShipmentChargeTypeSupport.EC3.equals(normalized)) {
            return USD;
        }
        if (ShipmentChargeTypeSupport.usesCifShipperCountryCurrency(normalized)) {
            return shipperCurrency();
        }
        if (ShipmentChargeTypeSupport.usesCifThirdPartyCountryCurrency(normalized)) {
            return thirdPartyCurrency();
        }
        return destinationCurrency();
    }

    private Optional<String> currencyForCountry(String countryCode) {
        if (!StringUtils.hasText(countryCode)) {
            return Optional.empty();
        }
        String key = countryCode.strip().toUpperCase();
        return currencyByCountry.computeIfAbsent(
                key,
                ignored -> currencyRepository
                        .findByCountry_CodeIgnoreCaseAndActiveTrue(key)
                        .map(currency -> ExchangeRateService.normalizeCurrency(currency.getCurrencyCode())));
    }
}
