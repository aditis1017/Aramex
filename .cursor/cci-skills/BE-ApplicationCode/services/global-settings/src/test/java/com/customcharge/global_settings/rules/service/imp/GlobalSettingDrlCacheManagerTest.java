package com.customcharge.global_settings.rules.service.imp;

import com.customcharge.global_settings.rules.dto.RuleCacheEntry;
import com.customcharge.global_settings.rules.service.imp.GlobalSettingDrlCacheManager;
import com.customcharge.global_settings.support.GlobalSettingsTestDataFactory;
import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSettingDrlCacheManagerTest {

    @Mock
    private CacheService redisCacheService;

    @InjectMocks
    private GlobalSettingDrlCacheManager cacheManager;

    private void verifyCachePut(String expectedKey) {
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisCacheService).put(keyCaptor.capture(), any(), any());
        assertThat(keyCaptor.getValue()).isEqualTo(expectedKey);
    }

    @Test
    void syncOnCreate_shouldPutActiveSettingInCache() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f1", "v1");
        when(redisCacheService.get("rule:DXB:DDU", Map.class)).thenReturn(Optional.empty());

        cacheManager.syncOnCreate(setting);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, RuleCacheEntry>> captor = ArgumentCaptor.forClass(Map.class);
        verify(redisCacheService).put(keyCaptor.capture(), captor.capture(), any());
        assertThat(keyCaptor.getValue()).isEqualTo("rule:DXB:DDU");
        assertThat(captor.getValue()).containsKey(setting.getId().toString());
    }

    @Test
    void syncOnStatusChange_shouldEvict_whenInactive() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f1", "v1");
        setting.setStatus(Boolean.FALSE);
        Map<String, RuleCacheEntry> existing = new HashMap<>();
        existing.put(setting.getId().toString(), new RuleCacheEntry(1, "rule"));
        when(redisCacheService.get("rule:DXB:DDU", Map.class)).thenReturn(Optional.of(existing));

        cacheManager.syncOnStatusChange(setting);

        verify(redisCacheService).evict("rule:DXB:DDU");
    }

    @Test
    void syncOnDelete_shouldRemoveEntryFromCache() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f1", "v1");
        Map<String, Object> existing = new HashMap<>();
        existing.put(setting.getId().toString(), new RuleCacheEntry(1, "rule"));
        when(redisCacheService.get("rule:DXB:DDU", Map.class)).thenReturn(Optional.of(existing));

        cacheManager.syncOnDelete(setting);

        verify(redisCacheService).evict("rule:DXB:DDU");
    }

    @Test
    void syncOnUpdate_shouldMoveEntry_whenDestinationChanges() {
        GlobalSetting after = GlobalSettingsTestDataFactory.globalSetting("AMM", "DDP", "f1", "v1");
        UUID previousId = UUID.randomUUID();
        when(redisCacheService.get(anyString(), any(Class.class))).thenReturn(Optional.empty());

        cacheManager.syncOnUpdate(previousId, "DXB", "DDU", after);

        verifyCachePut("rule:AMM:DDP");
    }

    @Test
    void syncOnCreate_shouldNoOp_whenSettingNull() {
        cacheManager.syncOnCreate(null);
        verify(redisCacheService, never()).put(any(), any(), any());
    }

    @Test
    void syncOnStatusChange_shouldUpdateCache_whenActive() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f1", "v1");
        setting.setStatus(Boolean.TRUE);
        when(redisCacheService.get("rule:DXB:DDU", Map.class)).thenReturn(Optional.empty());

        cacheManager.syncOnStatusChange(setting);

        verifyCachePut("rule:DXB:DDU");
    }

    @Test
    void syncOnCreate_shouldRemoveFromCache_whenInactive() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f1", "v1");
        setting.setStatus(Boolean.FALSE);
        Map<String, RuleCacheEntry> existing = new HashMap<>();
        existing.put(setting.getId().toString(), new RuleCacheEntry(1, "rule"));
        when(redisCacheService.get("rule:DXB:DDU", Map.class)).thenReturn(Optional.of(existing));

        cacheManager.syncOnCreate(setting);

        verify(redisCacheService).evict("rule:DXB:DDU");
    }

    @Test
    void syncOnDelete_shouldPreserveOtherEntries_whenLegacyValuesExist() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f1", "v1");
        UUID otherId = UUID.randomUUID();
        Map<String, Object> existing = new HashMap<>();
        existing.put(setting.getId().toString(), "legacy-rule");
        existing.put(otherId.toString(), Map.of("priority", 2, "rule", "rule2"));
        when(redisCacheService.get("rule:DXB:DDU", Map.class)).thenReturn(Optional.of(existing));

        cacheManager.syncOnDelete(setting);

        verifyCachePut("rule:DXB:DDU");
    }

    @Test
    void syncOnDelete_shouldNoOpSave_whenIdNotInCache() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f1", "v1");
        when(redisCacheService.get("rule:DXB:DDU", Map.class)).thenReturn(Optional.of(new HashMap<>()));

        cacheManager.syncOnDelete(setting);

        verify(redisCacheService, never()).put(any(), any(), any());
        verify(redisCacheService, never()).evict(any());
    }

    @Test
    void syncOnCreate_shouldNoOp_whenDestinationNull() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f1", "v1");
        setting.setDestination(null);

        cacheManager.syncOnCreate(setting);

        verify(redisCacheService, never()).put(any(), any(), any());
    }

    @Test
    void syncOnCreate_shouldNormalizeWhitespaceInCacheKey() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting(" DXB ", "DD U", "f1", "v1");
        when(redisCacheService.get("rule:DXB:DD_U", Map.class)).thenReturn(Optional.empty());

        cacheManager.syncOnCreate(setting);

        verifyCachePut("rule:DXB:DD_U");
    }

    @Test
    void syncOnCreate_shouldNoOp_whenSettingIdMissing() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f1", "v1");
        org.springframework.test.util.ReflectionTestUtils.setField(setting, "id", null);

        cacheManager.syncOnCreate(setting);

        verify(redisCacheService, never()).put(any(), any(), any());
    }

    @Test
    void syncOnCreate_shouldNoOp_whenTargetValueMissing() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f1", "v1");
        setting.setTargetValue(null);

        cacheManager.syncOnCreate(setting);

        verify(redisCacheService, never()).put(any(), any(), any());
    }

    @Test
    void syncOnUpdate_shouldSkipPreviousRemoval_whenPreviousIdNull() {
        GlobalSetting after = GlobalSettingsTestDataFactory.globalSetting("AMM", "DDP", "f1", "v1");
        when(redisCacheService.get(anyString(), any(Class.class))).thenReturn(Optional.empty());

        cacheManager.syncOnUpdate(null, "DXB", "DDU", after);

        verifyCachePut("rule:AMM:DDP");
    }

    @Test
    void syncOnDelete_shouldEvict_whenLastEntryRemoved() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f1", "v1");
        Map<String, RuleCacheEntry> existing = new HashMap<>();
        existing.put(setting.getId().toString(), new RuleCacheEntry(1, "rule"));
        when(redisCacheService.get("rule:DXB:DDU", Map.class)).thenReturn(Optional.of(existing));

        cacheManager.syncOnDelete(setting);

        verify(redisCacheService).evict("rule:DXB:DDU");
    }

    @Test
    void syncOnDelete_shouldIgnoreUnrecognizedCacheValues() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f1", "v1");
        Map<String, Object> existing = new HashMap<>();
        existing.put(setting.getId().toString(), 42);
        when(redisCacheService.get("rule:DXB:DDU", Map.class)).thenReturn(Optional.of(existing));

        cacheManager.syncOnDelete(setting);

        verify(redisCacheService, never()).put(any(), any(), any());
        verify(redisCacheService, never()).evict(any());
    }

    @Test
    void syncOnCreate_shouldLoadLegacyMapEntryWithPriority() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f1", "v1");
        UUID otherId = UUID.randomUUID();
        Map<String, Object> existing = new HashMap<>();
        existing.put(otherId.toString(), Map.of("priority", "bad", "rule", "rule2"));
        when(redisCacheService.get("rule:DXB:DDU", Map.class)).thenReturn(Optional.of(existing));

        cacheManager.syncOnCreate(setting);

        verifyCachePut("rule:DXB:DDU");
    }
}
