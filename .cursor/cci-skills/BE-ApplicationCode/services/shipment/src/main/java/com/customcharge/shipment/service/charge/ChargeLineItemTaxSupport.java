package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.customcharge.persistencelayer.util.ServiceChargeTaxResolutionSupport;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Resolves tax for shipment charge line items and applies {@code amount} (pre-tax) and
 * {@code preTaxableAmount} (taxed).
 * Tax account: entity-settings for cost charges (ECD/ECS/EC3); AR billing account only for revenue.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ChargeLineItemTaxSupport {

    private static final int TAX_SCALE = 4;

    ServiceChargeAccountResolver serviceChargeAccountResolver;
    ServiceChargeRepository serviceChargeRepository;
    ServiceCodesRepository serviceCodesRepository;
    AccountRedisChargeDataService accountRedisChargeDataService;
    EntityRepository entityRepository;
    ShipmentChargeAmountSupport chargeAmountSupport;

    public record TaxedLineAmount(BigDecimal preTaxableAmount, BigDecimal amount, Double exchangeRate) {
    }

    public TaxedLineAmount computeFromRawRate(
            BigDecimal rawRateAmount,
            Shipment shipment,
            Charge charge,
            Account rateSheetAccount,
            String entityCode,
            String serviceCode,
            ServiceCodes master) {
        return computeFromRawRate(
                rawRateAmount, shipment, charge, rateSheetAccount, entityCode, serviceCode, master, false, null);
    }

    public TaxedLineAmount computeFromRawRate(
            BigDecimal rawRateAmount,
            Shipment shipment,
            Charge charge,
            Account rateSheetAccount,
            String entityCode,
            String serviceCode,
            ServiceCodes master,
            Double basisExchangeRate) {
        return computeFromRawRate(
                rawRateAmount, shipment, charge, rateSheetAccount, entityCode, serviceCode, master, false,
                basisExchangeRate);
    }

    public TaxedLineAmount computeFromRawRate(
            BigDecimal rawRateAmount,
            Shipment shipment,
            Charge charge,
            Account rateSheetAccount,
            String entityCode,
            String serviceCode,
            ServiceCodes master,
            boolean forceZero) {
        return computeFromRawRate(
                rawRateAmount, shipment, charge, rateSheetAccount, entityCode, serviceCode, master, forceZero,
                null);
    }

    public TaxedLineAmount computeFromRawRate(
            BigDecimal rawRateAmount,
            Shipment shipment,
            Charge charge,
            Account rateSheetAccount,
            String entityCode,
            String serviceCode,
            ServiceCodes master,
            boolean forceZero,
            Double basisExchangeRate) {
        if (forceZero) {
            return new TaxedLineAmount(BigDecimal.ZERO, BigDecimal.ZERO, null);
        }

        BigDecimal safeRaw = rawRateAmount != null ? rawRateAmount : BigDecimal.ZERO;
        ConvertedChargeAmount preTaxConverted =
                chargeAmountSupport.convertAndFormatWithRate(safeRaw, shipment, charge);
        BigDecimal preTax = preTaxConverted.amount();

        Double taxPercent = resolveTaxPercent(
                shipment, charge, rateSheetAccount, entityCode, serviceCode, master);
        BigDecimal withTax = applyTaxPercent(preTax, taxPercent);
        BigDecimal amount = chargeAmountSupport.formatAmount(withTax, shipment, charge);

        return new TaxedLineAmount(preTax, amount, resolveStoredExchangeRate(charge, preTaxConverted.exchangeRate(),
                basisExchangeRate));
    }

    private static Double resolveStoredExchangeRate(
            Charge charge, Double postCalculationExchangeRate, Double basisExchangeRate) {
        String chargeCode = ShipmentChargeTypeSupport.normalize(charge.getChargeCode());
        if (ShipmentChargeTypeSupport.usesDestinationCountryCurrency(chargeCode) && basisExchangeRate != null) {
            return basisExchangeRate;
        }
        return postCalculationExchangeRate;
    }

    public void applyToLineItem(ChargeLineItem lineItem, TaxedLineAmount taxed) {
        lineItem.setAmount(taxed.preTaxableAmount());
        lineItem.setPreTaxableAmount(taxed.amount());
        lineItem.setExchangeRate(taxed.exchangeRate());
    }

    public BigDecimal formatWithTax(
            BigDecimal preTax, Double taxPercent, Shipment shipment, Charge charge) {
        BigDecimal taxed = applyTaxPercent(preTax, taxPercent);
        return chargeAmountSupport.formatAmount(taxed, shipment, charge);
    }

    /**
     * Contracted rate is pre-tax: only currency conversion/formatting is applied.
     */
    public BigDecimal computeContractedRate(
            BigDecimal rawContracted,
            Shipment shipment,
            Charge charge) {
        BigDecimal safeRaw = rawContracted != null ? rawContracted : BigDecimal.ZERO;
        return chargeAmountSupport.convertAndFormatWithRate(safeRaw, shipment, charge).amount();
    }

    Double resolveTaxPercent(
            Shipment shipment,
            Charge charge,
            Account rateSheetAccount,
            String entityCode,
            String serviceCode,
            ServiceCodes master) {
        String chargeCode = ShipmentChargeTypeSupport.normalize(charge.getChargeCode());
        ServiceCodes resolvedMaster = master != null
                ? master
                : loadMaster(entityCode, chargeCode, serviceCode).orElse(null);

        Optional<Account> taxAccount = resolveTaxAccount(shipment, entityCode, chargeCode, rateSheetAccount);
        ServiceCharge serviceCharge = taxAccount
                .flatMap(account -> findServiceCharge(account, resolvedMaster))
                .orElse(null);

        Double entityTaxValue = resolveEntityTaxValue(entityCode);

        return ServiceChargeTaxResolutionSupport.resolve(serviceCharge, resolvedMaster, entityTaxValue)
                .taxValue();
    }

    private Optional<Account> resolveTaxAccount(
            Shipment shipment, String entityCode, String chargeCode, Account rateSheetAccount) {
        if (ShipmentChargeTypeSupport.usesEntitySettingsAccountOnly(chargeCode)) {
            return Optional.ofNullable(rateSheetAccount);
        }
        if (ShipmentChargeTypeSupport.isRevenueCharge(chargeCode)) {
            return serviceChargeAccountResolver.resolveArBillingAccount(shipment, entityCode, chargeCode);
        }
        return Optional.ofNullable(rateSheetAccount);
    }

    private Optional<ServiceCharge> findServiceCharge(Account account, ServiceCodes master) {
        if (account == null || master == null || master.getId() == null) {
            return Optional.empty();
        }
        ShipmentProcessingContext context = ShipmentProcessingContext.get();
        if (context != null) {
            return context.findServiceCharge(account, master);
        }
        return accountRedisChargeDataService.findServiceCharge(
                account, account.getEntityCode(), account.getChargeType(), master);
    }

    private Double resolveEntityTaxValue(String entityCode) {
        ShipmentProcessingContext context = ShipmentProcessingContext.get();
        if (context != null) {
            return context.findEntity(entityCode).map(Entity::getTaxValue).orElse(null);
        }
        return entityRepository.findByCodeIgnoreCase(entityCode).map(Entity::getTaxValue).orElse(null);
    }

    private Optional<ServiceCodes> loadMaster(String entityCode, String chargeCode, String serviceCode) {
        if (!StringUtils.hasText(entityCode) || !StringUtils.hasText(serviceCode)) {
            return Optional.empty();
        }
        ShipmentProcessingContext context = ShipmentProcessingContext.get();
        if (context != null) {
            return context.findServiceCode(entityCode, chargeCode, serviceCode);
        }
        return accountRedisChargeDataService.findServiceCodeMaster(
                entityCode.strip(), ShipmentChargeTypeSupport.normalize(chargeCode), serviceCode.strip());
    }

    private static BigDecimal applyTaxPercent(BigDecimal preTax, Double taxPercent) {
        BigDecimal base = preTax != null ? preTax : BigDecimal.ZERO;
        if (taxPercent == null || taxPercent <= 0) {
            return base;
        }
        BigDecimal tax = base.multiply(BigDecimal.valueOf(taxPercent))
                .divide(BigDecimal.valueOf(100), TAX_SCALE, RoundingMode.HALF_UP);
        return base.add(tax);
    }
}
