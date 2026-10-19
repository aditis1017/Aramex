package com.customcharge.chargescalculator.service.charge;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.constants.ChargeCreationConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.service.batch.RecordProcessingLookups;
import com.customcharge.chargescalculator.service.calculation.ShipmentCostBasisCalculator;
import com.customcharge.chargescalculator.service.calculation.TaxValueAmountCalculator;
import com.customcharge.chargescalculator.service.ratesheet.PercentageRateSpec;
import com.customcharge.chargescalculator.service.ratesheet.RateSheetAmountBreakdown;
import com.customcharge.persistencelayer.calculatorhelper.ChargeHeaderAmountSupport;
import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.persistencelayer.dto.ServiceChargeQueryCriteria;
import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.entities.Country;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.CountryRepository;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.persistencelayer.util.AccountLoaders;
import com.customcharge.persistencelayer.util.DomEntityConfigResolver;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.persistencelayer.util.FixedDecimalUtil;
import com.customcharge.persistencelayer.util.RevenueBillingAccountResolver;
import com.customcharge.persistencelayer.util.ServiceChargeResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class ChargeLineSupport {

    private static final String USD = "USD";
    private static final String CURRENCY_CACHE_PREFIX = "Currency:";
    private static final int AMOUNT_SCALE = 4;

    private static final Set<String> ENTITY_SETTINGS_COST_CHARGE_CODES = Set.of(
            BatchProcessingConstants.CHARGE_CODE_ECS,
            "ECD",
            BatchProcessingConstants.CHARGE_CODE_EC3);
    private static final Set<String> DOM_DESTINATION_CHARGE_CODES = Set.of(
            BatchProcessingConstants.CHARGE_CODE_ECD,
            BatchProcessingConstants.CHARGE_CODE_DTC);
    private static final Set<String> USD_CHARGE_CODES = Set.of(
            BatchProcessingConstants.CHARGE_CODE_DTA,
            BatchProcessingConstants.CHARGE_CODE_ECS,
            BatchProcessingConstants.CHARGE_CODE_EC3);

    private final AccountRepository accountRepository;
    private final ARAccountRepository arAccountRepository;
    private final ServiceChargeResolver serviceChargeResolver;
    private final FixedDecimalUtil fixedDecimalUtil;
    private final ExchangeRateService exchangeRateService;
    private final CurrencyRepository currencyRepository;
    private final CountryRepository countryRepository;
    private final ObjectProvider<CacheService> cacheServiceProvider;
    private final ShipmentCostBasisCalculator shipmentCostBasisCalculator;
    private final RevenueBillingAccountResolver revenueBillingAccountResolver;
    private final DomEntityConfigResolver domEntityConfigResolver;
    private final RecordProcessingLookups recordProcessingLookups;

    public ChargeLineSupport(
            AccountRepository accountRepository,
            ARAccountRepository arAccountRepository,
            ServiceChargeResolver serviceChargeResolver,
            FixedDecimalUtil fixedDecimalUtil,
            ExchangeRateService exchangeRateService,
            CurrencyRepository currencyRepository,
            CountryRepository countryRepository,
            ObjectProvider<CacheService> cacheServiceProvider,
            ShipmentCostBasisCalculator shipmentCostBasisCalculator,
            RevenueBillingAccountResolver revenueBillingAccountResolver,
            DomEntityConfigResolver domEntityConfigResolver,
            RecordProcessingLookups recordProcessingLookups) {
        this.accountRepository = accountRepository;
        this.arAccountRepository = arAccountRepository;
        this.serviceChargeResolver = serviceChargeResolver;
        this.fixedDecimalUtil = fixedDecimalUtil;
        this.exchangeRateService = exchangeRateService;
        this.currencyRepository = currencyRepository;
        this.countryRepository = countryRepository;
        this.cacheServiceProvider = cacheServiceProvider;
        this.shipmentCostBasisCalculator = shipmentCostBasisCalculator;
        this.revenueBillingAccountResolver = revenueBillingAccountResolver;
        this.domEntityConfigResolver = domEntityConfigResolver;
        this.recordProcessingLookups = recordProcessingLookups;
    }

    public static String exchangeRateKeyPrefix(ChargeCalculationContext ctx) {
        if (ctx == null) {
            return null;
        }
        String fileId = ctx.getRawUploadFileId();
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        return "ExchangeRate:" + fileId.strip();
    }

    public BigDecimal applyExchangeToLineAmount(BigDecimal amount, Map<String, String> source, Charge charge) {
        return toConvertedAmount(applyExchangeToLine(amount, source, charge));
    }

    public BigDecimal applyExchangeToLineAmount(
            BigDecimal amount,
            Map<String, String> source,
            Charge charge,
            String keyPrefix) {
        return toConvertedAmount(applyExchangeToLine(amount, source, charge, keyPrefix));
    }

    public CurrencyConversionResult applyExchangeToLine(BigDecimal amount, Map<String, String> source, Charge charge) {
        return applyExchangeToLine(amount, source, charge, null);
    }

    public CurrencyConversionResult applyExchangeToLine(
            BigDecimal amount,
            Map<String, String> source,
            Charge charge,
            String keyPrefix) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return new CurrencyConversionResult(0.0, null);
        }
        String from = sourceValue(source, BatchProcessingConstants.SOURCE_KEY_CURRENCY);
        if (from.isBlank()) {
            from = BatchProcessingConstants.DEFAULT_CURRENCY;
        }
        String to = charge != null && charge.getCurrency() != null ? charge.getCurrency().strip() : "";
        if (to.isBlank()) {
            to = BatchProcessingConstants.DEFAULT_CURRENCY;
        }
        CurrencyConversionResult conversion =
                exchangeRateService.convertAmount(amount.doubleValue(), from, to, null, keyPrefix);
        if (conversion == null) {
            return new CurrencyConversionResult(amount.doubleValue(), null);
        }
        return conversion;
    }

    private static BigDecimal toConvertedAmount(CurrencyConversionResult conversion) {
        return conversion == null || conversion.convertedAmount() == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(conversion.convertedAmount());
    }

    public List<ServiceChargeResponse> resolveServiceCharges(
            String entityCode,
            Account account,
            String chargeCode) {
        return recordProcessingLookups.serviceCharges(
                entityCode,
                account,
                chargeCode,
                () -> serviceChargeResolver.resolve(
                        ServiceChargeQueryCriteria.builder()
                                .entityCode(entityCode)
                                .account(account)
                                .chargeTypes(List.of(chargeCode))
                                .active(true)
                                .build()));
    }

    public BigDecimal resolveTaxPercentForServiceCode(
            String entityCode,
            Account account,
            String chargeCode,
            String serviceCode) {
        if (account == null || chargeCode == null || serviceCode == null) {
            return BigDecimal.ZERO;
        }
        String normalizedServiceCode = serviceCode.strip().toUpperCase(Locale.ROOT);
        for (ServiceChargeResponse serviceCharge : resolveServiceCharges(entityCode, account, chargeCode)) {
            if (serviceCharge == null || serviceCharge.getServiceCode() == null) {
                continue;
            }
            if (serviceCharge.getServiceCode().strip().toUpperCase(Locale.ROOT).equals(normalizedServiceCode)) {
                return serviceCharge.getTaxValue() != null
                        ? BigDecimal.valueOf(serviceCharge.getTaxValue())
                        : BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    /** Destination entity (ECD and similar). */
    public String resolveEntityCode(Shipment shipment) {
        return resolveDestinationEntityCode(shipment);
    }

    public String resolveDestinationEntityCode(Shipment shipment) {
        return blankToEmpty(shipment.getHAWBDestinationEntity());
    }

    public String resolveDomDestinationChargeEntityCode(Shipment shipment, String chargeCode) {
        if (isDomesticProductGroup(shipment) && usesDomDestinationEntityOverride(chargeCode)) {
            return domEntityConfigResolver.resolveDomEntityCode(shipment.getConsigneeCountryCode());
        }
        return resolveDestinationEntityCode(shipment);
    }

    public String resolveDtcEntityCode(Shipment shipment) {
        return resolveDomDestinationChargeEntityCode(shipment, BatchProcessingConstants.CHARGE_CODE_DTC);
    }

    public static boolean usesDomDestinationEntityOverride(String chargeCode) {
        return chargeCode != null
                && DOM_DESTINATION_CHARGE_CODES.contains(normalizeChargeCode(chargeCode));
    }

    public static boolean isDomesticProductGroup(Shipment shipment) {
        if (shipment == null || shipment.getProductGroup() == null) {
            return false;
        }
        return BatchProcessingConstants.PRODUCT_GROUP_DOM.equalsIgnoreCase(shipment.getProductGroup().strip());
    }

    public String resolveOriginEntityCode(Shipment shipment) {
        return blankToEmpty(shipment.getOriginEntity());
    }

    public String resolveThirdPartyEntityCode(Shipment shipment) {
        return blankToEmpty(shipment.getThirdPartyEntity());
    }

    public boolean matchesPaymentType(Shipment shipment, String expectedPaymentType) {
        if (shipment == null || expectedPaymentType == null) {
            return false;
        }
        String actual = shipment.getPaymentType();
        if (actual == null || actual.isBlank()) {
            return false;
        }
        return actual.strip().equalsIgnoreCase(expectedPaymentType.strip());
    }

    public List<Account> resolveAccounts(Shipment shipment, String entityCode, String chargeCode) {
        if (usesEntitySettingsAccounts(chargeCode)) {
            return AccountLoaders.selectFirstAccount(resolveEntitySettingsAccounts(entityCode, chargeCode));
        }
        if (BatchProcessingConstants.CHARGE_CODE_DTA.equals(normalizeChargeCode(chargeCode))) {
            return AccountLoaders.selectFirstAccount(resolveDtaAccounts(entityCode));
        }
        String partyAccountNumber = resolvePartyAccountNumber(shipment, chargeCode);
        if (!partyAccountNumber.isBlank()) {
            String normalizedAccountNumber = partyAccountNumber.strip();
            Optional<Account> ar = recordProcessingLookups.partyArAccount(
                    entityCode,
                    normalizedAccountNumber,
                    chargeCode,
                    () -> accountRepository.findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                            normalizedAccountNumber, entityCode, AccountNature.AR, chargeCode));
            if (ar.isPresent()) {
                return List.of(ar.get());
            }
        }
        return AccountLoaders.selectFirstAccount(resolveEntitySettingsAccounts(entityCode, chargeCode));
    }

    /**
     * Resolves DTC billing account for new line creation using consignee party number,
     * or shipper number when product group is DOM.
     */
    public Optional<Account> resolveDtcBillingAccount(Shipment shipment, String entityCode) {
        if (shipment == null || entityCode == null || entityCode.isBlank()) {
            return Optional.empty();
        }
        String partyAccount = resolvePartyAccountNumber(shipment, BatchProcessingConstants.CHARGE_CODE_DTC);
        return recordProcessingLookups.dtcBillingAccount(
                entityCode.strip(),
                partyAccount,
                () -> revenueBillingAccountResolver.resolveBillingAccount(
                        entityCode.strip(),
                        RevenueBillingAccountResolver.CHARGE_TYPE_DTC,
                        partyAccount));
    }

    public List<Account> resolveAccountsForNewDtcLine(Shipment shipment, String entityCode) {
        return resolveDtcBillingAccount(shipment, entityCode)
                .map(List::of)
                .orElseGet(() -> resolveAccounts(shipment, entityCode, BatchProcessingConstants.CHARGE_CODE_DTC));
    }

    /**
     * Keeps accounts on existing DTC lines for updates; adds resolver account for newly active service codes.
     */
    public List<Account> resolveAccountsForDtcProcessing(
            Shipment shipment, String entityCode, List<ChargeLineItem> existingLines) {
        if (existingLines == null || existingLines.isEmpty()) {
            return resolveAccountsForNewDtcLine(shipment, entityCode);
        }
        LinkedHashMap<UUID, Account> accountsById = new LinkedHashMap<>();
        for (ChargeLineItem line : existingLines) {
            if (line != null && line.getAccount() != null) {
                accountsById.putIfAbsent(line.getAccount().getId(), line.getAccount());
            }
        }
        resolveDtcBillingAccount(shipment, entityCode)
                .ifPresent(account -> accountsById.putIfAbsent(account.getId(), account));
        if (accountsById.isEmpty()) {
            return resolveAccounts(shipment, entityCode, BatchProcessingConstants.CHARGE_CODE_DTC);
        }
        return List.copyOf(accountsById.values());
    }

    private List<Account> resolveDtaAccounts(String entityCode) {
        return recordProcessingLookups.dtaAccounts(entityCode, () -> loadDtaAccounts(entityCode));
    }

    private List<Account> loadDtaAccounts(String entityCode) {
        Optional<Account> entitySettings = accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                entityCode,
                AccountNature.ENTITY_SETTINGS,
                BatchProcessingConstants.CHARGE_CODE_DTA);
        if (entitySettings.isEmpty()) {
            return resolveEntitySettingsAccounts(entityCode, BatchProcessingConstants.CHARGE_CODE_DTA);
        }
        String defaultArAccountNumber = blankToEmpty(entitySettings.get().getDefaultArAccountNumber());
        if (defaultArAccountNumber.isBlank()) {
            return resolveEntitySettingsAccounts(entityCode, BatchProcessingConstants.CHARGE_CODE_DTA);
        }
        Optional<ARAccount> arMaster = recordProcessingLookups.arMaster(
                entityCode,
                defaultArAccountNumber,
                () -> arAccountRepository.findByArAccountNumberAndEntityCode(
                        defaultArAccountNumber, entityCode));
        if (arMaster.isEmpty()) {
            return resolveEntitySettingsAccounts(entityCode, BatchProcessingConstants.CHARGE_CODE_DTA);
        }
        Optional<Account> dtaAccount = AccountLoaders.findArChargeConfig(
                accountRepository,
                arMaster.get(),
                BatchProcessingConstants.CHARGE_CODE_DTA);
        return dtaAccount.map(List::of)
                .orElseGet(() -> resolveEntitySettingsAccounts(entityCode, BatchProcessingConstants.CHARGE_CODE_DTA));
    }

    private List<Account> resolveEntitySettingsAccounts(String entityCode, String chargeCode) {
        return recordProcessingLookups.entitySettingsAccounts(
                entityCode,
                chargeCode,
                () -> accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeTypeOrderByChargeTypeAsc(
                        entityCode, AccountNature.ENTITY_SETTINGS, chargeCode));
    }

    public static boolean usesEntitySettingsAccounts(String chargeCode) {
        return chargeCode != null && ENTITY_SETTINGS_COST_CHARGE_CODES.contains(chargeCode.strip().toUpperCase(Locale.ROOT));
    }

    static String resolvePartyAccountNumber(Shipment shipment, String chargeCode) {
        if (shipment == null) {
            return "";
        }
        String normalizedChargeCode = chargeCode == null ? "" : chargeCode.strip().toUpperCase(Locale.ROOT);
        return switch (normalizedChargeCode) {
            case BatchProcessingConstants.CHARGE_CODE_DTS -> blankToEmpty(shipment.getShipperNumber());
            case BatchProcessingConstants.CHARGE_CODE_DT3 -> blankToEmpty(shipment.getThirdPartyNumber());
            case BatchProcessingConstants.CHARGE_CODE_DTC -> isDomesticProductGroup(shipment)
                    ? blankToEmpty(shipment.getShipperNumber())
                    : blankToEmpty(shipment.getConsigneeNumber());
            default -> blankToEmpty(shipment.getConsigneeNumber());
        };
    }

    public List<ChargeLineItem> buildLineItemsForCharge(
            Charge charge,
            List<Account> accounts,
            Map<String, String> source,
            String chargeCode,
            String entityCode,
            UUID fileReferenceId,
            String paymentReference) {
        return buildLineItemsForCharge(
                charge, accounts, source, chargeCode, entityCode, fileReferenceId, paymentReference, null, false);
    }

    public List<ChargeLineItem> buildLineItemsForCharge(
            Charge charge,
            List<Account> accounts,
            Map<String, String> source,
            String chargeCode,
            String entityCode,
            UUID fileReferenceId,
            String paymentReference,
            DtaContextSupport.DtaLineContext dta,
            boolean actualCharge) {
        return buildLineItemsForCharge(
                charge, accounts, source, chargeCode, entityCode,
                fileReferenceId, paymentReference, dta, actualCharge, null);
    }

    public List<ChargeLineItem> buildLineItemsForCharge(
            Charge charge,
            List<Account> accounts,
            Map<String, String> source,
            String chargeCode,
            String entityCode,
            UUID fileReferenceId,
            String paymentReference,
            DtaContextSupport.DtaLineContext dta,
            boolean actualCharge,
            String exchangeRateKeyPrefix) {
        List<ChargeLineItem> lines = new ArrayList<>();
        boolean zeroAmounts = requiresZeroAmounts(charge != null ? charge.getShipment() : null);
        Map<String, BigDecimal> dtaAmounts =
                dta != null ? dta.amountsByDescription() : Map.of();
        boolean useDtaAmounts = dta != null && dta.useDtaAmounts();
        String normalizedChargeCode = chargeCode == null ? "" : chargeCode.strip().toUpperCase(Locale.ROOT);
        boolean actualDtsDt3 = actualCharge && useDtaAmounts && usesDtsStyleLineAmountResolution(normalizedChargeCode);
        for (Account account : AccountLoaders.selectFirstAccount(accounts)) {
            List<ServiceChargeResponse> serviceCharges =
                    resolveServiceCharges(entityCode, account, chargeCode);
            for (ServiceChargeResponse sc : serviceCharges) {
                lines.add(buildChargeLineItem(
                        charge,
                        account,
                        sc,
                        source,
                        chargeCode,
                        zeroAmounts,
                        actualCharge,
                        actualDtsDt3,
                        useDtaAmounts,
                        dtaAmounts,
                        dta,
                        fileReferenceId,
                        paymentReference,
                        exchangeRateKeyPrefix));
            }
        }
        return lines;
    }

    private ChargeLineItem buildChargeLineItem(
            Charge charge,
            Account account,
            ServiceChargeResponse sc,
            Map<String, String> source,
            String chargeCode,
            boolean zeroAmounts,
            boolean actualCharge,
            boolean actualDtsDt3,
            boolean useDtaAmounts,
            Map<String, BigDecimal> dtaAmounts,
            DtaContextSupport.DtaLineContext dta,
            UUID fileReferenceId,
            String paymentReference,
            String exchangeRateKeyPrefix) {
        ChargeLineItem line = new ChargeLineItem();
        line.setCharge(charge);
        line.setAccount(account);
        line.setServiceCode(sc.getServiceCode());
        line.setServiceCodeDescription(sc.getDescription());
        line.setContractedRate(BigDecimal.ZERO);
        if (actualCharge) {
            applyApAccountNumberFromSource(line, source);
        } else {
            applyApAccountNumber(line, sc);
        }
        Shipment shipment = charge != null ? charge.getShipment() : null;
        String normalizedChargeCode = normalizeChargeCode(chargeCode);
        boolean actualDta = actualCharge
                && BatchProcessingConstants.CHARGE_CODE_DTA.equals(normalizedChargeCode);
        BigDecimal taxPercent = sc.getTaxValue() != null
                ? BigDecimal.valueOf(sc.getTaxValue())
                : BigDecimal.ZERO;

        BigDecimal resolvedAmount = resolveAmountForLineItem(
                chargeCode, source, sc, zeroAmounts, actualDtsDt3, useDtaAmounts, dtaAmounts);
        if (actualCharge) {
            BigDecimal incoming = zeroAmounts ? BigDecimal.ZERO : resolvedAmount;
            line.setPreConvertedAmount(formatAmount(incoming, charge));
        }
        CurrencyConversionResult exchange = zeroAmounts
                ? new CurrencyConversionResult(0.0, null)
                : convertActualOrDefaultLineAmount(
                        actualDta,
                        actualDtsDt3,
                        normalizedChargeCode,
                        chargeCode,
                        resolvedAmount,
                        source,
                        shipment,
                        sc,
                        charge,
                        exchangeRateKeyPrefix);
        BigDecimal preTaxableAmount = toConvertedAmount(exchange);
        if (actualCharge) {
            applyContextDerivedLineAmounts(line, preTaxableAmount, charge);
        } else {
            applyTaxedLineAmounts(line, preTaxableAmount, taxPercent, charge);
        }
        if (dta != null && dta.rawCapture() != null && !zeroAmounts) {
            BigDecimal capturedAmount = line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
            dta.mergeCapture(sc.getDescription(), capturedAmount);
        }
        line.setExchangeRate(exchange.exchangeRate());
        line.setFileReferenceId(fileReferenceId);
        line.setPaymentReference(paymentReference);
        line.setInfoAxsId(sc.getInfoAxsId());
        applySourceFields(line, source);
        return line;
    }

    static void applyApAccountNumber(ChargeLineItem line, ServiceChargeResponse sc) {
        if (line == null || sc == null
                || sc.getDefaultAccountNumber() == null
                || sc.getDefaultAccountNumber().isBlank()) {
            return;
        }
        line.setApAccountNumber(sc.getDefaultAccountNumber().trim());
    }

    static void applyApAccountNumberFromSource(ChargeLineItem line, Map<String, String> source) {
        if (line == null) {
            return;
        }
        String apAccount = sourceValue(source, BatchProcessingConstants.SOURCE_KEY_AP_ACCOUNT);
        if (apAccount.isBlank()) {
            return;
        }
        line.setApAccountNumber(apAccount.trim());
    }

    public void applyTaxedLineAmounts(
            ChargeLineItem line,
            BigDecimal preTaxableAmount,
            BigDecimal taxPercent,
            Charge charge) {
        BigDecimal preTax = preTaxableAmount == null ? BigDecimal.ZERO : preTaxableAmount;
        line.setAmount(formatAmount(preTax, charge));
        line.setPreTaxableAmount(formatAmount(TaxValueAmountCalculator.apply(preTax, taxPercent), charge));
    }

    public void applyContextDerivedLineAmounts(ChargeLineItem line, BigDecimal amount, Charge charge) {
        BigDecimal formatted = formatAmount(amount == null ? BigDecimal.ZERO : amount, charge);
        line.setAmount(formatted);
        line.setPreTaxableAmount(formatted);
    }

    private CurrencyConversionResult convertActualOrDefaultLineAmount(
            boolean actualDta,
            boolean actualDtsDt3,
            String normalizedChargeCode,
            String chargeCode,
            BigDecimal amount,
            Map<String, String> source,
            Shipment shipment,
            ServiceChargeResponse serviceCharge,
            Charge charge,
            String exchangeRateKeyPrefix) {
        if (actualDta) {
            return convertEstimateLineAmount(
                    chargeCode, amount, null, source, shipment, serviceCharge, charge, false,
                    BigDecimal.ZERO, exchangeRateKeyPrefix);
        }
        if (actualDtsDt3 && usesDtsStyleLineAmountResolution(normalizedChargeCode)) {
            return convertEstimateLineAmount(
                    chargeCode, amount, null, source, shipment, serviceCharge, charge, true,
                    BigDecimal.ZERO, exchangeRateKeyPrefix);
        }
        return applyExchangeToLine(amount, source, charge, exchangeRateKeyPrefix);
    }

    private static BigDecimal resolveAmountForLineItem(
            String chargeCode,
            Map<String, String> source,
            ServiceChargeResponse sc,
            boolean zeroAmounts,
            boolean actualDtsDt3,
            boolean useDtaAmounts,
            Map<String, BigDecimal> dtaAmounts) {
        if (zeroAmounts) {
            return BigDecimal.ZERO;
        }
        if (actualDtsDt3) {
            return amountFromDtaContext(sc, dtaAmounts);
        }
        return resolveLineAmount(chargeCode, source, sc, useDtaAmounts, dtaAmounts);
    }

    public BigDecimal formatAmount(BigDecimal amount, Charge charge) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        Double formatted = fixedDecimalUtil.convertToFixedDecimal(amount.doubleValue(), entityCodeFromCharge(charge));
        return formatted == null ? BigDecimal.ZERO : BigDecimal.valueOf(formatted);
    }

    public void applyChargeHeaderAmounts(Charge charge, List<ChargeLineItem> lines) {
        ChargeHeaderAmountSupport.applyToCharge(charge, lines, amount -> formatAmount(amount, charge));
    }

    public BigDecimal convertAmountBetweenCurrencies(
            BigDecimal amount,
            String fromCurrency,
            String toCurrency,
            String exchangeRateKeyPrefix) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        String from = normalizeCurrencyCode(fromCurrency);
        String to = normalizeCurrencyCode(toCurrency);
        if (from.equalsIgnoreCase(to)) {
            return amount;
        }
        CurrencyConversionResult conversion = convertAmount(amount, from, to, exchangeRateKeyPrefix);
        if (conversion == null || conversion.convertedAmount() == null) {
            return amount;
        }
        return BigDecimal.valueOf(conversion.convertedAmount());
    }

    private static String normalizeCurrencyCode(String currency) {
        return currency == null || currency.isBlank()
                ? BatchProcessingConstants.DEFAULT_CURRENCY
                : currency.strip();
    }

    static String entityCodeFromCharge(Charge charge) {
        if (charge == null || charge.getEntity() == null || charge.getEntity().getCode() == null) {
            return "";
        }
        return charge.getEntity().getCode().strip();
    }

    public static boolean isZeroAmountShipmentType(Shipment shipment) {
        return shipment != null
                && BatchProcessingConstants.SHIPMENT_TYPE_DDO.equalsIgnoreCase(blankToEmpty(shipment.getShipmentType()));
    }

    public static boolean isExemptedCategoryType(Shipment shipment) {
        return shipment != null
                && BatchProcessingConstants.CATEGORY_TYPE_EXEMPTED.equalsIgnoreCase(
                        blankToEmpty(shipment.getCategoryType()));
    }

    public static boolean requiresZeroAmounts(Shipment shipment) {
        return isZeroAmountShipmentType(shipment) || isExemptedCategoryType(shipment);
    }

    public static String resolveChargeStatusForEstimate(Shipment shipment) {
        return requiresZeroAmounts(shipment)
                ? BatchProcessingConstants.STATUS_EXEMPTED
                : BatchProcessingConstants.STATUS_DUE;
    }

    public static UUID parseOptionalFileReference(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ChargeCreationConstants.INVALID_FILE_REFERENCE_ID + ": " + raw);
        }
    }

    public static boolean isRevenueServiceLine(ServiceChargeResponse sc) {
        if (sc == null) {
            return false;
        }
        return BatchProcessingConstants.REVENUE_TYPE_CODE.equalsIgnoreCase(blankToEmpty(sc.getType()));
    }

    public static BigDecimal amountFromSource(Map<String, String> sourceRecordLowerKeys, ServiceChargeResponse sc) {
        return amountFromSourceMap(sourceRecordLowerKeys, sc);
    }

    static BigDecimal resolveLineAmount(
            String chargeCode,
            Map<String, String> source,
            ServiceChargeResponse sc,
            boolean isRevenueAdd,
            Map<String, BigDecimal> dtaRevenueByDescription) {
        if (!isRevenueAdd) {
            return amountFromSourceMap(source, sc);
        }
        return resolveRevenueAddLineAmount(chargeCode, source, sc, dtaRevenueByDescription);
    }

    private static BigDecimal resolveRevenueAddLineAmount(
            String chargeCode,
            Map<String, String> source,
            ServiceChargeResponse sc,
            Map<String, BigDecimal> dtaRevenueByDescription) {
        String normalizedCode = normalizeChargeCode(chargeCode);
        if (usesDtsStyleLineAmountResolution(normalizedCode)) {
            return amountFromDtaContext(sc, dtaRevenueByDescription);
        }
        if (usesDtaRevenueContextFallback(normalizedCode)) {
            return amountFromDtaContext(sc, dtaRevenueByDescription);
        }
        return amountFromSourceMap(source, sc);
    }

    private static String normalizeChargeCode(String chargeCode) {
        return chargeCode == null ? "" : chargeCode.strip().toUpperCase(Locale.ROOT);
    }

    public static boolean usesDtsStyleLineAmountResolution(String normalizedChargeCode) {
        return BatchProcessingConstants.CHARGE_CODE_DTS.equals(normalizedChargeCode)
                || BatchProcessingConstants.CHARGE_CODE_DT3.equals(normalizedChargeCode);
    }

    public static boolean usesDtaRevenueContextFallback(String normalizedChargeCode) {
        return BatchProcessingConstants.CHARGE_CODE_ECS.equals(normalizedChargeCode)
                || BatchProcessingConstants.CHARGE_CODE_EC3.equals(normalizedChargeCode);
    }

    public static String resolveDtaMappingChargeType(String chargeCode) {
        if (chargeCode == null) {
            return "";
        }
        return switch (chargeCode.strip().toUpperCase(Locale.ROOT)) {
            case BatchProcessingConstants.CHARGE_CODE_ECS -> BatchProcessingConstants.CHARGE_CODE_ECS;
            case BatchProcessingConstants.CHARGE_CODE_DTS -> BatchProcessingConstants.CHARGE_CODE_DTS;
            case BatchProcessingConstants.CHARGE_CODE_EC3 -> BatchProcessingConstants.CHARGE_CODE_EC3;
            case BatchProcessingConstants.CHARGE_CODE_DT3 -> BatchProcessingConstants.CHARGE_CODE_DT3;
            default -> "";
        };
    }

    /**
     * DTS/DT3 service descriptions match ECS/EC3; on Actual, reuse ECS/EC3 mapper rows for DTA context remapping.
     */
    public static String resolveDtaContextMappingChargeType(String chargeCode, boolean actualProcess) {
        String mappingType = resolveDtaMappingChargeType(chargeCode);
        if (!actualProcess || mappingType.isBlank()) {
            return mappingType;
        }
        return switch (mappingType) {
            case BatchProcessingConstants.CHARGE_CODE_DTS -> BatchProcessingConstants.CHARGE_CODE_ECS;
            case BatchProcessingConstants.CHARGE_CODE_DT3 -> BatchProcessingConstants.CHARGE_CODE_EC3;
            default -> mappingType;
        };
    }

    static String normalizeDescription(String description) {
        return description == null ? "" : description.strip().toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    static Map<String, BigDecimal> dtaRevenueMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, BigDecimal>) map : Map.of();
    }

    private static BigDecimal amountFromDtaContext(
            ServiceChargeResponse sc,
            Map<String, BigDecimal> dtaContextByDescription) {
        String key = normalizeDescription(sc.getDescription());
        if (key.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return dtaContextByDescription.getOrDefault(key, BigDecimal.ZERO);
    }

    private static BigDecimal amountFromSourceMap(Map<String, String> sourceRecordLowerKeys, ServiceChargeResponse sc) {
        String descriptionKey = normalizeDescription(sc.getDescription());
        String raw = sourceRecordLowerKeys.get(descriptionKey);
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal amount = new BigDecimal(raw.trim());
            if (amount.signum() < 0) {
                String safeRaw = truncateForErrorMessage(raw.trim(), 80);
                String columnLabel = descriptionKey.isEmpty() ? "(unknown column)" : descriptionKey;
                throw new ValidationException(String.format(
                        ChargeCreationConstants.INVALID_NEGATIVE_AMOUNT_FOR_COLUMN, columnLabel, safeRaw));
            }
            return amount;
        } catch (NumberFormatException ex) {
            String safeRaw = truncateForErrorMessage(raw.trim(), 80);
            String columnLabel = descriptionKey.isEmpty() ? "(unknown column)" : descriptionKey;
            throw new ValidationException(String.format(
                    ChargeCreationConstants.INVALID_NUMERIC_AMOUNT_FOR_COLUMN, columnLabel, safeRaw));
        }
    }

    private static String truncateForErrorMessage(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen) + "...";
    }

    private static void applySourceFields(ChargeLineItem line, Map<String, String> source) {
        line.setPaymentReference(sourceValue(source, BatchProcessingConstants.PAYMENT_REFERENCE));
        line.setDeclarationNumber(sourceValue(source, BatchProcessingConstants.SOURCE_KEY_DECLARATION_NUMBER));
        line.setConsolGroup(sourceValue(source, BatchProcessingConstants.SOURCE_KEY_CONSOL_GROUP));
        line.setPayingEntity(sourceValue(source, BatchProcessingConstants.SOURCE_KEY_PAYING_ENTITY));
    }

    public static String sourceValue(Map<String, String> source, String key) {
        if (source == null || key == null) {
            return "";
        }
        String raw = source.get(key.toLowerCase(Locale.ROOT));
        return raw == null ? "" : raw.strip();
    }

    private static String blankToEmpty(String s) {
        return s == null ? "" : s.strip();
    }

    public void applyEstimateChargeCurrency(Charge charge, String chargeCode, String billingEntityCode, Shipment shipment) {
        charge.setCurrency(resolveEstimateTargetCurrency(chargeCode, billingEntityCode, shipment));
    }

    public void applyChargeCurrency(
            Charge charge,
            String chargeCode,
            String billingEntityCode,
            Shipment shipment,
            boolean calculatorHelperEnabled) {
        if (calculatorHelperEnabled && usesEntitySettingsAccounts(chargeCode)) {
            charge.setCurrency(resolveCostChargeCurrency(chargeCode, billingEntityCode, shipment));
            return;
        }
        applyEstimateChargeCurrency(charge, chargeCode, billingEntityCode, shipment);
    }

    private String resolveCostChargeCurrency(String chargeCode, String billingEntityCode, Shipment shipment) {
        String normalized = normalizeChargeCode(chargeCode);
        if (BatchProcessingConstants.CHARGE_CODE_ECS.equals(normalized)
                || BatchProcessingConstants.CHARGE_CODE_EC3.equals(normalized)) {
            return USD;
        }
        return resolveDestinationCurrency(billingEntityCode);
    }

    public CurrencyConversionResult convertEstimateLineAmount(
            String chargeCode,
            BigDecimal amount,
            RateSheetAmountBreakdown rateSheetForFx,
            Map<String, String> source,
            Shipment shipment,
            ServiceChargeResponse serviceCharge,
            Charge charge,
            boolean fromDtaContext,
            BigDecimal taxPercent,
            String exchangeRateKeyPrefix) {
        return convertEstimateLineAmount(
                chargeCode,
                amount,
                rateSheetForFx,
                source,
                shipment,
                serviceCharge,
                charge,
                fromDtaContext,
                false,
                taxPercent,
                exchangeRateKeyPrefix);
    }

    public CurrencyConversionResult convertEstimateLineAmount(
            String chargeCode,
            BigDecimal amount,
            RateSheetAmountBreakdown rateSheetForFx,
            Map<String, String> source,
            Shipment shipment,
            ServiceChargeResponse serviceCharge,
            Charge charge,
            boolean fromDtaContext,
            boolean siblingChargeContext,
            BigDecimal taxPercent,
            String exchangeRateKeyPrefix) {
        requireEstimateLineContext(charge, shipment, serviceCharge);
        if (rateSheetForFx != null) {
            return convertRateSheetEstimateLineAmount(
                    rateSheetForFx,
                    taxPercent,
                    chargeCode,
                    shipment,
                    charge,
                    exchangeRateKeyPrefix);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return new CurrencyConversionResult(0.0, null);
        }
        String normalized = normalizeChargeCode(chargeCode);
        String targetCurrency = blankToEmpty(charge.getCurrency());
        if (targetCurrency.isBlank()) {
            targetCurrency = BatchProcessingConstants.DEFAULT_CURRENCY;
        }
        if (fromDtaContext && usesDtaRevenueContextFallback(normalized)) {
            return new CurrencyConversionResult(amount.doubleValue(), 1.0);
        }
        if (fromDtaContext && usesDtsStyleLineAmountResolution(normalized)) {
            return convertAmount(amount, USD, targetCurrency, exchangeRateKeyPrefix);
        }
        String fromCurrency = sourceValue(source, BatchProcessingConstants.SOURCE_KEY_CURRENCY);
        if (fromCurrency.isBlank()) {
            fromCurrency = BatchProcessingConstants.DEFAULT_CURRENCY;
        }
        return convertAmount(amount, fromCurrency, targetCurrency, exchangeRateKeyPrefix);
    }

    private static void requireEstimateLineContext(
            Charge charge,
            Shipment shipment,
            ServiceChargeResponse serviceCharge) {
        if (charge == null) {
            throw new ValidationException(ChargeCreationConstants.MISSING_CHARGE_FOR_ESTIMATE_CONVERSION);
        }
        if (shipment == null) {
            throw new ValidationException(ChargeCreationConstants.MISSING_SHIPMENT_FOR_ESTIMATE_CONVERSION);
        }
        if (serviceCharge == null) {
            throw new ValidationException(ChargeCreationConstants.MISSING_SERVICE_CHARGE_FOR_ESTIMATE_CONVERSION);
        }
    }

    private CurrencyConversionResult convertRateSheetEstimateLineAmount(
            RateSheetAmountBreakdown breakdown,
            BigDecimal taxPercent,
            String chargeCode,
            Shipment shipment,
            Charge charge,
            String exchangeRateKeyPrefix) {
        BigDecimal fixedTotal = breakdown.fixedPart() == null ? BigDecimal.ZERO : breakdown.fixedPart();
        List<PercentageRateSpec> percentageRates = breakdown.percentageRates() == null
                ? List.of()
                : breakdown.percentageRates();
        if (fixedTotal.compareTo(BigDecimal.ZERO) == 0 && percentageRates.isEmpty()) {
            return new CurrencyConversionResult(0.0, null);
        }
        String normalizedCharge = normalizeChargeCode(chargeCode);
        String targetCurrency = blankToEmpty(charge.getCurrency());
        if (targetCurrency.isBlank()) {
            targetCurrency = BatchProcessingConstants.DEFAULT_CURRENCY;
        }
        String billingEntityCode = resolveBillingEntityCode(charge, shipment);
        String entityBasedCurrency = resolveEntityBasedCurrency(normalizedCharge, billingEntityCode, shipment);
        String customsCurrency = resolveCustomsCurrency(shipment);

        CurrencyConversionResult fixedFx = convertRateSheetFixedAmount(
                fixedTotal, shipment, charge, targetCurrency, exchangeRateKeyPrefix);
        BigDecimal taxedFixed = TaxValueAmountCalculator.apply(
                toScaledBigDecimal(fixedFx.convertedAmount()), taxPercent);
        if (percentageRates.isEmpty()) {
            return new CurrencyConversionResult(taxedFixed.doubleValue(), fixedFx.exchangeRate());
        }

        BigDecimal percentageTotalInTarget = BigDecimal.ZERO;
        Double lastExchangeRate = fixedFx.exchangeRate();
        boolean dtaCharge = BatchProcessingConstants.CHARGE_CODE_DTA.equals(normalizedCharge);
        for (PercentageRateSpec spec : percentageRates) {
            if (spec == null || spec.percentageOf() == null) {
                continue;
            }
            BigDecimal ratePercent = spec.ratePercent() == null ? BigDecimal.ZERO : spec.ratePercent();
            if (ratePercent.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal basis = shipmentCostBasisCalculator.resolveBasis(shipment, spec.percentageOf());
            if (basis.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            CurrencyConversionResult basisFx = convertAmount(
                    basis, customsCurrency, entityBasedCurrency, exchangeRateKeyPrefix);
            BigDecimal basisInEntity = toScaledBigDecimal(basisFx.convertedAmount());
            BigDecimal percentageAmount = basisInEntity.multiply(ratePercent)
                    .divide(BigDecimal.valueOf(100), AMOUNT_SCALE, RoundingMode.HALF_UP);
            if (dtaCharge) {
                CurrencyConversionResult percentageFx = convertAmount(
                        percentageAmount, entityBasedCurrency, targetCurrency, exchangeRateKeyPrefix);
                percentageAmount = TaxValueAmountCalculator.apply(
                        toScaledBigDecimal(percentageFx.convertedAmount()), taxPercent);
                percentageTotalInTarget = percentageTotalInTarget.add(percentageAmount);
                lastExchangeRate = percentageFx.exchangeRate();
            } else {
                percentageAmount = TaxValueAmountCalculator.apply(percentageAmount, taxPercent);
                percentageTotalInTarget = percentageTotalInTarget.add(percentageAmount);
                lastExchangeRate = basisFx.exchangeRate();
            }
        }
        double total = taxedFixed.doubleValue() + percentageTotalInTarget.doubleValue();
        return new CurrencyConversionResult(total, lastExchangeRate);
    }

    private static BigDecimal toScaledBigDecimal(Double value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private String resolveBillingEntityCode(Charge charge, Shipment shipment) {
        if (charge != null && charge.getEntity() != null) {
            String code = blankToEmpty(charge.getEntity().getCode());
            if (!code.isBlank()) {
                return code;
            }
        }
        return resolveDestinationEntityCode(shipment);
    }

    String resolveEntityBasedCurrency(String normalizedChargeCode, String billingEntityCode, Shipment shipment) {
        if (BatchProcessingConstants.CHARGE_CODE_DTS.equals(normalizedChargeCode)) {
            return resolveDestinationCurrency(blankToEmpty(shipment.getOriginEntity()));
        }
        if (BatchProcessingConstants.CHARGE_CODE_DT3.equals(normalizedChargeCode)) {
            return resolveDestinationCurrency(blankToEmpty(shipment.getThirdPartyEntity()));
        }
        return resolveDestinationCurrency(billingEntityCode);
    }

    private CurrencyConversionResult convertRateSheetFixedAmount(
            BigDecimal fixedTotal,
            Shipment shipment,
            Charge charge,
            String targetCurrency,
            String exchangeRateKeyPrefix) {
        if (fixedTotal.compareTo(BigDecimal.ZERO) == 0) {
            return new CurrencyConversionResult(0.0, null);
        }
        String chargeCode = charge != null ? normalizeChargeCode(charge.getChargeCode()) : "";
        if (!BatchProcessingConstants.CHARGE_CODE_DTA.equals(chargeCode)) {
            return new CurrencyConversionResult(fixedTotal.doubleValue(), 1.0);
        }
        String destinationCurrency = resolveDestinationCurrency(resolveDestinationEntityCode(shipment));
        return convertAmount(fixedTotal, destinationCurrency, targetCurrency, exchangeRateKeyPrefix);
    }

    private String resolveEstimateTargetCurrency(String chargeCode, String billingEntityCode, Shipment shipment) {
        String normalized = normalizeChargeCode(chargeCode);
        if (USD_CHARGE_CODES.contains(normalized)) {
            return USD;
        }
        if (BatchProcessingConstants.CHARGE_CODE_DT3.equals(normalized)) {
            return resolveThirdPartyCountryCurrency(shipment);
        }
        if (BatchProcessingConstants.CHARGE_CODE_DTS.equals(normalized)) {
            return resolveOriginCurrency(shipment);
        }
        return resolveDestinationCurrency(billingEntityCode);
    }

    private String resolveDestinationCurrency(String billingEntityCode) {
        if (billingEntityCode == null || billingEntityCode.isBlank()) {
            return BatchProcessingConstants.DEFAULT_CURRENCY;
        }
        return currencyRepository.findActiveByEntityCode(billingEntityCode.strip())
                .map(currency -> currency.getCurrencyCode().strip())
                .filter(code -> !code.isEmpty())
                .orElse(BatchProcessingConstants.DEFAULT_CURRENCY);
    }

    private String resolveOriginCurrency(Shipment shipment) {
        if (shipment == null) {
            return BatchProcessingConstants.DEFAULT_CURRENCY;
        }
        return resolveCurrencyByCountryCode(shipment.getShipperCountryCode());
    }

    private String resolveThirdPartyCountryCurrency(Shipment shipment) {
        if (shipment == null) {
            return BatchProcessingConstants.DEFAULT_CURRENCY;
        }
        return resolveCurrencyByCountryCode(shipment.getThirdPartyCountry());
    }

    private String resolveCurrencyByCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return BatchProcessingConstants.DEFAULT_CURRENCY;
        }
        return countryRepository.findByCodeIgnoreCase(countryCode.strip())
                .map(this::resolveCurrencyForCountry)
                .orElse(BatchProcessingConstants.DEFAULT_CURRENCY);
    }

    private String resolveCurrencyForCountry(Country country) {
        CacheService cache = cacheServiceProvider.getIfAvailable();
        if (cache != null && country.getName() != null && !country.getName().isBlank()) {
            Optional<String> cached = cache.get(CURRENCY_CACHE_PREFIX + country.getName().strip(), String.class)
                    .map(String::strip)
                    .filter(code -> !code.isEmpty());
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        return currencyRepository.findByCountry_CodeIgnoreCaseAndActiveTrue(country.getCode())
                .map(currency -> currency.getCurrencyCode().strip())
                .filter(code -> !code.isEmpty())
                .orElse(BatchProcessingConstants.DEFAULT_CURRENCY);
    }

    private static String resolveCustomsCurrency(Shipment shipment) {
        String customsCurrency = shipment.getCustomsCurrency();
        return customsCurrency == null || customsCurrency.isBlank()
                ? BatchProcessingConstants.DEFAULT_CURRENCY
                : customsCurrency.strip();
    }

    private CurrencyConversionResult convertAmount(
            BigDecimal amount,
            String fromCurrency,
            String toCurrency,
            String exchangeRateKeyPrefix) {
        CurrencyConversionResult conversion = exchangeRateService.convertAmount(
                amount.doubleValue(), fromCurrency, toCurrency, null, exchangeRateKeyPrefix);
        return conversion == null
                ? new CurrencyConversionResult(amount.doubleValue(), null)
                : conversion;
    }
}
