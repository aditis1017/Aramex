package com.customcharge.persistencelayer.rules;

import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.repository.GlobalSettingReadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSettingRuleLookupServiceTest {

    @Mock
    private ObjectProvider<CacheService> cacheServiceProvider;
    @Mock
    private CacheService cacheService;
    @Mock
    private GlobalSettingReadRepository globalSettingReadRepository;

    @Test
    void loadDrlMap_shouldReturnFromCache_whenPresent() {
        UUID ruleId = UUID.randomUUID();
        Map<String, Object> cached = Map.of(ruleId.toString(), Map.of("priority", 1, "rule", "drl-from-cache"));
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get("rule:AE:DDU", Map.class)).thenReturn(Optional.of(cached));

        GlobalSettingRuleLookupService service = newService();

        Map<String, ?> result = service.loadDrlMap("AE", "DDU");

        assertThat(result).isEqualTo(cached);
        verify(globalSettingReadRepository, never())
                .findByDestinationAndTargetValueAndStatusTrue(any(), any());
    }

    @Test
    void loadDrlMap_shouldFallbackToDb_whenCacheEmpty() {
        UUID ruleId = UUID.randomUUID();
        GlobalSetting setting = new GlobalSetting();
        ReflectionTestUtils.setField(setting, "id", ruleId);
        setting.setDrlRule("drl-from-db");
        setting.setSequenceNumber(3);

        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get("rule:AE:HV", Map.class)).thenReturn(Optional.empty());
        when(globalSettingReadRepository.findByDestinationAndTargetValueAndStatusTrue("AE", "HV"))
                .thenReturn(List.of(setting));

        GlobalSettingRuleLookupService service = newService();

        Map<String, ?> result = service.loadDrlMap("AE", "HV");

        assertThat(result).containsKey(ruleId.toString());
        assertThat(result.get(ruleId.toString())).isEqualTo(Map.of("priority", 3, "rule", "drl-from-db"));
    }

    private GlobalSettingRuleLookupService newService() {
        return new GlobalSettingRuleLookupService(cacheServiceProvider, globalSettingReadRepository);
    }
}
