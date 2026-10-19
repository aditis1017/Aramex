package com.customcharge.chargescalculator.service.charge;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.service.batch.RecordProcessingLookups;
import com.customcharge.chargescalculator.service.batch.RowChargeWriteBuffer;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService.CopyAmountFxResult;
import com.customcharge.persistencelayer.dto.ServiceChargeDescriptionMappingDto;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * After ECD actual file upload, reconciles downstream charge lines against active service codes
 * (remove inactive, create newly active) and propagates amounts from ECD through the copy chain.
 * ECD charge lines are not reconciled here.
 */
@Service
public class ActualChargeAmountPropagationService {

    private final ChargeRepository chargeRepository;
    private final ChargeLineItemRepository chargeLineItemRepository;
    private final RecordProcessingLookups recordProcessingLookups;
    private final ChargeLineSupport chargeLineSupport;
    private final ServiceChargeMappingRepository serviceChargeMappingRepository;
    private final ChargeLineCopyService chargeLineCopyService;
    private final RowChargeWriteBuffer rowChargeWriteBuffer;

    public ActualChargeAmountPropagationService(
            ChargeRepository chargeRepository,
            ChargeLineItemRepository chargeLineItemRepository,
            RecordProcessingLookups recordProcessingLookups,
            ChargeLineSupport chargeLineSupport,
            ServiceChargeMappingRepository serviceChargeMappingRepository,
            ChargeLineCopyService chargeLineCopyService,
            RowChargeWriteBuffer rowChargeWriteBuffer) {
        this.chargeRepository = chargeRepository;
        this.chargeLineItemRepository = chargeLineItemRepository;
        this.recordProcessingLookups = recordProcessingLookups;
        this.chargeLineSupport = chargeLineSupport;
        this.serviceChargeMappingRepository = serviceChargeMappingRepository;
        this.chargeLineCopyService = chargeLineCopyService;
        this.rowChargeWriteBuffer = rowChargeWriteBuffer;
    }

    @Transactional
    public void propagateAfterEcdActualUpload(ChargeCalculationContext ctx) {
        Shipment shipment = ctx.getShipment();
        String shipmentType = blankToEmpty(shipment.getShipmentType());
        if (BatchProcessingConstants.SHIPMENT_TYPE_DDU.equalsIgnoreCase(shipmentType)
                || BatchProcessingConstants.SHIPMENT_TYPE_DDO.equalsIgnoreCase(shipmentType)) {
            propagateEcdToRevenue(ctx, BatchProcessingConstants.CHARGE_CODE_DTC);
            return;
        }
        if (BatchProcessingConstants.SHIPMENT_TYPE_DDP.equalsIgnoreCase(shipmentType)) {
            propagateEcdToRevenue(ctx, BatchProcessingConstants.CHARGE_CODE_DTA);
            propagateDtaToCost(
                    ctx,
                    BatchProcessingConstants.CHARGE_CODE_ECS,
                    BatchProcessingConstants.PAYMENT_TYPE_PREPAID);
            propagateDtaToCost(
                    ctx,
                    BatchProcessingConstants.CHARGE_CODE_EC3,
                    BatchProcessingConstants.PAYMENT_TYPE_THIRD_PARTY);
            propagateCostToRevenue(
                    ctx,
                    BatchProcessingConstants.CHARGE_CODE_ECS,
                    BatchProcessingConstants.CHARGE_CODE_DTS,
                    BatchProcessingConstants.PAYMENT_TYPE_PREPAID);
            propagateCostToRevenue(
                    ctx,
                    BatchProcessingConstants.CHARGE_CODE_EC3,
                    BatchProcessingConstants.CHARGE_CODE_DT3,
                    BatchProcessingConstants.PAYMENT_TYPE_THIRD_PARTY);
        }
    }

    private void propagateEcdToRevenue(ChargeCalculationContext ctx, String revenueChargeCode) {
        Shipment shipment = ctx.getShipment();
        Optional<Charge> ecdCharge = recordProcessingLookups.findCharge(
                shipment.getId(), BatchProcessingConstants.CHARGE_CODE_ECD);
        Optional<Charge> revenueCharge = recordProcessingLookups.findCharge(
                shipment.getId(), revenueChargeCode);
        if (ecdCharge.isEmpty() || revenueCharge.isEmpty()) {
            return;
        }

        String entityCode = chargeLineSupport.resolveDomDestinationChargeEntityCode(shipment, revenueChargeCode);
        List<ChargeLineItem> existingLines = recordProcessingLookups.findLineItems(revenueCharge.get().getId());
        List<Account> accounts = BatchProcessingConstants.CHARGE_CODE_DTC.equalsIgnoreCase(revenueChargeCode)
                ? chargeLineSupport.resolveAccountsForDtcProcessing(shipment, entityCode, existingLines)
                : chargeLineSupport.resolveAccounts(shipment, entityCode, revenueChargeCode);
        if (accounts.isEmpty()) {
            return;
        }

        List<ChargeLineItem> ecdActualLines = actualLines(
                recordProcessingLookups.findLineItems(ecdCharge.get().getId()));
        Map<String, BigDecimal> amountsByDescription = aggregateByDescription(
                ecdActualLines, ActualChargeAmountPropagationService::sourceLineAmount);
        Map<String, BigDecimal> amountsByServiceCode = aggregateByServiceCode(
                ecdActualLines, ActualChargeAmountPropagationService::sourceLineAmount);

        Map<LineKey, ChargeLineItem> existingByKey = indexExistingLines(existingLines);
        Set<LineKey> desiredKeys = new HashSet<>();
        List<ChargeLineItem> linesToSave = new ArrayList<>();
        boolean dtcRevenueCharge = BatchProcessingConstants.CHARGE_CODE_DTC.equalsIgnoreCase(revenueChargeCode);

        for (Account account : accounts) {
            for (ServiceChargeResponse serviceCharge :
                    chargeLineSupport.resolveServiceCharges(entityCode, account, revenueChargeCode)) {
                LineKey key = lineKey(account, serviceCharge.getServiceCode());
                desiredKeys.add(key);
                ChargeLineItem line = existingByKey.get(key);
                if (line == null) {
                    Account billingAccount = dtcRevenueCharge
                            ? chargeLineSupport.resolveDtcBillingAccount(shipment, entityCode).orElse(account)
                            : account;
                    line = newLine(revenueCharge.get(), billingAccount, serviceCharge);
                }
                BigDecimal sourceAmount = resolveDirectSourceValue(
                        normalizeDescription(serviceCharge.getDescription()),
                        normalizeServiceCode(serviceCharge.getServiceCode()),
                        amountsByDescription,
                        amountsByServiceCode);
                applyFxAmount(
                        ctx,
                        shipment,
                        ecdCharge.get(),
                        revenueCharge.get(),
                        BatchProcessingConstants.CHARGE_CODE_ECD,
                        revenueChargeCode,
                        entityCode,
                        entityCode,
                        line,
                        sourceAmount,
                        false);
                linesToSave.add(line);
            }
        }

        deleteOrphans(existingLines, desiredKeys);
        if (!linesToSave.isEmpty()) {
            rowChargeWriteBuffer.enqueueLineSaves(linesToSave);
        }
        recordProcessingLookups.cacheLineItems(
                revenueCharge.get().getId(),
                mergeSavedLines(existingLines, linesToSave, desiredKeys));
        recalculateChargeHeader(revenueCharge.get());
    }

    private void propagateDtaToCost(
            ChargeCalculationContext ctx,
            String targetChargeCode,
            String expectedPaymentType) {
        Shipment shipment = ctx.getShipment();
        if (!chargeLineSupport.matchesPaymentType(shipment, expectedPaymentType)) {
            return;
        }

        Optional<Charge> dtaCharge = recordProcessingLookups.findCharge(
                shipment.getId(), BatchProcessingConstants.CHARGE_CODE_DTA);
        Optional<Charge> targetCharge = recordProcessingLookups.findCharge(
                shipment.getId(), targetChargeCode);
        if (dtaCharge.isEmpty() || targetCharge.isEmpty()) {
            return;
        }

        String sourceEntityCode = chargeLineSupport.resolveDestinationEntityCode(shipment);
        String targetEntityCode = resolveCostEntityCode(shipment, targetChargeCode);
        List<Account> accounts = chargeLineSupport.resolveAccounts(shipment, targetEntityCode, targetChargeCode);
        if (accounts.isEmpty()) {
            return;
        }

        List<ChargeLineItem> dtaLines = recordProcessingLookups.findLineItems(dtaCharge.get().getId());
        Map<String, BigDecimal> effectiveByDescription = aggregateByDescription(
                dtaLines, ActualChargeAmountPropagationService::effectiveAmount);
        Map<String, BigDecimal> effectiveByServiceCode = aggregateByServiceCode(
                dtaLines, ActualChargeAmountPropagationService::effectiveAmount);
        List<ServiceChargeDescriptionMappingDto> mappings = resolveMappings(
                sourceEntityCode, targetEntityCode, targetChargeCode);

        List<ChargeLineItem> existingLines = recordProcessingLookups.findLineItems(targetCharge.get().getId());
        Map<LineKey, ChargeLineItem> existingByKey = indexExistingLines(existingLines);
        Set<LineKey> desiredKeys = new HashSet<>();
        List<ChargeLineItem> linesToSave = new ArrayList<>();

        for (Account account : accounts) {
            for (ServiceChargeResponse serviceCharge :
                    chargeLineSupport.resolveServiceCharges(targetEntityCode, account, targetChargeCode)) {
                LineKey key = lineKey(account, serviceCharge.getServiceCode());
                desiredKeys.add(key);
                ChargeLineItem line = existingByKey.get(key);
                if (line == null) {
                    line = newLine(targetCharge.get(), account, serviceCharge);
                }
                BigDecimal sourceAmount = resolveMappedAmount(
                        serviceCharge.getDescription(),
                        serviceCharge.getServiceCode(),
                        effectiveByDescription,
                        effectiveByServiceCode,
                        mappings);
                applyFxAmount(
                        ctx,
                        shipment,
                        dtaCharge.get(),
                        targetCharge.get(),
                        BatchProcessingConstants.CHARGE_CODE_DTA,
                        targetChargeCode,
                        sourceEntityCode,
                        targetEntityCode,
                        line,
                        sourceAmount,
                        false);
                linesToSave.add(line);
            }
        }

        deleteOrphans(existingLines, desiredKeys);
        if (!linesToSave.isEmpty()) {
            rowChargeWriteBuffer.enqueueLineSaves(linesToSave);
        }
        recordProcessingLookups.cacheLineItems(
                targetCharge.get().getId(),
                mergeSavedLines(existingLines, linesToSave, desiredKeys));
        recalculateChargeHeader(targetCharge.get());
    }

    private void propagateCostToRevenue(
            ChargeCalculationContext ctx,
            String sourceChargeCode,
            String targetChargeCode,
            String expectedPaymentType) {
        Shipment shipment = ctx.getShipment();
        if (!chargeLineSupport.matchesPaymentType(shipment, expectedPaymentType)) {
            return;
        }

        Optional<Charge> sourceCharge = recordProcessingLookups.findCharge(shipment.getId(), sourceChargeCode);
        Optional<Charge> targetCharge = recordProcessingLookups.findCharge(shipment.getId(), targetChargeCode);
        if (sourceCharge.isEmpty() || targetCharge.isEmpty()) {
            return;
        }

        String sourceEntityCode = resolveCostEntityCode(shipment, sourceChargeCode);
        String targetEntityCode = resolveCostEntityCode(shipment, targetChargeCode);
        List<Account> accounts = chargeLineSupport.resolveAccounts(shipment, targetEntityCode, targetChargeCode);
        if (accounts.isEmpty()) {
            return;
        }

        List<ChargeLineItem> sourceLines = recordProcessingLookups.findLineItems(sourceCharge.get().getId());
        Map<String, BigDecimal> amountsByDescription = aggregateByDescription(
                sourceLines,
                ActualChargeAmountPropagationService::sourceLineAmount);
        Map<String, BigDecimal> amountsByServiceCode = aggregateByServiceCode(
                sourceLines,
                ActualChargeAmountPropagationService::sourceLineAmount);

        List<ChargeLineItem> existingLines = recordProcessingLookups.findLineItems(targetCharge.get().getId());
        Map<LineKey, ChargeLineItem> existingByKey = indexExistingLines(existingLines);
        Set<LineKey> desiredKeys = new HashSet<>();
        List<ChargeLineItem> linesToSave = new ArrayList<>();

        for (Account account : accounts) {
            for (ServiceChargeResponse serviceCharge :
                    chargeLineSupport.resolveServiceCharges(targetEntityCode, account, targetChargeCode)) {
                LineKey key = lineKey(account, serviceCharge.getServiceCode());
                desiredKeys.add(key);
                ChargeLineItem line = existingByKey.get(key);
                if (line == null) {
                    line = newLine(targetCharge.get(), account, serviceCharge);
                }
                BigDecimal sourceAmount = resolveDirectSourceValue(
                        normalizeDescription(serviceCharge.getDescription()),
                        normalizeServiceCode(serviceCharge.getServiceCode()),
                        amountsByDescription,
                        amountsByServiceCode);
                applyFxAmount(
                        ctx,
                        shipment,
                        sourceCharge.get(),
                        targetCharge.get(),
                        sourceChargeCode,
                        targetChargeCode,
                        sourceEntityCode,
                        targetEntityCode,
                        line,
                        sourceAmount,
                        true);
                linesToSave.add(line);
            }
        }

        deleteOrphans(existingLines, desiredKeys);
        if (!linesToSave.isEmpty()) {
            rowChargeWriteBuffer.enqueueLineSaves(linesToSave);
        }
        recordProcessingLookups.cacheLineItems(
                targetCharge.get().getId(),
                mergeSavedLines(existingLines, linesToSave, desiredKeys));
        recalculateChargeHeader(targetCharge.get());
    }

    private static ChargeLineItem newLine(Charge charge, Account account, ServiceChargeResponse serviceCharge) {
        ChargeLineItem line = new ChargeLineItem();
        line.setCharge(charge);
        line.setAccount(account);
        line.setServiceCode(serviceCharge.getServiceCode());
        line.setServiceCodeDescription(serviceCharge.getDescription());
        line.setInfoAxsId(serviceCharge.getInfoAxsId());
        line.setContractedRate(BigDecimal.ZERO);
        ChargeLineSupport.applyApAccountNumber(line, serviceCharge);
        return line;
    }

    private static Map<LineKey, ChargeLineItem> indexExistingLines(List<ChargeLineItem> existingLines) {
        if (existingLines == null || existingLines.isEmpty()) {
            return Map.of();
        }
        return existingLines.stream()
                .filter(line -> line != null)
                .collect(Collectors.toMap(
                        ActualChargeAmountPropagationService::lineKey,
                        line -> line,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private void deleteOrphans(List<ChargeLineItem> existingLines, Set<LineKey> desiredKeys) {
        if (existingLines == null || existingLines.isEmpty()) {
            return;
        }
        List<ChargeLineItem> orphans = existingLines.stream()
                .filter(line -> line != null && !desiredKeys.contains(lineKey(line)))
                .toList();
        if (!orphans.isEmpty()) {
            rowChargeWriteBuffer.enqueueLineDeletes(orphans);
        }
    }

    private record LineKey(UUID accountId, String serviceCode) {
    }

    private static LineKey lineKey(Account account, String serviceCode) {
        UUID accountId = account != null ? account.getId() : null;
        return new LineKey(accountId, normalizeServiceCode(serviceCode));
    }

    private static LineKey lineKey(ChargeLineItem line) {
        UUID accountId = line.getAccount() != null ? line.getAccount().getId() : null;
        return new LineKey(accountId, normalizeServiceCode(line.getServiceCode()));
    }

    private void applyFxAmount(
            ChargeCalculationContext ctx,
            Shipment shipment,
            Charge sourceCharge,
            Charge targetCharge,
            String sourceChargeType,
            String targetChargeType,
            String sourceEntityCode,
            String targetEntityCode,
            ChargeLineItem line,
            BigDecimal sourceAmount,
            boolean usdSiblingConversion) {
        CopyAmountFxResult fx = chargeLineCopyService.convertCopyAmount(
                sourceAmount,
                shipment,
                sourceCharge,
                targetCharge,
                sourceChargeType,
                targetChargeType,
                sourceEntityCode,
                targetEntityCode,
                ChargeLineSupport.exchangeRateKeyPrefix(ctx),
                usdSiblingConversion);
        BigDecimal taxPercent = chargeLineSupport.resolveTaxPercentForServiceCode(
                targetEntityCode,
                line.getAccount(),
                targetChargeType,
                line.getServiceCode());
        chargeLineSupport.applyTaxedLineAmounts(line, fx.convertedAmount(), taxPercent, targetCharge);
        line.setExchangeRate(fx.exchangeRate());
    }

    private void recalculateChargeHeader(Charge charge) {
        List<ChargeLineItem> allLines = recordProcessingLookups.findLineItems(charge.getId());
        chargeLineSupport.applyChargeHeaderAmounts(charge, allLines);
        rowChargeWriteBuffer.enqueueChargeUpdate(charge);
    }

    private static List<ChargeLineItem> mergeSavedLines(
            List<ChargeLineItem> existingLines,
            List<ChargeLineItem> linesToSave,
            Set<LineKey> desiredKeys) {
        Map<LineKey, ChargeLineItem> merged = new LinkedHashMap<>(indexExistingLines(existingLines.stream()
                .filter(line -> line != null && desiredKeys.contains(lineKey(line)))
                .toList()));
        for (ChargeLineItem line : linesToSave) {
            if (line != null) {
                merged.put(lineKey(line), line);
            }
        }
        return new ArrayList<>(merged.values());
    }

    private List<ServiceChargeDescriptionMappingDto> resolveMappings(
            String sourceEntityCode,
            String targetEntityCode,
            String targetChargeType) {
        return recordProcessingLookups.descriptionMappings(
                sourceEntityCode,
                targetEntityCode,
                targetChargeType,
                () -> loadDescriptionMappings(sourceEntityCode, targetEntityCode, targetChargeType));
    }

    private List<ServiceChargeDescriptionMappingDto> loadDescriptionMappings(
            String sourceEntityCode,
            String targetEntityCode,
            String targetChargeType) {
        List<ServiceChargeDescriptionMappingDto> mappings =
                serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                        sourceEntityCode, targetEntityCode, targetChargeType);
        if (mappings.isEmpty()) {
            mappings = serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                    BatchProcessingConstants.GLOBAL_ENTITY_CODE,
                    BatchProcessingConstants.GLOBAL_ENTITY_CODE,
                    targetChargeType);
        }
        return mappings;
    }

    private String resolveCostEntityCode(Shipment shipment, String chargeCode) {
        if (BatchProcessingConstants.CHARGE_CODE_ECS.equalsIgnoreCase(chargeCode)) {
            return chargeLineSupport.resolveOriginEntityCode(shipment);
        }
        if (BatchProcessingConstants.CHARGE_CODE_EC3.equalsIgnoreCase(chargeCode)) {
            return chargeLineSupport.resolveThirdPartyEntityCode(shipment);
        }
        if (BatchProcessingConstants.CHARGE_CODE_DTS.equalsIgnoreCase(chargeCode)) {
            return chargeLineSupport.resolveOriginEntityCode(shipment);
        }
        if (BatchProcessingConstants.CHARGE_CODE_DT3.equalsIgnoreCase(chargeCode)) {
            return chargeLineSupport.resolveThirdPartyEntityCode(shipment);
        }
        return chargeLineSupport.resolveDomDestinationChargeEntityCode(shipment, chargeCode);
    }

    private static List<ChargeLineItem> actualLines(List<ChargeLineItem> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        return lines.stream().filter(ActualChargeAmountPropagationService::hasPaymentReference).toList();
    }

    private static boolean hasPaymentReference(ChargeLineItem line) {
        return line != null && StringUtils.hasText(line.getPaymentReference());
    }

    private static BigDecimal effectiveAmount(ChargeLineItem line) {
        BigDecimal contractedRate = line.getContractedRate();
        if (contractedRate == null || contractedRate.compareTo(BigDecimal.ZERO) == 0) {
            return sourceLineAmount(line);
        }
        return contractedRate;
    }

    private static BigDecimal sourceLineAmount(ChargeLineItem line) {
        return line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
    }

    private static Map<String, BigDecimal> aggregateByDescription(
            List<ChargeLineItem> lines,
            java.util.function.Function<ChargeLineItem, BigDecimal> extractor) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        if (lines == null) {
            return values;
        }
        for (ChargeLineItem line : lines) {
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
            List<ChargeLineItem> lines,
            java.util.function.Function<ChargeLineItem, BigDecimal> extractor) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        if (lines == null) {
            return values;
        }
        for (ChargeLineItem line : lines) {
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

    private static BigDecimal resolveMappedAmount(
            String targetDescription,
            String targetServiceCode,
            Map<String, BigDecimal> sourceByDescription,
            Map<String, BigDecimal> sourceByServiceCode,
            List<ServiceChargeDescriptionMappingDto> mappings) {
        String targetDescriptionKey = normalizeDescription(targetDescription);
        String targetServiceCodeKey = normalizeServiceCode(targetServiceCode);
        BigDecimal total = BigDecimal.ZERO;
        boolean matched = false;
        for (ServiceChargeDescriptionMappingDto mapping : mappings) {
            if (mapping == null || !mappingMatchesTarget(mapping, targetDescriptionKey, targetServiceCodeKey)) {
                continue;
            }
            matched = true;
            total = total.add(lookupMappedSourceValue(mapping, sourceByDescription, sourceByServiceCode));
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
            Map<String, BigDecimal> sourceByDescription,
            Map<String, BigDecimal> sourceByServiceCode) {
        BigDecimal fromDescription = sourceByDescription.getOrDefault(
                normalizeDescription(mapping.fromDescription()), BigDecimal.ZERO);
        if (fromDescription.compareTo(BigDecimal.ZERO) != 0) {
            return fromDescription;
        }
        return sourceByServiceCode.getOrDefault(
                normalizeServiceCode(mapping.fromCode()), BigDecimal.ZERO);
    }

    private static String normalizeDescription(String description) {
        return description == null ? "" : description.strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizeServiceCode(String serviceCode) {
        return serviceCode == null ? "" : serviceCode.strip().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal resolveDirectSourceValue(
            String targetDescriptionKey,
            String targetServiceCodeKey,
            Map<String, BigDecimal> sourceByDescription,
            Map<String, BigDecimal> sourceByServiceCode) {
        BigDecimal fromDescription = targetDescriptionKey.isEmpty()
                ? BigDecimal.ZERO
                : sourceByDescription.getOrDefault(targetDescriptionKey, BigDecimal.ZERO);
        if (fromDescription.compareTo(BigDecimal.ZERO) != 0) {
            return fromDescription;
        }
        return targetServiceCodeKey.isEmpty()
                ? BigDecimal.ZERO
                : sourceByServiceCode.getOrDefault(targetServiceCodeKey, BigDecimal.ZERO);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.strip();
    }
}
