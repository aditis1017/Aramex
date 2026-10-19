package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.constants.AccountAuditConstants;
import com.customcharge.persistencelayer.cache.MasterCategoryValueLookupService;
import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.enums.RateType;
import com.customcharge.persistencelayer.repository.RateSheetItemRateRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAuditSnapshotEnricherTest {

    @Mock
    private RateSheetItemRateRepository rateSheetItemRateRepository;

    @Mock
    private ServiceCodesRepository serviceCodesRepository;

    @Mock
    private MasterCategoryValueLookupService masterCategoryValueLookupService;

    @InjectMocks
    private AccountAuditSnapshotEnricher enricher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void enrich_shouldResolveRateReferences() {
        UUID rateId = UUID.randomUUID();
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode rateRef = objectMapper.createObjectNode();
        rateRef.put(AccountAuditConstants.JAVERS_TYPE_NAME, "RateSheetItemRate");
        rateRef.put(AccountAuditConstants.JAVERS_CDO_ID, rateId.toString());
        root.set(AccountAuditConstants.FIELD_RATES, objectMapper.createArrayNode().add(rateRef));

        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateType(RateType.FIXED);
        rate.setValue(new BigDecimal("12.50"));
        when(rateSheetItemRateRepository.findById(rateId)).thenReturn(Optional.of(rate));

        ObjectNode enriched = (ObjectNode) enricher.enrich(root);

        assertThat(enriched.get(AccountAuditConstants.FIELD_RATES)).hasSize(1);
        assertThat(enriched.get(AccountAuditConstants.FIELD_RATES).get(0).get(AccountAuditConstants.FIELD_RATE_TYPE).asText())
                .isEqualTo("FIXED");
        assertThat(enriched.get(AccountAuditConstants.FIELD_RATES).get(0).get(AccountAuditConstants.FIELD_VALUE).asText())
                .isEqualTo("12.50");
    }

    @Test
    void enrich_shouldResolveServiceCodeReference() {
        UUID serviceCodeId = UUID.randomUUID();
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode serviceCodeRef = objectMapper.createObjectNode();
        serviceCodeRef.put(AccountAuditConstants.JAVERS_TYPE_NAME, "ServiceCodes");
        serviceCodeRef.put(AccountAuditConstants.JAVERS_CDO_ID, serviceCodeId.toString());
        root.set(AccountAuditConstants.FIELD_SERVICE_CODES, serviceCodeRef);

        ServiceCodes serviceCode = new ServiceCodes();
        serviceCode.setServiceCode("DTC");
        serviceCode.setServiceDescription("Door to door");
        when(serviceCodesRepository.findById(serviceCodeId)).thenReturn(Optional.of(serviceCode));

        ObjectNode enriched = (ObjectNode) enricher.enrich(root);

        assertThat(enriched.has(AccountAuditConstants.FIELD_SERVICE_CODES)).isFalse();
        assertThat(enriched.get(AccountAuditConstants.FIELD_SERVICE_CODE).asText()).isEqualTo("DTC");
        assertThat(enriched.get(AccountAuditConstants.FIELD_SERVICE_DESCRIPTION).asText()).isEqualTo("Door to door");
    }

    @Test
    void enrich_shouldResolveAccountCategoryLabels() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("rateCurrency", "USD");

        when(masterCategoryValueLookupService.resolveDisplayName("USD")).thenReturn("US Dollar");

        ObjectNode enriched = (ObjectNode) enricher.enrich(root);

        assertThat(enriched.get("rateCurrency").asText()).isEqualTo("US Dollar");
    }

    @Test
    void enrich_shouldKeepPercentageRateValues() {
        UUID rateId = UUID.randomUUID();
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode rateRef = objectMapper.createObjectNode();
        rateRef.put(AccountAuditConstants.JAVERS_TYPE_NAME, "RateSheetItemRate");
        rateRef.put(AccountAuditConstants.JAVERS_CDO_ID, rateId.toString());
        root.set(AccountAuditConstants.FIELD_RATES, objectMapper.createArrayNode().add(rateRef));

        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateType(RateType.PERCENTAGE);
        rate.setPercentageOf(PercentageOf.CIF_VALUE);
        rate.setValue(new BigDecimal("5.00"));
        when(rateSheetItemRateRepository.findById(rateId)).thenReturn(Optional.of(rate));

        ObjectNode enriched = (ObjectNode) enricher.enrich(root);

        assertThat(enriched.get(AccountAuditConstants.FIELD_RATES).get(0).get(AccountAuditConstants.FIELD_PERCENTAGE_OF).asText())
                .isEqualTo("CIF_VALUE");
    }

    @Test
    void enrich_shouldReturnNullForNullNode() {
        assertThat(enricher.enrich(null)).isNull();
    }

    @Test
    void enrich_shouldTraverseNestedArrays() {
        ObjectNode child = objectMapper.createObjectNode();
        child.put("note", "value");
        ArrayNode array = objectMapper.createArrayNode().add(child);
        ObjectNode root = objectMapper.createObjectNode();
        root.set("items", array);

        ObjectNode enriched = (ObjectNode) enricher.enrich(root);

        assertThat(enriched.get("items").get(0).get("note").asText()).isEqualTo("value");
    }

    @Test
    void enrich_shouldBatchLoadRatesWhenPrepared() {
        UUID rateId = UUID.randomUUID();
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode rateRef = objectMapper.createObjectNode();
        rateRef.put(AccountAuditConstants.JAVERS_TYPE_NAME, "RateSheetItemRate");
        rateRef.put(AccountAuditConstants.JAVERS_CDO_ID, rateId.toString());
        root.set(AccountAuditConstants.FIELD_RATES, objectMapper.createArrayNode().add(rateRef));

        RateSheetItemRate rate = mock(RateSheetItemRate.class);
        when(rate.getId()).thenReturn(rateId);
        when(rate.getRateType()).thenReturn(RateType.FIXED);
        when(rate.getValue()).thenReturn(new BigDecimal("9.99"));
        when(rateSheetItemRateRepository.findAllById(any())).thenReturn(List.of(rate));

        enricher.prepareBatch(List.of(root));
        try {
            ObjectNode enriched = (ObjectNode) enricher.enrich(root);
            assertThat(enriched.get(AccountAuditConstants.FIELD_RATES).get(0).get(AccountAuditConstants.FIELD_VALUE).asText())
                    .isEqualTo("9.99");
        } finally {
            enricher.clearBatch();
        }

        verify(rateSheetItemRateRepository).findAllById(any());
    }

    @Test
    void enrich_shouldReturnScalarNodesUnchanged() {
        assertThat(enricher.enrich(objectMapper.getNodeFactory().textNode("plain")).asText())
                .isEqualTo("plain");
    }
}
