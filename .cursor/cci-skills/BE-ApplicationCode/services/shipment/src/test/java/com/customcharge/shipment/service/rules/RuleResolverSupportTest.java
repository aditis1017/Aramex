package com.customcharge.shipment.service.rules;

import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.cache.dto.MasterCategoryValueRow;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.ShipmentRuleAssignment;
import com.customcharge.persistencelayer.repository.ShipmentRuleAssignmentRepository;
import com.customcharge.persistencelayer.rules.RuleEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleResolverSupportTest {

    @Mock
    private CacheService cacheService;
    @Mock
    private RuleEngine ruleEngine;
    @Mock
    private ShipmentRuleAssignmentRepository assignmentRepository;
    @Mock
    private Logger log;

    @Test
    void loadRules_shouldUseCacheKey() {
        Map<String, Object> rules = Map.of("id", Map.of("priority", 1, "rule", "when then"));
        when(cacheService.get("rule:AE:DDU", Map.class)).thenReturn(Optional.of(rules));

        assertThat(RuleResolverSupport.loadRules(cacheService, " AE ", " DDU ")).isEqualTo(rules);
    }

    @Test
    void loadCandidateCodes_shouldReturnDistinctSortedCodes() {
        when(cacheService.getList(any(), eq(MasterCategoryValueRow.class)))
                .thenReturn(Optional.of(List.of(
                        new MasterCategoryValueRow(null, null, "DDP", null, null, null, null, null, null),
                        new MasterCategoryValueRow(null, null, "ddu", null, null, null, null, null, null),
                        new MasterCategoryValueRow(null, null, null, null, null, null, null, null, null),
                        new MasterCategoryValueRow(null, null, " ", null, null, null, null, null, null))));

        assertThat(RuleResolverSupport.loadCandidateCodes(cacheService, "SHIPMENT_TYPE"))
                .containsExactly("DDP", "ddu");
    }

    @Test
    void orderByPreferredCodes_shouldRankPreferredEntriesFirst() {
        List<String> ordered = RuleResolverSupport.orderByPreferredCodes(
                List.of("DDP", "DDU", "OTHER"),
                List.of("DDU", "DDP"));

        assertThat(ordered).containsExactly("DDU", "DDP", "OTHER");
    }

    @Test
    void executeOrderedRules_shouldReturnFirstResolvedAssignment() {
        ShipmentDto message = new ShipmentDto();
        UUID ruleId = UUID.randomUUID();
        List<PriorityOrderedRuleDrl.RuleEntry> rules = List.of(
                new PriorityOrderedRuleDrl.RuleEntry(ruleId.toString(), 1, "rule-a", "DOMAIN"));

        Optional<RuleResolverSupport.RuleAssignment> result = RuleResolverSupport.executeOrderedRules(
                message,
                rules,
                "DOMAIN",
                ruleEngine,
                drl -> drl,
                dto -> "DDU",
                log,
                "shipment-type");

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo("DDU");
        assertThat(result.get().ruleId()).isEqualTo(ruleId);
        verify(ruleEngine).execute(message, "rule-a");
    }

    @Test
    void executeOrderedRules_shouldContinueWhenRuleExecutionFails() {
        ShipmentDto message = new ShipmentDto();
        UUID ruleId = UUID.randomUUID();
        List<PriorityOrderedRuleDrl.RuleEntry> rules = List.of(
                new PriorityOrderedRuleDrl.RuleEntry("bad-id", 1, "rule-a", "DOMAIN"),
                new PriorityOrderedRuleDrl.RuleEntry(ruleId.toString(), 2, "rule-b", "DOMAIN"));

        doThrow(new RuntimeException("boom"))
                .when(ruleEngine)
                .execute(any(ShipmentDto.class), eq("rule-a"));

        Optional<RuleResolverSupport.RuleAssignment> result = RuleResolverSupport.executeOrderedRules(
                message,
                rules,
                "DOMAIN",
                ruleEngine,
                drl -> drl,
                dto -> "DDP",
                log,
                "shipment-type");

        assertThat(result).isPresent();
        assertThat(result.get().ruleId()).isEqualTo(ruleId);
        verify(ruleEngine).execute(message, "rule-a");
        verify(ruleEngine).execute(message, "rule-b");
    }

    @Test
    void executeOrderedRulesUntilTrue_shouldStopOnFirstTrueFlag() {
        ShipmentDto message = new ShipmentDto();
        message.setIsShipmentFull(false);

        boolean result = RuleResolverSupport.executeOrderedRulesUntilTrue(
                message,
                List.of("rule-a", "rule-b"),
                ruleEngine,
                drl -> drl,
                ShipmentDto::getIsShipmentFull,
                log,
                "fullness");

        assertThat(result).isFalse();
        verify(ruleEngine).execute(message, "rule-a");
        verify(ruleEngine).execute(message, "rule-b");
    }

    @Test
    void saveAssignment_shouldPersistWhenInputsValid() {
        UUID ruleId = UUID.randomUUID();
        RuleResolverSupport.RuleAssignment assignment = new RuleResolverSupport.RuleAssignment(
                "DDU",
                ruleId,
                List.of(),
                null);

        RuleResolverSupport.saveAssignment(assignmentRepository, " HAWB1 ", assignment);

        ArgumentCaptor<ShipmentRuleAssignment> captor = ArgumentCaptor.forClass(ShipmentRuleAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getHawbNumber()).isEqualTo("HAWB1");
        assertThat(captor.getValue().getType()).isEqualTo("DDU");
        assertThat(captor.getValue().getRuleId()).isEqualTo(ruleId);
    }

    @Test
    void saveAssignment_shouldSkipBlankHawb() {
        RuleResolverSupport.saveAssignment(
                assignmentRepository,
                "  ",
                new RuleResolverSupport.RuleAssignment("DDU", UUID.randomUUID(), List.of(), null));

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void loadCandidateCodes_shouldUseFallbackCategory() {
        when(cacheService.getList(any(), eq(MasterCategoryValueRow.class)))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(List.of(
                        new MasterCategoryValueRow(null, null, "DDU", null, null, null, null, null, null))));

        assertThat(RuleResolverSupport.loadCandidateCodes(cacheService, "CUSTOMS_TYPE", "SHIPMENT_TYPE"))
                .containsExactly("DDU");
    }

    @Test
    void orderByPreferredCodes_shouldHandleNullOrEmptyInputs() {
        assertThat(RuleResolverSupport.orderByPreferredCodes(null, List.of("DDU"))).isEmpty();
        assertThat(RuleResolverSupport.orderByPreferredCodes(List.of("DDU"), null))
                .containsExactly("DDU");
    }

    @Test
    void executeOrderedRules_shouldReturnEmpty_whenRulesNull() {
        assertThat(RuleResolverSupport.executeOrderedRules(
                new ShipmentDto(),
                null,
                "DOMAIN",
                ruleEngine,
                drl -> drl,
                ShipmentDto::getShipmentType,
                log,
                "ctx")).isEmpty();
    }

    @Test
    void executeOrderedRules_shouldRequireRulesFired_whenFlagEnabled() {
        ShipmentDto message = new ShipmentDto();
        message.setShipmentType("DDO");
        UUID ruleId = UUID.randomUUID();
        List<PriorityOrderedRuleDrl.RuleEntry> rules = List.of(
                new PriorityOrderedRuleDrl.RuleEntry(ruleId.toString(), 1, "rule-ddo", "DDO"));

        when(ruleEngine.countRulesFired(message, "rule-ddo")).thenReturn(0);

        Optional<RuleResolverSupport.RuleAssignment> result = RuleResolverSupport.executeOrderedRules(
                message,
                rules,
                "DDO",
                ruleEngine,
                drl -> drl,
                ShipmentDto::getShipmentType,
                log,
                "shipment-type",
                true);

        assertThat(result).isEmpty();
        assertThat(message.getShipmentType()).isEqualTo("DDO");
        verify(ruleEngine).countRulesFired(message, "rule-ddo");
        verify(ruleEngine, never()).execute(any(), any());
    }
}
