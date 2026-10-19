package com.customcharge.global_settings.controller;

import com.customcharge.global_settings.cache.GlobalSettingQueryCacheService;
import com.customcharge.global_settings.rules.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class GlobalSettingCacheController {

    private final GlobalSettingQueryCacheService globalSettingQueryCacheService;

    @DeleteMapping("/query-caches")
    public MessageResponse clearQueryCaches() {
        globalSettingQueryCacheService.clearQueryCaches();
        return new MessageResponse("Global settings query caches cleared successfully");
    }
}
