package com.customcharge.persistencelayer.rules;

import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.repository.GlobalSettingReadRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GlobalSettingRuleLookupService {

    private static final String RULE_KEY_PREFIX = "rule:";

    private final ObjectProvider<CacheService> cacheServiceProvider;
    private final GlobalSettingReadRepository globalSettingReadRepository;

    public GlobalSettingRuleLookupService(
            ObjectProvider<CacheService> cacheServiceProvider,
            GlobalSettingReadRepository globalSettingReadRepository) {
        this.cacheServiceProvider = cacheServiceProvider;
        this.globalSettingReadRepository = globalSettingReadRepository;
    }

    @SuppressWarnings("unchecked")
    public Map<String, ?> loadDrlMap(String destinationCode, String targetCode) {
        if (!StringUtils.hasText(destinationCode) || !StringUtils.hasText(targetCode)) {
            return Collections.emptyMap();
        }
        String cacheKey = buildRuleDrlMapCacheKey(destinationCode, targetCode);
        CacheService cacheService = cacheServiceProvider.getIfAvailable();
        if (cacheService != null) {
            Map<String, ?> fromCache = cacheService.get(cacheKey, Map.class)
                    .map(map -> (Map<String, ?>) map)
                    .orElse(Collections.emptyMap());
            if (!fromCache.isEmpty()) {
                return fromCache;
            }
        }
        return loadDrlMapFromDb(destinationCode.strip(), targetCode.strip());
    }

    private Map<String, ?> loadDrlMapFromDb(String destination, String targetValue) {
        List<GlobalSetting> settings =
                globalSettingReadRepository.findByDestinationAndTargetValueAndStatusTrue(
                        destination, targetValue);
        if (settings.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, Object>> drlMap = new LinkedHashMap<>();
        for (GlobalSetting setting : settings) {
            if (setting == null || setting.getId() == null) {
                continue;
            }
            String drl = setting.getDrlRule();
            if (!StringUtils.hasText(drl)) {
                continue;
            }
            Map<String, Object> entry = new HashMap<>(2);
            Integer sequence = setting.getSequenceNumber();
            entry.put("priority", sequence != null ? sequence : 0);
            entry.put("rule", drl.trim());
            drlMap.put(setting.getId().toString(), entry);
        }
        return drlMap.isEmpty() ? Collections.emptyMap() : drlMap;
    }

    private static String buildRuleDrlMapCacheKey(String destinationCode, String targetCode) {
        return RULE_KEY_PREFIX + normalizeKeyPart(destinationCode) + ":" + normalizeKeyPart(targetCode);
    }

    private static String normalizeKeyPart(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "_");
    }
}
