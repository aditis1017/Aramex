package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.constants.AccountAuditConstants;
import com.customcharge.persistencelayer.cache.MasterCategoryValueLookupService;
import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.repository.RateSheetItemRateRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves Javers entity references and master-data codes before account audit snapshots are sanitized.
 */
@Component
@RequiredArgsConstructor
class AccountAuditSnapshotEnricher {

    private final RateSheetItemRateRepository rateSheetItemRateRepository;
    private final ServiceCodesRepository serviceCodesRepository;
    private final MasterCategoryValueLookupService masterCategoryValueLookupService;

    private BatchContext batchContext;

    void prepareBatch(List<JsonNode> roots) {
        batchContext = buildBatchContext(roots);
    }

    void clearBatch() {
        batchContext = null;
    }

    JsonNode enrich(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isArray()) {
            ArrayNode enriched = node.deepCopy();
            for (int i = 0; i < enriched.size(); i++) {
                enriched.set(i, enrich(enriched.get(i)));
            }
            return enriched;
        }
        if (!node.isObject()) {
            return node;
        }
        ObjectNode copy = node.deepCopy();
        enrichRates(copy);
        enrichServiceCodes(copy);
        resolveAccountLabels(copy);
        return copy;
    }

    private void enrichRates(ObjectNode node) {
        JsonNode ratesNode = node.get(AccountAuditConstants.FIELD_RATES);
        if (ratesNode == null || !ratesNode.isArray()) {
            return;
        }
        ArrayNode enrichedRates = ratesNode.deepCopy();
        boolean changed = false;
        for (int i = 0; i < enrichedRates.size(); i++) {
            JsonNode rateNode = enrichedRates.get(i);
            if (!isJaversEntityReference(rateNode)) {
                continue;
            }
            JsonNode resolved = resolveRateReference(rateNode);
            if (resolved != null) {
                enrichedRates.set(i, resolved);
                changed = true;
            }
        }
        if (changed) {
            node.set(AccountAuditConstants.FIELD_RATES, enrichedRates);
        }
    }

    private JsonNode resolveRateReference(JsonNode rateNode) {
        UUID rateId = parseCdoId(rateNode);
        if (rateId == null) {
            return null;
        }
        RateSheetItemRate rate = resolveRate(rateId);
        return rate == null ? null : toRateNode(rate);
    }

    private RateSheetItemRate resolveRate(UUID rateId) {
        if (batchContext != null) {
            return batchContext.ratesById().get(rateId);
        }
        return rateSheetItemRateRepository.findById(rateId).orElse(null);
    }

    private ObjectNode toRateNode(RateSheetItemRate rate) {
        ObjectNode rateObject = rateNodeFactory().objectNode();
        if (rate.getRateType() != null) {
            rateObject.put(AccountAuditConstants.FIELD_RATE_TYPE, rate.getRateType().name());
        }
        if (rate.getPercentageOf() != null) {
            rateObject.put(AccountAuditConstants.FIELD_PERCENTAGE_OF, rate.getPercentageOf().name());
        }
        if (rate.getValue() != null) {
            rateObject.put(AccountAuditConstants.FIELD_VALUE, rate.getValue());
        }
        return rateObject;
    }

    private void enrichServiceCodes(ObjectNode node) {
        JsonNode serviceCodesNode = node.get(AccountAuditConstants.FIELD_SERVICE_CODES);
        if (!isJaversEntityReference(serviceCodesNode)) {
            return;
        }
        UUID serviceCodeId = parseCdoId(serviceCodesNode);
        if (serviceCodeId == null) {
            return;
        }
        ServiceCodes serviceCode = resolveServiceCode(serviceCodeId);
        if (serviceCode != null) {
            applyServiceCodeFields(node, serviceCode);
        }
    }

    private ServiceCodes resolveServiceCode(UUID serviceCodeId) {
        if (batchContext != null) {
            return batchContext.serviceCodesById().get(serviceCodeId);
        }
        return serviceCodesRepository.findById(serviceCodeId).orElse(null);
    }

    private void applyServiceCodeFields(ObjectNode node, ServiceCodes serviceCode) {
        node.remove(AccountAuditConstants.FIELD_SERVICE_CODES);
        if (serviceCode.getServiceCode() != null) {
            node.put(AccountAuditConstants.FIELD_SERVICE_CODE, serviceCode.getServiceCode());
        }
        if (serviceCode.getServiceDescription() != null) {
            node.put(AccountAuditConstants.FIELD_SERVICE_DESCRIPTION, serviceCode.getServiceDescription());
        }
    }

    private void resolveAccountLabels(ObjectNode node) {
        for (Map.Entry<String, String> fieldCategory : AccountAuditConstants.LABEL_FIELD_CATEGORIES.entrySet()) {
            JsonNode valueNode = node.get(fieldCategory.getKey());
            if (valueNode == null || valueNode.isNull() || !valueNode.isTextual()) {
                continue;
            }
            String code = valueNode.asText();
            if (code == null || code.isBlank()) {
                continue;
            }
            String displayName = resolveLabel(code);
            if (displayName != null && !displayName.isBlank() && !displayName.equals(code)) {
                node.put(fieldCategory.getKey(), displayName);
            }
        }
    }

    private String resolveLabel(String code) {
        if (batchContext != null) {
            return batchContext.labelByCode().getOrDefault(code, code);
        }
        return masterCategoryValueLookupService.resolveDisplayName(code);
    }

    private BatchContext buildBatchContext(List<JsonNode> roots) {
        Set<UUID> rateIds = new HashSet<>();
        Set<UUID> serviceCodeIds = new HashSet<>();
        Set<String> labelCodes = new HashSet<>();
        for (JsonNode root : roots) {
            collectBatchKeys(root, rateIds, serviceCodeIds, labelCodes);
        }

        Map<UUID, RateSheetItemRate> ratesById = new HashMap<>();
        if (!rateIds.isEmpty()) {
            rateSheetItemRateRepository.findAllById(rateIds).forEach(rate -> ratesById.put(rate.getId(), rate));
        }

        Map<UUID, ServiceCodes> serviceCodesById = new HashMap<>();
        if (!serviceCodeIds.isEmpty()) {
            serviceCodesRepository.findAllByIdInWithEntity(serviceCodeIds)
                    .forEach(serviceCode -> serviceCodesById.put(serviceCode.getId(), serviceCode));
        }

        Map<String, String> labelByCode = new HashMap<>();
        for (String code : labelCodes) {
            labelByCode.put(code, masterCategoryValueLookupService.resolveDisplayName(code));
        }

        return new BatchContext(ratesById, serviceCodesById, labelByCode);
    }

    private void collectBatchKeys(
            JsonNode node,
            Set<UUID> rateIds,
            Set<UUID> serviceCodeIds,
            Set<String> labelCodes) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectBatchKeys(child, rateIds, serviceCodeIds, labelCodes);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        JsonNode ratesNode = node.get(AccountAuditConstants.FIELD_RATES);
        if (ratesNode != null && ratesNode.isArray()) {
            for (JsonNode rateNode : ratesNode) {
                if (isJaversEntityReference(rateNode)) {
                    UUID rateId = parseCdoId(rateNode);
                    if (rateId != null) {
                        rateIds.add(rateId);
                    }
                }
            }
        }
        JsonNode serviceCodesNode = node.get(AccountAuditConstants.FIELD_SERVICE_CODES);
        if (isJaversEntityReference(serviceCodesNode)) {
            UUID serviceCodeId = parseCdoId(serviceCodesNode);
            if (serviceCodeId != null) {
                serviceCodeIds.add(serviceCodeId);
            }
        }
        for (String labelField : AccountAuditConstants.LABEL_FIELD_CATEGORIES.keySet()) {
            JsonNode valueNode = node.get(labelField);
            if (valueNode != null && valueNode.isTextual() && !valueNode.asText().isBlank()) {
                labelCodes.add(valueNode.asText());
            }
        }
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            collectBatchKeys(node.get(fieldNames.next()), rateIds, serviceCodeIds, labelCodes);
        }
    }

    private static boolean isJaversEntityReference(JsonNode node) {
        if (node == null || !node.isObject()
                || !node.has(AccountAuditConstants.JAVERS_TYPE_NAME)
                || !node.has(AccountAuditConstants.JAVERS_CDO_ID)) {
            return false;
        }
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String name = fieldNames.next();
            if (!AccountAuditConstants.JAVERS_TYPE_NAME.equals(name)
                    && !AccountAuditConstants.JAVERS_CDO_ID.equals(name)) {
                return false;
            }
        }
        return true;
    }

    private static UUID parseCdoId(JsonNode node) {
        if (node == null || !node.has(AccountAuditConstants.JAVERS_CDO_ID)) {
            return null;
        }
        String raw = node.get(AccountAuditConstants.JAVERS_CDO_ID).asText();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static com.fasterxml.jackson.databind.node.JsonNodeFactory rateNodeFactory() {
        return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;
    }

    private record BatchContext(
            Map<UUID, RateSheetItemRate> ratesById,
            Map<UUID, ServiceCodes> serviceCodesById,
            Map<String, String> labelByCode) {
    }
}
