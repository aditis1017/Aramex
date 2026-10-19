package com.customcharge.persistencelayer.calculatorhelper;

import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.dto.ServiceChargeDescriptionMappingDto;
import com.customcharge.persistencelayer.dto.ServiceChargeQueryCriteria;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository;
import com.customcharge.persistencelayer.util.AccountLoaders;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.persistencelayer.util.FixedDecimalUtil;
import com.customcharge.persistencelayer.util.ServiceChargeResolver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChargeLineCopyService {

    private static final String GLOBAL_ENTITY_CODE = "Global";
    private static final String USD = "USD";
    private static final String DEFAULT_CURRENCY = "SAR";
    private static final int AMOUNT_SCALE = 4;
    private static final Set<String> USD_CHARGE_CODES = Set.of("DTA", "ECS", "EC3");

    private final ServiceChargeResolver serviceChargeResolver;
    private final ServiceChargeMappingRepository serviceChargeMappingRepository;
    private final ExchangeRateService exchangeRateService;
    private final CurrencyRepository currencyRepository;
    private final FixedDecimalUtil fixedDecimalUtil;

    public ChargeLineCopyService(
            ServiceChargeResolver serviceChargeResolver,
            ServiceChargeMappingRepository serviceChargeMappingRepository,
            ExchangeRateService exchangeRateService,
            CurrencyRepository currencyRepository,
            FixedDecimalUtil fixedDecimalUtil) {
        this.serviceChargeResolver = serviceChargeResolver;
        this.serviceChargeMappingRepository = serviceChargeMappingRepository;
        this.exchangeRateService = exchangeRateService;
        this.currencyRepository = currencyRepository;
        this.fixedDecimalUtil = fixedDecimalUtil;
    }

    public enum CopyMatchMode {
        DESCRIPTION,
        SERVICE_CHARGE_MAPPING
    }

    /**
     * Source value selection for copy.
     * <ul>
     *   <li>{@link #AMOUNT_ONLY} — ECD→DTC/DTA, ECS→DTS/DT3: source {@code amount}</li>
     *   <li>{@link #EFFECTIVE_X} — DTA→ECS/EC3: X = contractedRate if set, else amount</li>
     * </ul>
     */
    public enum CopyTaxMode {
        AMOUNT_ONLY,
        EFFECTIVE_X
    }

    @FunctionalInterface
    public interface ServiceChargeLookup {
        List<ServiceChargeResponse> resolve(ServiceChargeQueryCriteria criteria);
    }

    @FunctionalInterface
    public interface DescriptionMappingsLookup {
        List<ServiceChargeDescriptionMappingDto> resolve(
                String sourceEntityCode, String targetEntityCode, String targetChargeType);
    }

    public record ChargeLineCopyRequest(
            String sourceChargeType,
            String targetChargeType,
            Shipment shipment,
            String sourceEntityCode,
            String targetEntityCode,
            Charge sourceCharge,
            Charge targetCharge,
            List<Account> targetAccounts,
            List<ChargeLineItem> sourceLines,
            String exchangeRateKeyPrefix,
            boolean estimateProcess,
            CopyMatchMode matchMode,
            UUID fileReferenceId,
            String paymentReference,
            CopyTaxMode taxMode,
            ServiceChargeLookup serviceChargeLookup,
            DescriptionMappingsLookup descriptionMappingsLookup) {

        public ChargeLineCopyRequest(
                String sourceChargeType,
                String targetChargeType,
                Shipment shipment,
                String sourceEntityCode,
                String targetEntityCode,
                Charge sourceCharge,
                Charge targetCharge,
                List<Account> targetAccounts,
                List<ChargeLineItem> sourceLines,
                String exchangeRateKeyPrefix,
                boolean estimateProcess,
                CopyMatchMode matchMode,
                UUID fileReferenceId,
                String paymentReference,
                CopyTaxMode taxMode) {
            this(
                    sourceChargeType,
                    targetChargeType,
                    shipment,
                    sourceEntityCode,
                    targetEntityCode,
                    sourceCharge,
                    targetCharge,
                    targetAccounts,
                    sourceLines,
                    exchangeRateKeyPrefix,
                    estimateProcess,
                    matchMode,
                    fileReferenceId,
                    paymentReference,
                    taxMode,
                    null,
                    null);
        }
    }

    public record CopyAmountFxResult(BigDecimal convertedAmount, Double exchangeRate) {
    }

    public CopyAmountFxResult convertCopyAmount(
            BigDecimal sourceAmount,
            Shipment shipment,
            Charge sourceCharge,
            Charge targetCharge,
            String sourceChargeType,
            String targetChargeType,
            String sourceEntityCode,
            String targetEntityCode,
            String exchangeRateKeyPrefix,
            boolean usdSiblingConversion) {
        CurrencyConversionResult fx = convertBetweenCharges(
                sourceAmount,
                shipment,
                sourceCharge,
                targetCharge,
                sourceChargeType,
                targetChargeType,
                sourceEntityCode,
                targetEntityCode,
                exchangeRateKeyPrefix,
                usdSiblingConversion);
        BigDecimal converted = format(toBigDecimal(fx.convertedAmount()), targetCharge);
        return new CopyAmountFxResult(converted, fx.exchangeRate());
    }

    public List<ChargeLineItem> copy(ChargeLineCopyRequest request) {
        if (request == null || request.targetCharge() == null || request.targetAccounts() == null) {
            return List.of();
        }
        List<ChargeLineItem> copied = new ArrayList<>();
        Map<String, ChargeLineItem> sourceByDescription = indexSourceLinesByDescription(request.sourceLines());
        Map<String, ChargeLineItem> sourceByServiceCode = indexSourceLinesByServiceCode(request.sourceLines());
        Map<String, BigDecimal> sourceLineAmounts = aggregateSourceLineAmountsByDescription(request.sourceLines());
        Map<String, BigDecimal> sourceLineAmountsByServiceCode =
                aggregateByServiceCode(request.sourceLines(), ChargeLineCopyService::sourceLineAmount);
        Map<String, BigDecimal> sourceEffectiveAmounts =
                aggregateByDescription(request.sourceLines(), ChargeLineCopyService::effectiveAmount);
        Map<String, BigDecimal> sourceEffectiveAmountsByServiceCode =
                aggregateByServiceCode(request.sourceLines(), ChargeLineCopyService::effectiveAmount);
        List<ServiceChargeDescriptionMappingDto> mappings = resolveMappings(request);
        boolean useEffectiveX = request.taxMode() == CopyTaxMode.EFFECTIVE_X;
        boolean siblingChargeContext = isSiblingChargeContext(request);
        List<Account> targetAccounts = AccountLoaders.selectFirstAccount(request.targetAccounts());

        for (Account account : targetAccounts) {
            List<ServiceChargeResponse> serviceCharges = resolveServiceCharges(request, account);
            for (ServiceChargeResponse serviceCharge : serviceCharges) {
                ChargeLineItem sourceLine = findSourceLine(sourceByDescription, sourceByServiceCode, serviceCharge);

                ChargeLineItem line = new ChargeLineItem();
                line.setCharge(request.targetCharge());
                line.setAccount(account);
                line.setServiceCode(serviceCharge.getServiceCode());
                line.setServiceCodeDescription(serviceCharge.getDescription());
                line.setInfoAxsId(serviceCharge.getInfoAxsId());
                line.setFileReferenceId(request.fileReferenceId());
                line.setPaymentReference(request.paymentReference());
                if (sourceLine != null) {
                    line.setDeclarationNumber(sourceLine.getDeclarationNumber());
                    line.setConsolGroup(sourceLine.getConsolGroup());
                    line.setPayingEntity(sourceLine.getPayingEntity());
                    if (!isEcsOrEc3(normalizeChargeCode(request.targetChargeType()))) {
                        line.setApAccountNumber(sourceLine.getApAccountNumber());
                    }
                } else if (serviceCharge.getDefaultAccountNumber() != null
                        && !serviceCharge.getDefaultAccountNumber().isBlank()) {
                    line.setApAccountNumber(serviceCharge.getDefaultAccountNumber().trim());
                }
                if (isEcsOrEc3(normalizeChargeCode(request.targetChargeType()))
                        && serviceCharge.getDefaultAccountNumber() != null
                        && !serviceCharge.getDefaultAccountNumber().isBlank()) {
                    line.setApAccountNumber(serviceCharge.getDefaultAccountNumber().trim());
                }

                BigDecimal sourceValue = resolveSourceCopyValue(
                        request,
                        serviceCharge,
                        sourceByDescription,
                        sourceByServiceCode,
                        useEffectiveX ? sourceEffectiveAmounts : sourceLineAmounts,
                        useEffectiveX ? sourceEffectiveAmountsByServiceCode : sourceLineAmountsByServiceCode,
                        mappings);
                CopyAmountFxResult fx = convertCopyAmount(
                        sourceValue,
                        request.shipment(),
                        request.sourceCharge(),
                        request.targetCharge(),
                        request.sourceChargeType(),
                        request.targetChargeType(),
                        request.sourceEntityCode(),
                        request.targetEntityCode(),
                        request.exchangeRateKeyPrefix(),
                        siblingChargeContext);
                BigDecimal taxPercent = serviceCharge.getTaxValue() != null
                        ? BigDecimal.valueOf(serviceCharge.getTaxValue())
                        : BigDecimal.ZERO;
                applyPreTaxAndTaxedAmounts(line, fx.convertedAmount(), taxPercent, request.targetCharge());
                line.setExchangeRate(fx.exchangeRate());
                line.setContractedRate(BigDecimal.ZERO);
                copied.add(line);
            }
        }
        return copied;
    }

    public static BigDecimal sumLineTotals(List<ChargeLineItem> lines) {
        if (lines == null || lines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ChargeLineItem line : lines) {
            if (line == null) {
                continue;
            }
            total = total.add(effectiveAmount(line));
        }
        return total;
    }

    private BigDecimal resolveSourceCopyValue(
            ChargeLineCopyRequest request,
            ServiceChargeResponse targetServiceCharge,
            Map<String, ChargeLineItem> sourceByDescription,
            Map<String, ChargeLineItem> sourceByServiceCode,
            Map<String, BigDecimal> sourceValuesByDescription,
            Map<String, BigDecimal> sourceValuesByServiceCode,
            List<ServiceChargeDescriptionMappingDto> mappings) {
        if (request.matchMode() == CopyMatchMode.SERVICE_CHARGE_MAPPING) {
            return resolveMappedAmount(
                    targetServiceCharge, sourceValuesByDescription, sourceValuesByServiceCode, mappings);
        }
        ChargeLineItem sourceLine = findSourceLine(sourceByDescription, sourceByServiceCode, targetServiceCharge);
        if (sourceLine == null) {
            return BigDecimal.ZERO;
        }
        return request.taxMode() == CopyTaxMode.EFFECTIVE_X
                ? effectiveAmount(sourceLine)
                : sourceLineAmount(sourceLine);
    }

    private List<ServiceChargeResponse> resolveServiceCharges(ChargeLineCopyRequest request, Account account) {
        ServiceChargeQueryCriteria criteria = ServiceChargeQueryCriteria.builder()
                .entityCode(request.targetEntityCode())
                .account(account)
                .chargeTypes(List.of(request.targetChargeType()))
                .active(true)
                .build();
        if (request.serviceChargeLookup() != null) {
            return request.serviceChargeLookup().resolve(criteria);
        }
        return serviceChargeResolver.resolve(criteria);
    }

    private List<ServiceChargeDescriptionMappingDto> resolveMappings(ChargeLineCopyRequest request) {
        if (request.matchMode() != CopyMatchMode.SERVICE_CHARGE_MAPPING) {
            return List.of();
        }
        if (request.descriptionMappingsLookup() != null) {
            return request.descriptionMappingsLookup().resolve(
                    request.sourceEntityCode(), request.targetEntityCode(), request.targetChargeType());
        }
        List<ServiceChargeDescriptionMappingDto> mappings =
                serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                        request.sourceEntityCode(), request.targetEntityCode(), request.targetChargeType());
        if (mappings.isEmpty()) {
            mappings = serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                    GLOBAL_ENTITY_CODE, GLOBAL_ENTITY_CODE, request.targetChargeType());
        }
        return mappings;
    }

    private CurrencyConversionResult convertBetweenCharges(
            BigDecimal amount,
            Shipment shipment,
            Charge sourceCharge,
            Charge targetCharge,
            String sourceChargeType,
            String targetChargeType,
            String sourceEntityCode,
            String targetEntityCode,
            String exchangeRateKeyPrefix,
            boolean usdSiblingConversion) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return new CurrencyConversionResult(0.0, null);
        }
        String targetCurrency = blankToEmpty(targetCharge.getCurrency());
        if (targetCurrency.isBlank()) {
            targetCurrency = resolveTargetCurrency(targetChargeType, targetEntityCode, shipment);
        }
        String sourceCurrency = blankToEmpty(sourceCharge.getCurrency());
        if (sourceCurrency.isBlank()) {
            sourceCurrency = resolveTargetCurrency(sourceChargeType, sourceEntityCode, shipment);
        }
        if (usdSiblingConversion) {
            return convertAmount(amount, USD, targetCurrency, exchangeRateKeyPrefix);
        }
        return convertAmount(amount, sourceCurrency, targetCurrency, exchangeRateKeyPrefix);
    }

    private String resolveTargetCurrency(String chargeCode, String billingEntityCode, Shipment shipment) {
        String normalized = normalizeChargeCode(chargeCode);
        if (USD_CHARGE_CODES.contains(normalized)) {
            return USD;
        }
        if ("DT3".equals(normalized)) {
            return resolveDestinationCurrency(blankToEmpty(shipment.getThirdPartyEntity()));
        }
        if ("DTS".equals(normalized)) {
            return resolveDestinationCurrency(blankToEmpty(shipment.getOriginEntity()));
        }
        return resolveDestinationCurrency(billingEntityCode);
    }

    private CurrencyConversionResult convertAmount(
            BigDecimal amount,
            String fromCurrency,
            String toCurrency,
            String exchangeRateKeyPrefix) {
        String from = normalizeCurrency(fromCurrency);
        String to = normalizeCurrency(toCurrency);
        if (from.equalsIgnoreCase(to)) {
            return new CurrencyConversionResult(amount.doubleValue(), 1.0);
        }
        CurrencyConversionResult conversion = exchangeRateService.convertAmount(
                amount.doubleValue(), from, to, null, exchangeRateKeyPrefix);
        return conversion == null
                ? new CurrencyConversionResult(amount.doubleValue(), null)
                : conversion;
    }

    private String resolveDestinationCurrency(String billingEntityCode) {
        if (billingEntityCode == null || billingEntityCode.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        return currencyRepository.findActiveByEntityCode(billingEntityCode.strip())
                .map(currency -> currency.getCurrencyCode().strip())
                .filter(code -> !code.isEmpty())
                .orElse(DEFAULT_CURRENCY);
    }

    private BigDecimal format(BigDecimal amount, Charge charge) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        String entityCode = charge != null && charge.getEntity() != null && charge.getEntity().getCode() != null
                ? charge.getEntity().getCode().strip()
                : "";
        Double formatted = fixedDecimalUtil.convertToFixedDecimal(amount.doubleValue(), entityCode);
        return formatted == null ? BigDecimal.ZERO : BigDecimal.valueOf(formatted);
    }

    private void applyPreTaxAndTaxedAmounts(
            ChargeLineItem line,
            BigDecimal preTaxAmount,
            BigDecimal taxPercent,
            Charge charge) {
        BigDecimal preTax = format(preTaxAmount == null ? BigDecimal.ZERO : preTaxAmount, charge);
        line.setAmount(preTax);
        line.setPreTaxableAmount(format(applyTaxPercent(preTax, taxPercent), charge));
    }

    static BigDecimal applyTaxPercent(BigDecimal preTaxAmount, BigDecimal taxPercent) {
        BigDecimal core = preTaxAmount == null ? BigDecimal.ZERO : preTaxAmount;
        if (core.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal tax = taxPercent == null ? BigDecimal.ZERO : taxPercent;
        BigDecimal taxComponent = core.multiply(tax)
                .divide(BigDecimal.valueOf(100), AMOUNT_SCALE, RoundingMode.HALF_UP);
        return core.add(taxComponent).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private static boolean isSiblingChargeContext(ChargeLineCopyRequest request) {
        return request.estimateProcess()
                && ("DTS".equals(request.targetChargeType()) || "DT3".equals(request.targetChargeType()))
                && ("ECS".equals(request.sourceChargeType()) || "EC3".equals(request.sourceChargeType()));
    }

    private static Map<String, ChargeLineItem> indexSourceLinesByDescription(List<ChargeLineItem> sourceLines) {
        Map<String, ChargeLineItem> indexed = new LinkedHashMap<>();
        if (sourceLines == null) {
            return indexed;
        }
        for (ChargeLineItem line : sourceLines) {
            if (line == null) {
                continue;
            }
            String key = normalizeDescription(line.getServiceCodeDescription());
            if (!key.isEmpty()) {
                indexed.put(key, line);
            }
        }
        return indexed;
    }

    private static Map<String, ChargeLineItem> indexSourceLinesByServiceCode(List<ChargeLineItem> sourceLines) {
        Map<String, ChargeLineItem> indexed = new LinkedHashMap<>();
        if (sourceLines == null) {
            return indexed;
        }
        for (ChargeLineItem line : sourceLines) {
            if (line == null) {
                continue;
            }
            String key = normalizeServiceCode(line.getServiceCode());
            if (!key.isEmpty()) {
                indexed.put(key, line);
            }
        }
        return indexed;
    }

    private static Map<String, BigDecimal> aggregateSourceLineAmountsByDescription(List<ChargeLineItem> sourceLines) {
        return aggregateByDescription(sourceLines, ChargeLineCopyService::sourceLineAmount);
    }

    private static Map<String, BigDecimal> aggregateByDescription(
            List<ChargeLineItem> sourceLines,
            java.util.function.Function<ChargeLineItem, BigDecimal> extractor) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        if (sourceLines == null) {
            return values;
        }
        for (ChargeLineItem line : sourceLines) {
            if (line == null) {
                continue;
            }
            String key = normalizeDescription(line.getServiceCodeDescription());
            if (!key.isEmpty()) {
                values.merge(key, extractor.apply(line), BigDecimal::add);
            }
        }
        return values;
    }

    private static Map<String, BigDecimal> aggregateByServiceCode(
            List<ChargeLineItem> sourceLines,
            java.util.function.Function<ChargeLineItem, BigDecimal> extractor) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        if (sourceLines == null) {
            return values;
        }
        for (ChargeLineItem line : sourceLines) {
            if (line == null) {
                continue;
            }
            String key = normalizeServiceCode(line.getServiceCode());
            if (!key.isEmpty()) {
                values.merge(key, extractor.apply(line), BigDecimal::add);
            }
        }
        return values;
    }

    private static ChargeLineItem findSourceLine(
            Map<String, ChargeLineItem> byDescription,
            Map<String, ChargeLineItem> byServiceCode,
            ServiceChargeResponse targetServiceCharge) {
        ChargeLineItem byDescriptionMatch = findByDescription(byDescription, targetServiceCharge);
        if (byDescriptionMatch != null) {
            return byDescriptionMatch;
        }
        if (byServiceCode == null || targetServiceCharge == null) {
            return null;
        }
        String serviceCodeKey = normalizeServiceCode(targetServiceCharge.getServiceCode());
        return serviceCodeKey.isEmpty() ? null : byServiceCode.get(serviceCodeKey);
    }

    private static ChargeLineItem findByDescription(
            Map<String, ChargeLineItem> indexed,
            ServiceChargeResponse targetServiceCharge) {
        if (indexed == null || targetServiceCharge == null) {
            return null;
        }
        String key = normalizeDescription(targetServiceCharge.getDescription());
        return key.isEmpty() ? null : indexed.get(key);
    }

    private static BigDecimal resolveMappedAmount(
            ServiceChargeResponse targetServiceCharge,
            Map<String, BigDecimal> sourceValuesByDescription,
            Map<String, BigDecimal> sourceValuesByServiceCode,
            List<ServiceChargeDescriptionMappingDto> mappings) {
        if (targetServiceCharge == null) {
            return BigDecimal.ZERO;
        }
        String targetDescriptionKey = normalizeDescription(targetServiceCharge.getDescription());
        String targetServiceCodeKey = normalizeServiceCode(targetServiceCharge.getServiceCode());
        BigDecimal total = BigDecimal.ZERO;
        boolean matched = false;
        for (ServiceChargeDescriptionMappingDto mapping : mappings) {
            if (mapping == null || !mappingMatchesTarget(mapping, targetDescriptionKey, targetServiceCodeKey)) {
                continue;
            }
            matched = true;
            total = total.add(lookupMappedSourceValue(mapping, sourceValuesByDescription, sourceValuesByServiceCode));
        }
        return matched ? total : BigDecimal.ZERO;
    }

    private static boolean mappingMatchesTarget(
            ServiceChargeDescriptionMappingDto mapping,
            String targetDescriptionKey,
            String targetServiceCodeKey) {
        String mappedDescription = normalizeDescription(mapping.toDescription());
        if (!targetDescriptionKey.isEmpty() && targetDescriptionKey.equals(mappedDescription)) {
            return true;
        }
        String mappedServiceCode = normalizeServiceCode(mapping.toCode());
        return !targetServiceCodeKey.isEmpty()
                && !mappedServiceCode.isEmpty()
                && targetServiceCodeKey.equals(mappedServiceCode);
    }

    private static BigDecimal lookupMappedSourceValue(
            ServiceChargeDescriptionMappingDto mapping,
            Map<String, BigDecimal> sourceValuesByDescription,
            Map<String, BigDecimal> sourceValuesByServiceCode) {
        BigDecimal fromDescription = sourceValuesByDescription.getOrDefault(
                normalizeDescription(mapping.fromDescription()), BigDecimal.ZERO);
        if (fromDescription.compareTo(BigDecimal.ZERO) != 0) {
            return fromDescription;
        }
        return sourceValuesByServiceCode.getOrDefault(
                normalizeServiceCode(mapping.fromCode()), BigDecimal.ZERO);
    }

    static BigDecimal sourceLineAmount(ChargeLineItem line) {
        return line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
    }

    static BigDecimal effectiveAmount(ChargeLineItem line) {
        BigDecimal contractedRate = line.getContractedRate();
        if (contractedRate == null || contractedRate.compareTo(BigDecimal.ZERO) == 0) {
            return sourceLineAmount(line);
        }
        return contractedRate;
    }

    static String normalizeDescription(String description) {
        return description == null ? "" : description.strip().toLowerCase(Locale.ROOT);
    }

    static String normalizeServiceCode(String serviceCode) {
        return serviceCode == null ? "" : serviceCode.strip().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private static boolean isEcsOrEc3(String normalizedChargeCode) {
        return "ECS".equals(normalizedChargeCode) || "EC3".equals(normalizedChargeCode);
    }

    private static String normalizeChargeCode(String chargeCode) {
        return chargeCode == null ? "" : chargeCode.strip().toUpperCase(Locale.ROOT);
    }

    private static String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? DEFAULT_CURRENCY : currency.strip();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.strip();
    }
}
