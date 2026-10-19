package com.customcharge.global_settings.rules.service.imp;

import com.customcharge.global_settings.rules.dto.RuleCacheEntry;
import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class GlobalSettingDrlCacheManager {
    private final CacheService redisCacheService;

    public GlobalSettingDrlCacheManager(CacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    public void syncOnCreate(GlobalSetting setting) {
        updateCacheForSetting(setting);
    }

    public void syncOnUpdate(UUID previousId, String previousDestinationCode, String previousTargetValue,
                             GlobalSetting after) {
        removeFromCache(previousId, previousDestinationCode, previousTargetValue);
        updateCacheForSetting(after);
    }

    public void syncOnStatusChange(GlobalSetting setting) {
        if (Boolean.TRUE.equals(setting.getStatus())) {
            updateCacheForSetting(setting);
        } else {
            removeFromCache(setting);
        }
    }

    public void syncOnDelete(GlobalSetting setting) {
        removeFromCache(setting);
    }

    private String buildCacheKey(GlobalSetting setting) {
        if (setting == null || setting.getDestination() == null || setting.getTargetValue() == null) {
            return null;
        }
        return buildCacheKeyFromCodes(setting.getDestination(), setting.getTargetValue());
    }

    private static String buildCacheKeyFromCodes(String destinationCode, String targetValue) {
        if (destinationCode == null || targetValue == null) {
            return null;
        }
        String destination = normalizeKeyPart(destinationCode);
        String target = normalizeKeyPart(targetValue);
        return "rule:" + destination + ":" + target;
    }

    private static String normalizeKeyPart(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "_");
    }

    @SuppressWarnings("unchecked")
    private Map<String, RuleCacheEntry> loadDrlMap(String cacheKey) {
        return redisCacheService
                .get(cacheKey, Map.class)
                .map(m -> convertToRuleCacheMap((Map<String, Object>) m))
                .orElseGet(HashMap::new);
    }

    private Map<String, RuleCacheEntry> convertToRuleCacheMap(Map<String, Object> sourceMap) {
        Map<String, RuleCacheEntry> convertedMap = new HashMap<>();
        if (sourceMap == null || sourceMap.isEmpty()) {
            return convertedMap;
        }
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            String settingId = entry.getKey();
            RuleCacheEntry convertedEntry = toRuleCacheEntry(entry.getValue());
            if (settingId != null && convertedEntry != null && convertedEntry.getRule() != null) {
                convertedMap.put(settingId, convertedEntry);
            }
        }
        return convertedMap;
    }

    @SuppressWarnings("unchecked")
    private RuleCacheEntry toRuleCacheEntry(Object cacheValue) {
        if (cacheValue == null) {
            return null;
        }
        if (cacheValue instanceof RuleCacheEntry ruleCacheEntry) {
            return ruleCacheEntry;
        }
        if (cacheValue instanceof String legacyRuleValue) {
            return new RuleCacheEntry(null, legacyRuleValue);
        }
        if (cacheValue instanceof Map<?, ?> valueMap) {
            Object priorityValue = valueMap.get("priority");
            Integer priority = priorityValue instanceof Number
                    ? ((Number) priorityValue).intValue()
                    : null;
            Object ruleValue = valueMap.get("rule");
            String rule = Objects.toString(ruleValue, null);
            return new RuleCacheEntry(priority, rule);
        }
        return null;
    }

    private void saveDrlMap(String cacheKey, Map<String, RuleCacheEntry> drlMap) {
        if (cacheKey == null) {
            return;
        }
        if (drlMap == null || drlMap.isEmpty()) {
            redisCacheService.evict(cacheKey);
        } else {
            redisCacheService.put(cacheKey, drlMap, Duration.ofHours(25));
        }
    }

    private void updateCacheForSetting(GlobalSetting setting) {
        if (setting == null || setting.getId() == null || !Boolean.TRUE.equals(setting.getStatus())) {
            removeFromCache(setting);
            return;
        }
        String cacheKey = buildCacheKey(setting);
        if (cacheKey == null) {
            return;
        }
        Map<String, RuleCacheEntry> drlMap = loadDrlMap(cacheKey);
        drlMap.put(
                setting.getId().toString(),
                new RuleCacheEntry(setting.getSequenceNumber(), setting.getDrlRule())
        );
        saveDrlMap(cacheKey, drlMap);
    }

    private void removeFromCache(GlobalSetting setting) {
        if (setting == null || setting.getId() == null) {
            return;
        }
        String cacheKey = buildCacheKey(setting);
        removeRuleIdFromMap(cacheKey, setting.getId());
    }

    private void removeFromCache(UUID previousId, String previousDestinationCode, String previousTargetValue) {
        if (previousId == null) {
            return;
        }
        String cacheKey = buildCacheKeyFromCodes(previousDestinationCode, previousTargetValue);
        removeRuleIdFromMap(cacheKey, previousId);
    }

    private void removeRuleIdFromMap(String cacheKey, UUID ruleId) {
        if (cacheKey == null) {
            return;
        }
        Map<String, RuleCacheEntry> drlMap = loadDrlMap(cacheKey);
        if (drlMap.remove(ruleId.toString()) != null) {
            saveDrlMap(cacheKey, drlMap);
        }
    }
}
