package com.customcharge.chargescalculator.service.charge;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.persistencelayer.dto.ServiceChargeDescriptionMappingDto;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DtaContextSupport {

    private final ServiceChargeMappingRepository serviceChargeMappingRepository;

    public DtaContextSupport(ServiceChargeMappingRepository serviceChargeMappingRepository) {
        this.serviceChargeMappingRepository = serviceChargeMappingRepository;
    }

    public record DtaLineContext(
            Map<String, BigDecimal> amountsByDescription,
            boolean useDtaAmounts,
            boolean siblingChargeContext,
            Map<String, BigDecimal> rawCapture) {

        public void mergeCapture(String description, BigDecimal amount) {
            if (rawCapture == null || amount == null) {
                return;
            }
            String key = ChargeLineSupport.normalizeDescription(description);
            if (!key.isEmpty()) {
                rawCapture.merge(key, amount, BigDecimal::add);
            }
        }

        public void persist(ChargeCalculationContext ctx) {
            if (rawCapture != null) {
                ctx.getCalculatedCharges().put(
                        BatchProcessingConstants.CALCULATED_CHARGES_KEY_DTA_REVENUE, rawCapture);
            }
        }
    }

    public DtaLineContext forCharge(
            ChargeCalculationContext ctx,
            Shipment shipment,
            String chargeCode,
            String billingEntityCode,
            ChargeLineSupport chargeLineSupport) {
        Map<String, BigDecimal> raw = ChargeLineSupport.dtaRevenueMap(
                ctx.getCalculatedCharges().get(BatchProcessingConstants.CALCULATED_CHARGES_KEY_DTA_REVENUE));
        String fromEntity = chargeLineSupport.resolveDomDestinationChargeEntityCode(shipment, chargeCode);
        String normalizedChargeCode = chargeCode == null ? "" : chargeCode.strip().toUpperCase(java.util.Locale.ROOT);
        boolean estimate = BatchProcessingConstants.PROCESS_FOR_ESTIMATE.equalsIgnoreCase(ctx.getProcessFor());
        boolean dtaCharge = BatchProcessingConstants.CHARGE_CODE_DTA.equalsIgnoreCase(normalizedChargeCode);
        boolean ecsEc3Charge = BatchProcessingConstants.CHARGE_CODE_ECS.equalsIgnoreCase(normalizedChargeCode)
                || BatchProcessingConstants.CHARGE_CODE_EC3.equalsIgnoreCase(normalizedChargeCode);
        boolean siblingContextEstimate = estimate
                && ChargeLineSupport.usesDtsStyleLineAmountResolution(normalizedChargeCode);
        Map<String, BigDecimal> amounts = siblingContextEstimate
                ? Map.copyOf(raw)
                : resolveForDownstreamCharge(fromEntity, billingEntityCode, chargeCode, raw, !estimate);
        boolean useDta = !ChargeLineSupport.resolveDtaMappingChargeType(chargeCode).isBlank();
        boolean enableCapture = dtaCharge || (estimate && ecsEc3Charge);
        Map<String, BigDecimal> capture = enableCapture ? new LinkedHashMap<>() : null;
        return new DtaLineContext(amounts, useDta, siblingContextEstimate, capture);
    }

    public Map<String, BigDecimal> resolveForDownstreamCharge(
            String fromEntity,
            String billingEntityCode,
            String chargeCode,
            Map<String, BigDecimal> rawDtaContext) {
        return resolveForDownstreamCharge(fromEntity, billingEntityCode, chargeCode, rawDtaContext, true);
    }

    public Map<String, BigDecimal> resolveForDownstreamCharge(
            String fromEntity,
            String billingEntityCode,
            String chargeCode,
            Map<String, BigDecimal> rawDtaContext,
            boolean actualProcess) {
        if (rawDtaContext == null || rawDtaContext.isEmpty()) {
            return Map.of();
        }
        String mappingChargeType = ChargeLineSupport.resolveDtaContextMappingChargeType(chargeCode, actualProcess);
        if (mappingChargeType.isBlank()) {
            return rawDtaContext;
        }
        return remapForDownstream(fromEntity, billingEntityCode, mappingChargeType, rawDtaContext);
    }

    Map<String, BigDecimal> remapForDownstream(
            String fromEntity,
            String toEntity,
            String chargeType,
            Map<String, BigDecimal> rawContext) {
        if (rawContext == null || rawContext.isEmpty()) {
            return Map.of();
        }
        if (fromEntity == null || fromEntity.isBlank()
                || toEntity == null || toEntity.isBlank()
                || chargeType == null || chargeType.isBlank()) {
            return Map.copyOf(rawContext);
        }
        List<ServiceChargeDescriptionMappingDto> mappings =
                serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                        fromEntity.strip(), toEntity.strip(), chargeType.strip());
        if (mappings.isEmpty()) {
            mappings = serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                    BatchProcessingConstants.GLOBAL_ENTITY_CODE,
                    BatchProcessingConstants.GLOBAL_ENTITY_CODE,
                    chargeType.strip());
        }
        return remapByDescription(rawContext, mappings);
    }

    static Map<String, BigDecimal> remapByDescription(
            Map<String, BigDecimal> context,
            List<ServiceChargeDescriptionMappingDto> mappings) {
        Map<String, BigDecimal> remapped = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : context.entrySet()) {
            String key = entry.getKey();
            BigDecimal value = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
            List<ServiceChargeDescriptionMappingDto> matches = mappings.stream()
                    .filter(m -> key.equals(ChargeLineSupport.normalizeDescription(m.fromDescription())))
                    .toList();
            if (matches.isEmpty()) {
                remapped.merge(key, value, BigDecimal::add);
                continue;
            }
            for (ServiceChargeDescriptionMappingDto match : matches) {
                String targetKey = ChargeLineSupport.normalizeDescription(match.toDescription());
                if (!targetKey.isEmpty()) {
                    remapped.merge(targetKey, value, BigDecimal::add);
                }

            }
        }
        for (ServiceChargeDescriptionMappingDto mapping : mappings) {
            String targetKey = ChargeLineSupport.normalizeDescription(mapping.toDescription());
            if (!targetKey.isEmpty()) {
                remapped.putIfAbsent(targetKey, BigDecimal.ZERO);
            }
        }
        return remapped;
    }
}
