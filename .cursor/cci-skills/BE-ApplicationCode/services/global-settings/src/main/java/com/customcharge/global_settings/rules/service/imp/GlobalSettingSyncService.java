package com.customcharge.global_settings.rules.service.imp;

import com.customcharge.global_settings.rulesmanager.DrlRuleConverter;
import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.global_settings.rules.dao.GlobalSettingDao;
import com.customcharge.global_settings.rules.dto.MessageResponse;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.exception.ConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = false)
public class GlobalSettingSyncService {

    private static final String LOCK_KEY = "lock:globalSetting:sync";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final String DRL_MAP_PATTERN = "rule:*";

    private final GlobalSettingDao globalSettingDao;
    private final GlobalSettingDrlCacheManager globalSettingDrlCacheManager;
    private final CacheService cacheService;
    private final CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DrlRuleConverter drlRuleConverter;

    public GlobalSettingSyncService(GlobalSettingDao globalSettingDao,
                                    GlobalSettingDrlCacheManager globalSettingDrlCacheManager,
                                    @Qualifier("templateDrlRuleConverter") DrlRuleConverter drlRuleConverter,
                                    CacheService cacheService,
                                    CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService,
                                    RedisTemplate<String, Object> redisTemplate) {
        this.globalSettingDao = globalSettingDao;
        this.globalSettingDrlCacheManager = globalSettingDrlCacheManager;
        this.cacheService = cacheService;
        this.categoryCodesGlobalDataCacheService = categoryCodesGlobalDataCacheService;
        this.redisTemplate = redisTemplate;
        this.drlRuleConverter=drlRuleConverter;
    }

    public MessageResponse syncGlobalSettings(LocalDate referenceDate, boolean isGlobalSetting) {
        referenceDate = (referenceDate == null) ? LocalDate.now() : referenceDate;

        log.info("Starting GlobalSettings sync at {}", referenceDate);

        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, "LOCK", LOCK_TTL);

        if (Boolean.FALSE.equals(lockAcquired)) {
            log.warn("Sync already running. Exiting.");
            throw new ConflictException(
                    "Another global settings sync is already in progress. Please wait a few minutes and try again.");
        }

        try {
            if(isGlobalSetting){
                log.info("Refreshing master:category:* and master:category_value:* reference cache...");
                categoryCodesGlobalDataCacheService.refreshAll();
            }

            log.info("Clearing existing DRL map cache ({})...", DRL_MAP_PATTERN);
            cacheService.deleteCacheUsingPattern(DRL_MAP_PATTERN);
            log.info("rule:* DRL map cache cleared");

            List<GlobalSetting> activeSettings = globalSettingDao.findByStatusTrue();
            log.info("Fetched {} active settings to rebuild cache", activeSettings.size());

            for (GlobalSetting setting : activeSettings) {
                if(!setting.getTargetValue().equalsIgnoreCase("SHIPMENT_FILTER")){
                    setting.setDrlRule(drlRuleConverter.convert(setting, ShipmentDto.class));
                }
                globalSettingDrlCacheManager.syncOnCreate(setting);
            }
            log.info("DRL rule:* cache rebuild complete");
            return new MessageResponse("Sync Completed on " + referenceDate);
        } finally {
            redisTemplate.delete(LOCK_KEY);
            log.info("Lock released, sync completed");
        }
    }
}
