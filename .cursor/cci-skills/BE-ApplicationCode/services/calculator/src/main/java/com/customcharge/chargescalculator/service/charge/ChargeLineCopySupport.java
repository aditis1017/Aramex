package com.customcharge.chargescalculator.service.charge;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.service.batch.RecordProcessingLookups;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService.ChargeLineCopyRequest;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService.CopyMatchMode;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService.CopyTaxMode;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService.DescriptionMappingsLookup;
import com.customcharge.persistencelayer.calculatorhelper.ChargeLineCopyService.ServiceChargeLookup;
import com.customcharge.persistencelayer.dto.ServiceChargeDescriptionMappingDto;
import com.customcharge.persistencelayer.dto.ServiceChargeQueryCriteria;
import com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Shipment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ChargeLineCopySupport {

    private static final Map<String, CopyPair> ESTIMATE_COPY_PAIRS = Map.of(
            BatchProcessingConstants.CHARGE_CODE_DTC,
            new CopyPair(BatchProcessingConstants.CHARGE_CODE_ECD, CopyMatchMode.DESCRIPTION, CopyTaxMode.AMOUNT_ONLY),
            BatchProcessingConstants.CHARGE_CODE_DTA,
            new CopyPair(BatchProcessingConstants.CHARGE_CODE_ECD, CopyMatchMode.DESCRIPTION, CopyTaxMode.AMOUNT_ONLY),
            BatchProcessingConstants.CHARGE_CODE_ECS,
            new CopyPair(
                    BatchProcessingConstants.CHARGE_CODE_DTA,
                    CopyMatchMode.SERVICE_CHARGE_MAPPING,
                    CopyTaxMode.EFFECTIVE_X),
            BatchProcessingConstants.CHARGE_CODE_EC3,
            new CopyPair(
                    BatchProcessingConstants.CHARGE_CODE_DTA,
                    CopyMatchMode.SERVICE_CHARGE_MAPPING,
                    CopyTaxMode.EFFECTIVE_X),
            BatchProcessingConstants.CHARGE_CODE_DTS,
            new CopyPair(BatchProcessingConstants.CHARGE_CODE_ECS, CopyMatchMode.DESCRIPTION, CopyTaxMode.AMOUNT_ONLY),
            BatchProcessingConstants.CHARGE_CODE_DT3,
            new CopyPair(BatchProcessingConstants.CHARGE_CODE_EC3, CopyMatchMode.DESCRIPTION, CopyTaxMode.AMOUNT_ONLY));

    private static final String GLOBAL_ENTITY_CODE = "Global";

    private final ChargeLineCopyService chargeLineCopyService;
    private final RecordProcessingLookups recordProcessingLookups;
    private final ChargeLineSupport chargeLineSupport;
    private final ServiceChargeMappingRepository serviceChargeMappingRepository;

    public ChargeLineCopySupport(
            ChargeLineCopyService chargeLineCopyService,
            RecordProcessingLookups recordProcessingLookups,
            ChargeLineSupport chargeLineSupport,
            ServiceChargeMappingRepository serviceChargeMappingRepository) {
        this.chargeLineCopyService = chargeLineCopyService;
        this.recordProcessingLookups = recordProcessingLookups;
        this.chargeLineSupport = chargeLineSupport;
        this.serviceChargeMappingRepository = serviceChargeMappingRepository;
    }

    public boolean isCopyDerivedCharge(String chargeCode) {
        return chargeCode != null && ESTIMATE_COPY_PAIRS.containsKey(normalize(chargeCode));
    }

    public List<ChargeLineItem> copyToTargetCharge(
            ChargeCalculationContext ctx,
            Shipment shipment,
            String targetChargeCode,
            String targetEntityCode,
            Charge targetCharge,
            List<Account> targetAccounts,
            UUID fileReferenceId,
            String paymentReference) {
        return copyToTargetCharge(
                ctx,
                shipment,
                targetChargeCode,
                targetEntityCode,
                targetCharge,
                targetAccounts,
                fileReferenceId,
                paymentReference,
                ESTIMATE_COPY_PAIRS);
    }

    private List<ChargeLineItem> copyToTargetCharge(
            ChargeCalculationContext ctx,
            Shipment shipment,
            String targetChargeCode,
            String targetEntityCode,
            Charge targetCharge,
            List<Account> targetAccounts,
            UUID fileReferenceId,
            String paymentReference,
            Map<String, CopyPair> copyPairs) {
        CopyPair pair = copyPairs.get(normalize(targetChargeCode));
        if (pair == null) {
            throw new IllegalStateException("No copy pair for charge: " + targetChargeCode);
        }
        String sourceEntityCode = resolveSourceEntityCode(shipment, pair.sourceChargeType());
        Charge sourceCharge = recordProcessingLookups.findCharge(shipment.getId(), pair.sourceChargeType())
                .orElseThrow(() -> new IllegalStateException(
                        "Source charge not found: " + pair.sourceChargeType() + " for shipment " + shipment.getId()));
        List<ChargeLineItem> sourceLines = filterSourceLinesForPaymentReference(
                recordProcessingLookups.findLineItems(sourceCharge.getId()),
                paymentReference);
        ServiceChargeLookup serviceChargeLookup = criteria -> chargeLineSupport.resolveServiceCharges(
                criteria.getEntityCode(), criteria.getAccount(), criteria.getChargeTypes().getFirst());
        DescriptionMappingsLookup descriptionMappingsLookup = (from, to, chargeType) ->
                recordProcessingLookups.descriptionMappings(
                        from,
                        to,
                        chargeType,
                        () -> loadDescriptionMappings(from, to, chargeType));
        ChargeLineCopyRequest request = new ChargeLineCopyRequest(
                pair.sourceChargeType(),
                normalize(targetChargeCode),
                shipment,
                sourceEntityCode,
                targetEntityCode,
                sourceCharge,
                targetCharge,
                targetAccounts,
                sourceLines,
                ChargeLineSupport.exchangeRateKeyPrefix(ctx),
                BatchProcessingConstants.PROCESS_FOR_ESTIMATE.equalsIgnoreCase(ctx.getProcessFor()),
                pair.matchMode(),
                fileReferenceId,
                paymentReference,
                pair.taxMode(),
                serviceChargeLookup,
                descriptionMappingsLookup);
        return chargeLineCopyService.copy(request);
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
                    GLOBAL_ENTITY_CODE, GLOBAL_ENTITY_CODE, targetChargeType);
        }
        return mappings;
    }

    private static List<ChargeLineItem> filterSourceLinesForPaymentReference(
            List<ChargeLineItem> sourceLines,
            String paymentReference) {
        if (!StringUtils.hasText(paymentReference) || sourceLines == null || sourceLines.isEmpty()) {
            return sourceLines == null ? List.of() : sourceLines;
        }
        String normalizedPaymentReference = paymentReference.strip();
        return sourceLines.stream()
                .filter(line -> line != null
                        && line.getPaymentReference() != null
                        && line.getPaymentReference().strip().equalsIgnoreCase(normalizedPaymentReference))
                .collect(Collectors.toList());
    }

    private String resolveSourceEntityCode(Shipment shipment, String sourceChargeType) {
        return switch (normalize(sourceChargeType)) {
            case BatchProcessingConstants.CHARGE_CODE_ECS -> chargeLineSupport.resolveOriginEntityCode(shipment);
            case BatchProcessingConstants.CHARGE_CODE_EC3 -> chargeLineSupport.resolveThirdPartyEntityCode(shipment);
            default -> chargeLineSupport.resolveDomDestinationChargeEntityCode(shipment, sourceChargeType);
        };
    }

    private static String normalize(String chargeCode) {
        return chargeCode.strip().toUpperCase(Locale.ROOT);
    }

    private record CopyPair(String sourceChargeType, CopyMatchMode matchMode, CopyTaxMode taxMode) {
    }
}
