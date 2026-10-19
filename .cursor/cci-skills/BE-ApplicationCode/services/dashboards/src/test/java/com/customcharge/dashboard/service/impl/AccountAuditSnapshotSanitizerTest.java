package com.customcharge.dashboard.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountAuditSnapshotSanitizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sanitizeForApi_shouldStripJaversReference() throws Exception {
        ObjectNode ref = objectMapper.createObjectNode();
        ref.put("typeName", "Account");
        ref.put("cdoId", "123");

        assertThat(AccountAuditSnapshotSanitizer.sanitizeForApi(ref)).isNull();
    }

    @Test
    void sanitizeForApi_shouldRemoveAuditMetadataAndEmptyRates() throws Exception {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", "strip-me");
        node.put("createdBy", "user");
        node.put("name", "Account");
        node.set("rates", objectMapper.createArrayNode());

        var sanitized = AccountAuditSnapshotSanitizer.sanitizeForApi(node);

        assertThat(sanitized.get("id")).isNull();
        assertThat(sanitized.get("createdBy")).isNull();
        assertThat(sanitized.get("name").asText()).isEqualTo("Account");
        assertThat(sanitized.get("rates")).isNull();
    }

    @Test
    void sanitizeForApi_shouldCleanNestedArrays() throws Exception {
        ArrayNode array = objectMapper.createArrayNode();
        ObjectNode ref = objectMapper.createObjectNode();
        ref.put("typeName", "Rate");
        ref.put("cdoId", "1");
        ObjectNode kept = objectMapper.createObjectNode();
        kept.put("value", "ok");
        array.add(ref);
        array.add(kept);

        ArrayNode sanitized = (ArrayNode) AccountAuditSnapshotSanitizer.sanitizeForApi(array);

        assertThat(sanitized).hasSize(1);
        assertThat(sanitized.get(0).get("value").asText()).isEqualTo("ok");
    }

    @Test
    void sanitizeForApi_shouldKeepEnrichedRates() throws Exception {
        ObjectNode rate = objectMapper.createObjectNode();
        rate.put("rateType", "FIXED");
        rate.put("value", "10.00");
        ObjectNode node = objectMapper.createObjectNode();
        node.set("rates", objectMapper.createArrayNode().add(rate));

        var sanitized = AccountAuditSnapshotSanitizer.sanitizeForApi(node);

        assertThat(sanitized.get("rates")).hasSize(1);
        assertThat(sanitized.get("rates").get(0).get("value").asText()).isEqualTo("10.00");
    }
}
