package com.customcharge.global_settings.rules.service.imp;

import com.customcharge.global_settings.rules.dao.GlobalSettingDao;
import com.customcharge.global_settings.rulesmanager.DrlRuleConverter;
import com.customcharge.global_settings.rules.service.imp.GlobalSettingDrlCacheManager;
import com.customcharge.global_settings.support.GlobalSettingsTestDataFactory;
import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSettingSyncServiceTest {

    @Mock
    private GlobalSettingDao globalSettingDao;

    @Mock
    private GlobalSettingDrlCacheManager globalSettingDrlCacheManager;

    @Mock
    private CacheService cacheService;

    @Mock
    private CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private DrlRuleConverter drlRuleConverter;

    @InjectMocks
    private GlobalSettingSyncService syncService;

    @Test
    void syncGlobalSettings_shouldComplete_whenLockAcquired() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class)))
                .thenReturn(Boolean.TRUE);
        when(globalSettingDao.findByStatusTrue()).thenReturn(List.of(setting));
        when(drlRuleConverter.convert(setting, ShipmentDto.class)).thenReturn("drl");

        var response = syncService.syncGlobalSettings(LocalDate.of(2026, 5, 24), true);

        assertThat(response.getMessage()).contains("Sync Completed");
        verify(categoryCodesGlobalDataCacheService).refreshAll();
        verify(cacheService).deleteCacheUsingPattern("rule:*");
        verify(globalSettingDrlCacheManager).syncOnCreate(setting);
        verify(redisTemplate).delete("lock:globalSetting:sync");
    }

    @Test
    void syncGlobalSettings_shouldThrowConflict_whenLockNotAcquired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class)))
                .thenReturn(Boolean.FALSE);

        assertThatThrownBy(() -> syncService.syncGlobalSettings(null, false))
                .isInstanceOf(ConflictException.class);
        verify(globalSettingDao, never()).findByStatusTrue();
    }

    @Test
    void syncGlobalSettings_shouldSkipDrlConversion_forShipmentFilter() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "SHIPMENT_FILTER", "f", "v");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(Boolean.TRUE);
        when(globalSettingDao.findByStatusTrue()).thenReturn(List.of(setting));

        syncService.syncGlobalSettings(LocalDate.now(), false);

        verify(drlRuleConverter, never()).convert(any(), any());
        verify(globalSettingDrlCacheManager).syncOnCreate(setting);
    }
}
