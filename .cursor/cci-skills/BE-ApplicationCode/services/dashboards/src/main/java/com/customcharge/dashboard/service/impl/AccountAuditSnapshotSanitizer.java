package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.constants.AccountAuditConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Strips Javers entity references and audit metadata from snapshot JSON for account audit API responses.
 */
final class AccountAuditSnapshotSanitizer {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private static final Set<String> AUDIT_METADATA_FIELDS_TO_STRIP =
            AccountAuditConstants.AUDIT_METADATA_FIELDS_TO_STRIP;

    /** Association collections that only hold Javers refs; omit when empty after sanitizing. */
    private static final Set<String> EMPTY_ARRAY_FIELDS_TO_OMIT =
            AccountAuditConstants.EMPTY_ARRAY_FIELDS_TO_OMIT;

    private AccountAuditSnapshotSanitizer() {
    }

    static JsonNode sanitizeForApi(JsonNode node) {
        JsonNode sanitized = strip(node);
        if (sanitized != null && sanitized.isObject() && sanitized.isEmpty()) {
            return null;
        }
        return sanitized;
    }

    private static JsonNode strip(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (isJaversEntityReference(node)) {
            return null;
        }
        if (node.isArray()) {
            ArrayNode sanitized = JSON.arrayNode();
            for (JsonNode element : node) {
                JsonNode cleaned = strip(element);
                if (cleaned != null && !cleaned.isNull()) {
                    sanitized.add(cleaned);
                }
            }
            return sanitized;
        }
        if (node.isObject()) {
            ObjectNode copy = ((ObjectNode) node).deepCopy();
            AUDIT_METADATA_FIELDS_TO_STRIP.forEach(copy::remove);
            List<String> fieldNames = new ArrayList<>();
            copy.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                JsonNode cleaned = strip(copy.get(fieldName));
                if (shouldOmitField(fieldName, cleaned)) {
                    copy.remove(fieldName);
                } else {
                    copy.set(fieldName, cleaned);
                }
            }
            return copy;
        }
        return node;
    }

    private static boolean shouldOmitField(String fieldName, JsonNode cleaned) {
        if (cleaned == null || cleaned.isNull()) {
            return true;
        }
        if (cleaned.isObject() && cleaned.isEmpty()) {
            return true;
        }
        return cleaned.isArray()
                && cleaned.isEmpty()
                && EMPTY_ARRAY_FIELDS_TO_OMIT.contains(fieldName);
    }

    private static boolean isJaversEntityReference(JsonNode node) {
        if (!node.isObject() || !node.has(AccountAuditConstants.JAVERS_TYPE_NAME)
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
}
