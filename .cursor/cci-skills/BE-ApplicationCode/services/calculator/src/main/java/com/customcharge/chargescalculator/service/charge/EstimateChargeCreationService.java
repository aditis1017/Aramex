package com.customcharge.chargescalculator.service.charge;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.constants.ChargeCreationConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.service.batch.RecordProcessingLookups;
import com.customcharge.chargescalculator.service.batch.RowChargeWriteBuffer;
import com.customcharge.chargescalculator.service.calculation.TaxValueAmountCalculator;
import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EstimateChargeCreationService {

    private final ChargeLineSupport chargeLineSupport;
    private final ChargeRepository chargeRepository;
    private final ChargeLineItemRepository chargeLineItemRepository;
    private final RecordProcessingLookups recordProcessingLookups;
    private final DtaContextSupport dtaContextSupport;
    private final ChargeLineCopySupport chargeLineCopySupport;
    private final RowChargeWriteBuffer rowChargeWriteBuffer;

    public EstimateChargeCreationService(
            ChargeLineSupport chargeLineSupport,
            ChargeRepository chargeRepository,
            ChargeLineItemRepository chargeLineItemRepository,
            RecordProcessingLookups recordProcessingLookups,
            DtaContextSupport dtaContextSupport,
            ChargeLineCopySupport chargeLineCopySupport,
            RowChargeWriteBuffer rowChargeWriteBuffer) {
        this.chargeLineSupport = chargeLineSupport;
        this.chargeRepository = chargeRepository;
        this.chargeLineItemRepository = chargeLineItemRepository;
        this.recordProcessingLookups = recordProcessingLookups;
        this.dtaContextSupport = dtaContextSupport;
        this.chargeLineCopySupport = chargeLineCopySupport;
        this.rowChargeWriteBuffer = rowChargeWriteBuffer;
    }

    @Transactional
    public void createOrUpdateEstimateCharge(ChargeCalculationContext ctx, String chargeCode) {
        createOrUpdateEstimateCharge(
                ctx,
                chargeCode,
                chargeLineSupport.resolveDomDestinationChargeEntityCode(ctx.getShipment(), chargeCode));
    }

    @Transactional
    public void createOrUpdateEstimateCharge(
            ChargeCalculationContext ctx,
            String chargeCode,
            String billingEntityCode) {
        Shipment shipment = ctx.getShipment();
        String entityCode = billingEntityCode == null ? "" : billingEntityCode.strip();
        if (entityCode.isBlank()) {
            throw new ValidationException(ChargeCreationConstants.MISSING_ENTITY_CODE);
        }

        if (chargeLineCopySupport.isCopyDerivedCharge(chargeCode)) {
            createOrUpdateEstimateChargeFromCopy(ctx, chargeCode, entityCode);
            return;
        }

        List<Account> accounts = chargeLineSupport.resolveAccounts(shipment, entityCode, chargeCode);
        if (accounts.isEmpty()) {
            throw new ValidationException(ChargeCreationConstants.NO_ACCOUNTS_FOR_CHARGE + ": " + chargeCode);
        }

        com.customcharge.persistencelayer.entities.Entity billingEntity = recordProcessingLookups.findEntity(entityCode)
                .orElseThrow(() -> new ValidationException(
                        ChargeCreationConstants.ENTITY_MASTER_NOT_FOUND + ": " + entityCode));

        UUID fileReferenceId = ChargeLineSupport.parseOptionalFileReference(ctx.getRawUploadFileId());

        Charge charge = recordProcessingLookups.findCharge(shipment.getId(), chargeCode)
                .orElse(null);
        if (charge == null) {
            charge = new Charge();
            charge.setShipment(shipment);
            charge.setChargeCode(chargeCode);
            charge.setEntity(billingEntity);
            charge = rowChargeWriteBuffer.persistNewCharge(charge);
            recordProcessingLookups.registerCharge(charge);
        } else {
            charge.setEntity(billingEntity);
        }
        chargeLineSupport.applyChargeCurrency(
                charge,
                chargeCode,
                entityCode,
                shipment,
                true);

        List<ChargeLineItem> existingLines = recordProcessingLookups.findLineItems(charge.getId());
        Map<LineKey, ChargeLineItem> existingByKey = existingLines.stream()
                .collect(Collectors.toMap(EstimateChargeCreationService::lineKey, line -> line, (a, b) -> a));

        Set<LineKey> desiredKeys = new HashSet<>();
        Set<String> revenueServiceCodes = new HashSet<>();
        List<ChargeLineItem> linesToSave = new ArrayList<>();
        Map<String, String> source = ctx.getSourceRecord();
        String paymentReference = ChargeLineSupport.sourceValue(source, BatchProcessingConstants.PAYMENT_REFERENCE);
        DtaContextSupport.DtaLineContext dta =
                dtaContextSupport.forCharge(ctx, shipment, chargeCode, entityCode, chargeLineSupport);
        boolean zeroAmounts = ChargeLineSupport.requiresZeroAmounts(shipment);

        for (Account account : accounts) {
            List<ServiceChargeResponse> serviceCharges =
                    chargeLineSupport.resolveServiceCharges(entityCode, account, chargeCode);
            for (ServiceChargeResponse sc : serviceCharges) {
                collectRevenueServiceCode(sc, revenueServiceCodes);
                LineKey key = lineKey(account, sc);
                desiredKeys.add(key);
                linesToSave.add(buildEstimateLineItem(
                        existingByKey, key, charge, account, sc, chargeCode, source, ctx,
                        zeroAmounts, dta, fileReferenceId, paymentReference));
            }
        }

        List<ChargeLineItem> orphans = existingLines.stream()
                .filter(line -> !desiredKeys.contains(lineKey(line)))
                .toList();
        if (!orphans.isEmpty()) {
            rowChargeWriteBuffer.enqueueLineDeletes(orphans);
        }
        rowChargeWriteBuffer.enqueueLineSaves(linesToSave);
        dta.persist(ctx);

        List<ChargeLineItem> allLines = mergeSavedLines(existingLines, linesToSave, orphans);
        recordProcessingLookups.cacheLineItems(charge.getId(), allLines);
        if (zeroAmounts) {
            charge.setEstimateAmount(BigDecimal.ZERO);
            charge.setActualAmount(BigDecimal.ZERO);
            charge.setOutstandingAmount(BigDecimal.ZERO);
        } else {
            chargeLineSupport.applyChargeHeaderAmounts(charge, allLines);
        }
        charge.setStatus(ChargeLineSupport.resolveChargeStatusForEstimate(shipment));
        rowChargeWriteBuffer.enqueueChargeUpdate(charge);
    }

    private void createOrUpdateEstimateChargeFromCopy(
            ChargeCalculationContext ctx,
            String chargeCode,
            String entityCode) {
        Shipment shipment = ctx.getShipment();

        com.customcharge.persistencelayer.entities.Entity billingEntity = recordProcessingLookups.findEntity(entityCode)
                .orElseThrow(() -> new ValidationException(
                        ChargeCreationConstants.ENTITY_MASTER_NOT_FOUND + ": " + entityCode));

        Charge charge = recordProcessingLookups.findCharge(shipment.getId(), chargeCode)
                .orElse(null);
        if (charge == null) {
            charge = new Charge();
            charge.setShipment(shipment);
            charge.setChargeCode(chargeCode);
            charge.setEntity(billingEntity);
            charge = rowChargeWriteBuffer.persistNewCharge(charge);
            recordProcessingLookups.registerCharge(charge);
        } else {
            charge.setEntity(billingEntity);
        }
        chargeLineSupport.applyChargeCurrency(
                charge, chargeCode, entityCode, shipment, true);

        List<ChargeLineItem> existingLines = recordProcessingLookups.findLineItems(charge.getId());
        List<Account> accounts = BatchProcessingConstants.CHARGE_CODE_DTC.equalsIgnoreCase(chargeCode)
                ? chargeLineSupport.resolveAccountsForDtcProcessing(shipment, entityCode, existingLines)
                : chargeLineSupport.resolveAccounts(shipment, entityCode, chargeCode);
        if (accounts.isEmpty()) {
            throw new ValidationException(ChargeCreationConstants.NO_ACCOUNTS_FOR_CHARGE + ": " + chargeCode);
        }

        UUID fileReferenceId = ChargeLineSupport.parseOptionalFileReference(ctx.getRawUploadFileId());
        String paymentReference = ChargeLineSupport.sourceValue(ctx.getSourceRecord(), BatchProcessingConstants.PAYMENT_REFERENCE);
        boolean zeroAmounts = ChargeLineSupport.requiresZeroAmounts(shipment);

        Map<LineKey, ChargeLineItem> existingByKey = existingLines.stream()
                .collect(Collectors.toMap(EstimateChargeCreationService::lineKey, line -> line, (a, b) -> a));

        List<ChargeLineItem> copiedLines = chargeLineCopySupport.copyToTargetCharge(
                ctx, shipment, chargeCode, entityCode, charge, accounts, fileReferenceId, paymentReference);

        Set<LineKey> desiredKeys = new HashSet<>();
        List<ChargeLineItem> linesToSave = new ArrayList<>();
        for (ChargeLineItem copied : copiedLines) {
            LineKey key = lineKey(copied);
            desiredKeys.add(key);
            ChargeLineItem line = existingByKey.get(key);
            if (line == null) {
                line = copied;
            } else {
                mergeCopiedLine(line, copied);
            }
            linesToSave.add(line);
        }

        List<ChargeLineItem> orphans = existingLines.stream()
                .filter(line -> !desiredKeys.contains(lineKey(line)))
                .toList();
        if (!orphans.isEmpty()) {
            rowChargeWriteBuffer.enqueueLineDeletes(orphans);
        }
        rowChargeWriteBuffer.enqueueLineSaves(linesToSave);

        List<ChargeLineItem> allLines = mergeSavedLines(existingLines, linesToSave, orphans);
        recordProcessingLookups.cacheLineItems(charge.getId(), allLines);
        if (zeroAmounts) {
            charge.setEstimateAmount(BigDecimal.ZERO);
            charge.setActualAmount(BigDecimal.ZERO);
            charge.setOutstandingAmount(BigDecimal.ZERO);
        } else {
            chargeLineSupport.applyChargeHeaderAmounts(charge, allLines);
        }
        charge.setStatus(ChargeLineSupport.resolveChargeStatusForEstimate(shipment));
        rowChargeWriteBuffer.enqueueChargeUpdate(charge);
    }

    static void mergeCopiedLine(ChargeLineItem target, ChargeLineItem source) {
        target.setAmount(source.getAmount());
        if (hasExplicitContractedRate(source)) {
            target.setContractedRate(source.getContractedRate());
        }
        target.setPreTaxableAmount(source.getPreTaxableAmount());
        target.setPreConvertedAmount(source.getPreConvertedAmount());
        target.setExchangeRate(source.getExchangeRate());
        target.setServiceCodeDescription(source.getServiceCodeDescription());
        target.setInfoAxsId(source.getInfoAxsId());
        target.setFileReferenceId(source.getFileReferenceId());
        target.setPaymentReference(source.getPaymentReference());
        target.setApAccountNumber(source.getApAccountNumber());
        target.setDeclarationNumber(source.getDeclarationNumber());
        target.setConsolGroup(source.getConsolGroup());
        target.setPayingEntity(source.getPayingEntity());
    }

    private static boolean hasExplicitContractedRate(ChargeLineItem line) {
        return line != null
                && line.getContractedRate() != null
                && line.getContractedRate().compareTo(BigDecimal.ZERO) != 0;
    }

    private static BigDecimal resolveEstimateTotal(
            List<ChargeLineItem> allLines,
            Set<String> revenueServiceCodes,
            boolean calculatorHelperEnabled) {
        if (calculatorHelperEnabled) {
            return ChargeLineCopyService.sumLineTotals(allLines);
        }
        BigDecimal calculatedTotal = BigDecimal.ZERO;
        BigDecimal revenueTotal = BigDecimal.ZERO;
        for (ChargeLineItem line : allLines) {
            BigDecimal amount = line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
            if (revenueServiceCodes.contains(normalizeServiceCode(line.getServiceCode()))) {
                revenueTotal = revenueTotal.add(amount);
            } else {
                calculatedTotal = calculatedTotal.add(amount);
            }
        }
        return calculatedTotal.add(revenueTotal);
    }

    private static List<ChargeLineItem> mergeSavedLines(
            List<ChargeLineItem> existingLines,
            List<ChargeLineItem> linesToSave,
            List<ChargeLineItem> orphans) {
        Set<LineKey> orphanKeys = orphans.stream()
                .map(EstimateChargeCreationService::lineKey)
                .collect(Collectors.toSet());
        Map<LineKey, ChargeLineItem> merged = existingLines.stream()
                .filter(line -> !orphanKeys.contains(lineKey(line)))
                .collect(Collectors.toMap(EstimateChargeCreationService::lineKey, line -> line, (a, b) -> a));
        for (ChargeLineItem line : linesToSave) {
            merged.put(lineKey(line), line);
        }
        return new ArrayList<>(merged.values());
    }

    private record LineKey(UUID accountId, String serviceCode) {
    }

    private ChargeLineItem buildEstimateLineItem(
            Map<LineKey, ChargeLineItem> existingByKey,
            LineKey key,
            Charge charge,
            Account account,
            ServiceChargeResponse sc,
            String chargeCode,
            Map<String, String> source,
            ChargeCalculationContext ctx,
            boolean zeroAmounts,
            DtaContextSupport.DtaLineContext dta,
            UUID fileReferenceId,
            String paymentReference) {
        ChargeLineItem line = existingByKey.get(key);
        if (line == null) {
            line = new ChargeLineItem();
            line.setCharge(charge);
            line.setAccount(account);
            line.setServiceCode(sc.getServiceCode());
            line.setServiceCodeDescription(sc.getDescription());
            line.setContractedRate(BigDecimal.ZERO);
            ChargeLineSupport.applyApAccountNumber(line, sc);
        }

        boolean fromDtaContext = usesDtaContextAmount(chargeCode, dta);
        boolean siblingChargeContext = dta.siblingChargeContext();
        BigDecimal taxPercent = sc.getTaxValue() != null
                ? BigDecimal.valueOf(sc.getTaxValue())
                : BigDecimal.ZERO;
        BigDecimal preFxAmount = BigDecimal.ZERO;
        if (!zeroAmounts) {
            preFxAmount = ChargeLineSupport.resolveLineAmount(
                    chargeCode, source, sc, true, dta.amountsByDescription());
        }

        CurrencyConversionResult exchange = zeroAmounts
                ? new CurrencyConversionResult(0.0, null)
                : chargeLineSupport.convertEstimateLineAmount(
                        chargeCode,
                        preFxAmount,
                        null,
                        source,
                        ctx.getShipment(),
                        sc,
                        charge,
                        fromDtaContext,
                        siblingChargeContext,
                        taxPercent,
                        ChargeLineSupport.exchangeRateKeyPrefix(ctx));

        BigDecimal preTaxableAmount = exchange.convertedAmount() == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(exchange.convertedAmount());
        if (shouldSkipTaxOnEstimateLine(chargeCode, fromDtaContext, siblingChargeContext)) {
            chargeLineSupport.applyContextDerivedLineAmounts(line, preTaxableAmount, charge);
        } else if (useContextDerivedAmount(chargeCode, fromDtaContext, siblingChargeContext)) {
            chargeLineSupport.applyContextDerivedLineAmounts(line, preTaxableAmount, charge);
        } else {
            chargeLineSupport.applyTaxedLineAmounts(line, preTaxableAmount, taxPercent, charge);
        }
        if (!zeroAmounts && dta.rawCapture() != null) {
            dta.mergeCapture(sc.getDescription(), taxedAmount(line));
        }
        line.setExchangeRate(exchange.exchangeRate());
        line.setFileReferenceId(fileReferenceId);
        line.setPaymentReference(paymentReference);
        line.setInfoAxsId(sc.getInfoAxsId());
        return line;
    }

    private static BigDecimal taxedAmount(ChargeLineItem line) {
        return line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
    }

    /**
     * Estimate file upload for ECD: file column values are final amounts (FX only, no tax).
     * Rate-sheet paths (shipment service or future calculator rate-sheet breakdown) still apply tax.
     */
    private static boolean shouldSkipTaxOnEstimateLine(
            String chargeCode,
            boolean fromDtaContext,
            boolean siblingChargeContext) {
        if (fromDtaContext || siblingChargeContext) {
            return false;
        }
        String normalized = chargeCode == null ? "" : chargeCode.strip().toUpperCase(Locale.ROOT);
        return BatchProcessingConstants.CHARGE_CODE_ECD.equals(normalized);
    }

    private static boolean useContextDerivedAmount(
            String chargeCode,
            boolean fromDtaContext,
            boolean siblingChargeContext) {
        if (!fromDtaContext) {
            return false;
        }
        if (siblingChargeContext) {
            return true;
        }
        String normalized = chargeCode == null ? "" : chargeCode.strip().toUpperCase(Locale.ROOT);
        return BatchProcessingConstants.CHARGE_CODE_ECS.equals(normalized)
                || BatchProcessingConstants.CHARGE_CODE_EC3.equals(normalized);
    }

    private static boolean usesDtaContextAmount(String chargeCode, DtaContextSupport.DtaLineContext dta) {
        if (!dta.useDtaAmounts()) {
            return false;
        }
        String normalized = chargeCode == null ? "" : chargeCode.strip().toUpperCase(Locale.ROOT);
        if (ChargeLineSupport.usesDtaRevenueContextFallback(normalized)) {
            return true;
        }
        return ChargeLineSupport.usesDtsStyleLineAmountResolution(normalized);
    }

    private static LineKey lineKey(Account account, ServiceChargeResponse sc) {
        return new LineKey(account.getId(), normalizeServiceCode(sc.getServiceCode()));
    }

    private static LineKey lineKey(ChargeLineItem line) {
        UUID accountId = line.getAccount() != null ? line.getAccount().getId() : null;
        return new LineKey(accountId, normalizeServiceCode(line.getServiceCode()));
    }

    private static String normalizeServiceCode(String serviceCode) {
        return serviceCode == null ? "" : serviceCode.strip();
    }

    private static void collectRevenueServiceCode(ServiceChargeResponse sc, Set<String> revenueServiceCodes) {
        if (ChargeLineSupport.isRevenueServiceLine(sc)) {
            revenueServiceCodes.add(normalizeServiceCode(sc.getServiceCode()));
        }
    }
}
