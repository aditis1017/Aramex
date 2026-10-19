package com.customcharge.shipment.service.cif;

import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.shipment.config.ShipmentProperties;
import com.customcharge.shipment.service.charge.ChargeCategorySupport;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static com.customcharge.shipment.constants.LogMessages.SH267_CONVERTED_CIF;
import static com.customcharge.shipment.constants.LogMessages.SH267_SKIPPED_MISSING_HAWB;
import static com.customcharge.shipment.constants.LogMessages.SH267_SKIPPED_SHIPMENT_ALREADY_EXEMPTED;
import static com.customcharge.shipment.constants.LogMessages.SH267_SKIPPED_SHIPMENT_NOT_FOUND;
import static com.customcharge.shipment.constants.LogMessages.SH267_SKIPPED_UNPARSEABLE_COMMENT1;
import static com.customcharge.shipment.constants.LogMessages.SH267_STORED_WITHOUT_RECALCULATION;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentCifUpdateService {

    ShipmentRepository shipmentRepository;
    ShipmentCifChargeRecalculationService cifChargeRecalculationService;
    ObjectProvider<ExchangeRateService> exchangeRateService;
    ShipmentProperties shipmentProperties;

    @Transactional
    public void handle(ShipmentDto message) {
        Optional<HawbComment2CifParser.ParsedCif> parsedCif = HawbComment2CifParser.parse(message.getComment1());
        if (parsedCif.isEmpty()) {
            log.warn(SH267_SKIPPED_UNPARSEABLE_COMMENT1, message.getHawb());
            return;
        }

        String hawb = normalizeHawb(message.getHawb());
        if (hawb == null) {
            log.warn(SH267_SKIPPED_MISSING_HAWB);
            return;
        }

        Shipment shipment = shipmentRepository.findByHawbNumber(hawb).orElse(null);
        if (shipment == null) {
            log.warn(SH267_SKIPPED_SHIPMENT_NOT_FOUND, hawb);
            return;
        }

        if (ChargeCategorySupport.isExempted(shipment.getCategoryType())) {
            log.warn(SH267_SKIPPED_SHIPMENT_ALREADY_EXEMPTED, hawb);
            return;
        }

        HawbComment2CifParser.ParsedCif originalCif = parsedCif.get();
        StoredCif calculationCif = resolveCalculationCif(originalCif, shipment);
        shipment.setCifValue(calculationCif.amount());
        shipment.setCifValueCurrency(calculationCif.currency());

        if (calculationCif.amount() >= 0) {
            cifChargeRecalculationService.recalculate(shipment, originalCif);
        } else {
            log.warn(
                    SH267_STORED_WITHOUT_RECALCULATION,
                    hawb,
                    originalCif.amount(),
                    originalCif.currency());
        }

        shipment.setCifValue(originalCif.amount());
        shipment.setCifValueCurrency(originalCif.currency());
        shipmentRepository.save(shipment);
    }

    private StoredCif resolveCalculationCif(HawbComment2CifParser.ParsedCif cif, Shipment shipment) {
        String cifCurrency = normalizeCurrency(cif.currency());
        String costOfGoodsCurrency = normalizeCurrency(shipment.getCustomsCurrency());
        if (!StringUtils.hasText(cifCurrency)
                || !StringUtils.hasText(costOfGoodsCurrency)
                || cifCurrency.equalsIgnoreCase(costOfGoodsCurrency)) {
            return new StoredCif(cif.amount(), cif.currency());
        }

        double convertedAmount = convertAmount(cif.amount(), cifCurrency, costOfGoodsCurrency);
        log.debug(
                SH267_CONVERTED_CIF,
                cif.amount(),
                cifCurrency,
                convertedAmount,
                costOfGoodsCurrency,
                shipment.getHawbNumber());
        return new StoredCif(convertedAmount, costOfGoodsCurrency);
    }

    private double convertAmount(double amount, String fromCurrency, String toCurrency) {
        ExchangeRateService exchangeRate = exchangeRateService.getIfAvailable();
        if (exchangeRate == null) {
            return amount;
        }
        CurrencyConversionResult result = exchangeRate.convertAmount(amount, fromCurrency, toCurrency);
        if (result == null || result.convertedAmount() == null) {
            return amount;
        }
        return BigDecimal.valueOf(result.convertedAmount())
                .setScale(shipmentProperties.getMonetaryScale(), RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static String normalizeCurrency(String currency) {
        return currency == null ? "" : currency.strip();
    }

    private static String normalizeHawb(String hawb) {
        if (!StringUtils.hasText(hawb)) {
            return null;
        }
        return hawb.trim();
    }

    private record StoredCif(double amount, String currency) {}
}
