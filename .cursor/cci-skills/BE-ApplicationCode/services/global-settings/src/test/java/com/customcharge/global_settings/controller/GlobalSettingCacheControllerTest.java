package com.customcharge.global_settings.controller;

import com.customcharge.global_settings.cache.GlobalSettingQueryCacheService;
import com.customcharge.global_settings.rules.dto.MessageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GlobalSettingCacheControllerTest {

    @Mock
    private GlobalSettingQueryCacheService globalSettingQueryCacheService;

    @InjectMocks
    private GlobalSettingCacheController controller;

    @Test
    void clearQueryCaches_shouldDelegateAndReturnSuccessMessage() {
        MessageResponse response = controller.clearQueryCaches();

        verify(globalSettingQueryCacheService).clearQueryCaches();
        assertThat(response.getMessage()).isEqualTo("Global settings query caches cleared successfully");
    }
}
