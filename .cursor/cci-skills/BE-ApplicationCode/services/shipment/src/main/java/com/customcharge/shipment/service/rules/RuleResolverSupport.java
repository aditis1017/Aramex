package com.customcharge.shipment.service.rules;

import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.cache.MasterDataCacheKeys;
import com.customcharge.persistencelayer.cache.dto.MasterCategoryValueRow;
import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.dto.RuleSnapshotEntry;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.ShipmentRuleAssignment;
import com.customcharge.persistencelayer.enums.CurrencyType;
import com.customcharge.persistencelayer.repository.ShipmentRuleAssignmentRepository;
import com.customcharge.persistencelayer.rules.RuleEngine;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.shipment.constants.LogMessages;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RuleResolverSupport {

    public record ResolvedRule(String value, RuleAssignment assignment) {
    }

    public record RuleAssignment(
            String type,
            UUID ruleId,
            List<RuleSnapshotEntry> domainSnapshot,
            RuleSnapshotEntry ruleSnapshot
    ) {
    }

    private RuleResolverSupport() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, ?> loadRules(CacheService cacheService, String destination, String targetCode) {
        String key = GlobalSettingRuleCacheSupport.buildRuleDrlMapCacheKey(destination, targetCode);
        return cacheService.get(key, Map.class)
                .map(map -> (Map<String, ?>) map)
                .orElse(Collections.emptyMap());
    }

    public static List<String> loadCandidateCodes(CacheService cacheService, String categoryCode) {
        return loadCandidateCodes(cacheService, categoryCode, null);
    }

    public static List<String> loadCandidateCodes(
            CacheService cacheService,
            String primaryCategoryCode,
            String fallbackCategoryCode) {

        List<MasterCategoryValueRow> rows = cacheService
                .getList(MasterDataCacheKeys.categoryValuesClosure(primaryCategoryCode), MasterCategoryValueRow.class)
                .orElse(List.of());

        if (rows.isEmpty() && fallbackCategoryCode != null) {
            rows = cacheService
                    .getList(MasterDataCacheKeys.categoryValuesClosure(fallbackCategoryCode), MasterCategoryValueRow.class)
                    .orElse(List.of());
        }

        return rows.stream()
                .filter(row -> row != null && row.code() != null && !row.code().isBlank())
                .map(row -> row.code().trim())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    /**
     * Reorders codes so preferred entries (e.g. DDU before DDP) are evaluated first;
     * any other codes keep a stable case-insensitive order after them.
     */
    /**
     * Temporarily converts {@link ShipmentDto#getCostOfGoods()} to SAR for rule evaluation.
     */
    public static void applyCostOfGoodsSarConversion(ShipmentDto message, ExchangeRateService exchangeRateService) {
        if (message == null) {
            return;
        }
        Double costOfGoods = message.getCostOfGoods();
        if (costOfGoods == null || message.getCustomsCurrencyCode() == null || message.getCustomsCurrencyCode().isBlank()) {
            return;
        }
        if (exchangeRateService == null) {
            return;
        }
        CurrencyConversionResult result = exchangeRateService.convertAmount(
                costOfGoods,
                message.getCustomsCurrencyCode(),
                CurrencyType.SAR.name());
        if (result != null && result.convertedAmount() != null) {
            message.setCostOfGoods(result.convertedAmount());
        }
    }

    public static <T> T runWithTemporarySarCost(
            ShipmentDto message,
            ExchangeRateService exchangeRateService,
            Supplier<T> action) {
        Double originalCostOfGoods = message != null ? message.getCostOfGoods() : null;
        applyCostOfGoodsSarConversion(message, exchangeRateService);
        try {
            return action.get();
        } finally {
            if (message != null) {
                message.setCostOfGoods(originalCostOfGoods);
            }
        }
    }

    public static List<String> orderByPreferredCodes(List<String> codes, List<String> preferredOrder) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        if (preferredOrder == null || preferredOrder.isEmpty()) {
            return codes;
        }

        Map<String, Integer> rank = new HashMap<>();
        for (int i = 0; i < preferredOrder.size(); i++) {
            String code = preferredOrder.get(i);
            if (code != null && !code.isBlank()) {
                rank.put(code.trim().toUpperCase(), i);
            }
        }

        return codes.stream()
                .sorted(Comparator
                        .comparingInt((String code) ->
                                rank.getOrDefault(code.toUpperCase(), Integer.MAX_VALUE))
                        .thenComparing(String::compareToIgnoreCase))
                .toList();
    }

    public static Optional<RuleAssignment> executeOrderedRules(
            ShipmentDto message,
            List<PriorityOrderedRuleDrl.RuleEntry> rules,
            String domainCode,
            RuleEngine ruleEngine,
            Function<String, String> drlAdapter,
            Function<ShipmentDto, String> resolvedValueGetter,
            Logger log,
            String logContext) {
        return executeOrderedRules(
                message,
                rules,
                domainCode,
                ruleEngine,
                drlAdapter,
                resolvedValueGetter,
                log,
                logContext,
                false);
    }

    public static Optional<RuleAssignment> executeOrderedRules(
            ShipmentDto message,
            List<PriorityOrderedRuleDrl.RuleEntry> rules,
            String domainCode,
            RuleEngine ruleEngine,
            Function<String, String> drlAdapter,
            Function<ShipmentDto, String> resolvedValueGetter,
            Logger log,
            String logContext,
            boolean requireRulesFired) {

        if (rules == null || rules.isEmpty()) {
            return Optional.empty();
        }

        long batchStartedAtNanos = System.nanoTime();
        int rulesExecuted = 0;
        String hawb = resolveHawb(message);
        boolean resolved = false;

        List<RuleSnapshotEntry> domainSnapshot = rules.stream()
                .map(rule -> toSnapshotEntry(rule, domainCode))
                .toList();

        String adapterId = adapterIdFor(drlAdapter);

        try {
            for (PriorityOrderedRuleDrl.RuleEntry rule : rules) {
                long ruleStartedAtNanos = System.nanoTime();
                try {
                    String adaptedDrl = GlobalSettingRuleCacheSupport.adaptCached(adapterId, rule.drl(), drlAdapter);
                    if (requireRulesFired) {
                        if (ruleEngine.countRulesFired(message, adaptedDrl) <= 0) {
                            continue;
                        }
                    } else {
                        ruleEngine.execute(message, adaptedDrl);
                    }
                } catch (Exception ex) {
                    log.warn(
                            LogMessages.DROOLS_RULE_EXECUTION_FAILED,
                            message.getConsigneeCountryCode(),
                            logContext,
                            ex.getMessage()
                    );
                } finally {
                    rulesExecuted++;
                    log.debug(
                            LogMessages.DROOLS_RULE_EXECUTION_TIMING,
                            hawb,
                            logContext,
                            rule.settingId(),
                            elapsedMillis(ruleStartedAtNanos));
                }

                String resolvedValue = resolvedValueGetter.apply(message);
                if (resolvedValue != null && !resolvedValue.isBlank()) {
                    UUID ruleId = parseRuleId(rule.settingId());
                    if (ruleId != null) {
                        resolved = true;
                        return Optional.of(new RuleAssignment(
                                resolvedValue.trim(),
                                ruleId,
                                domainSnapshot,
                                toSnapshotEntry(rule, domainCode)
                        ));
                    }
                }
            }
            return Optional.empty();
        } finally {
            log.debug(
                    LogMessages.DROOLS_RULES_BATCH_TIMING,
                    hawb,
                    logContext,
                    rulesExecuted,
                    resolved,
                    elapsedMillis(batchStartedAtNanos));
        }
    }

    public static boolean executeOrderedRulesUntilTrue(
            ShipmentDto message,
            List<String> orderedDrls,
            RuleEngine ruleEngine,
            Function<String, String> drlAdapter,
            Function<ShipmentDto, Boolean> flagGetter,
            Logger log,
            String logContext) {

        long batchStartedAtNanos = System.nanoTime();
        int rulesExecuted = 0;
        String hawb = resolveHawb(message);
        boolean resolved = false;

        try {
            for (int index = 0; index < orderedDrls.size(); index++) {
                String drl = orderedDrls.get(index);
                long ruleStartedAtNanos = System.nanoTime();
                try {
                    String adaptedDrl = GlobalSettingRuleCacheSupport.adaptCached("fullness", drl, drlAdapter);
                    ruleEngine.execute(message, adaptedDrl);
                } catch (Exception ex) {
                    log.warn(
                            LogMessages.DROOLS_RULE_EXECUTION_FAILED,
                            message.getConsigneeCountryCode(),
                            logContext,
                            ex.getMessage()
                    );
                } finally {
                    rulesExecuted++;
                    log.debug(
                            LogMessages.DROOLS_RULE_EXECUTION_TIMING,
                            hawb,
                            logContext,
                            "index=" + index,
                            elapsedMillis(ruleStartedAtNanos));
                }
                if (Boolean.TRUE.equals(flagGetter.apply(message))) {
                    resolved = true;
                    return true;
                }
            }
            return false;
        } finally {
            log.debug(
                    LogMessages.DROOLS_RULES_BATCH_TIMING,
                    hawb,
                    logContext,
                    rulesExecuted,
                    resolved,
                    elapsedMillis(batchStartedAtNanos));
        }
    }

    public static void saveAssignment(
            ShipmentRuleAssignmentRepository repository,
            String hawbNumber,
            RuleAssignment assignment) {
        if (hawbNumber == null || hawbNumber.isBlank() || assignment == null) {
            return;
        }
        repository.save(toEntity(hawbNumber, assignment));
    }

    public static void saveAssignments(
            ShipmentRuleAssignmentRepository repository,
            String hawbNumber,
            List<RuleAssignment> assignments) {
        if (hawbNumber == null || hawbNumber.isBlank() || assignments == null || assignments.isEmpty()) {
            return;
        }
        List<ShipmentRuleAssignment> entities = new ArrayList<>();
        for (RuleAssignment assignment : assignments) {
            if (assignment != null) {
                entities.add(toEntity(hawbNumber, assignment));
            }
        }
        if (!entities.isEmpty()) {
            repository.saveAll(entities);
        }
    }

    private static ShipmentRuleAssignment toEntity(String hawbNumber, RuleAssignment assignment) {
        ShipmentRuleAssignment entity = new ShipmentRuleAssignment();
        entity.setHawbNumber(hawbNumber.trim());
        entity.setType(assignment.type());
        entity.setRuleId(assignment.ruleId());
        entity.setDomainSnapshot(assignment.domainSnapshot());
        entity.setRuleSnapshot(assignment.ruleSnapshot());
        return entity;
    }

    private static String adapterIdFor(Function<String, String> drlAdapter) {
        if (drlAdapter == null) {
            return "identity";
        }
        return "adapter:" + System.identityHashCode(drlAdapter);
    }

    private static RuleSnapshotEntry toSnapshotEntry(
            PriorityOrderedRuleDrl.RuleEntry rule,
            String fallbackDomainCode) {
        String domain = rule.domainCode() != null && !rule.domainCode().isBlank()
                ? rule.domainCode()
                : fallbackDomainCode;
        return new RuleSnapshotEntry(rule.settingId(), rule.priority(), domain);
    }

    private static UUID parseRuleId(String ruleId) {
        try {
            return UUID.fromString(ruleId.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private static String resolveHawb(ShipmentDto message) {
        if (message == null || message.getHawb() == null || message.getHawb().isBlank()) {
            return "unknown";
        }
        return message.getHawb().trim();
    }

    private static long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
